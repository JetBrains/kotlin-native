package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny


internal class RTTIGenerator(override val context: Context) : ContextUtils {

    private inner class FieldTableRecord(val nameSignature: LocalHash, val fieldOffset: Int) :
            Struct(runtime.fieldTableRecordType, nameSignature, Int32(fieldOffset))

    private inner class MethodTableRecord(val nameSignature: LocalHash, val methodEntryPoint: ConstValue) :
            Struct(runtime.methodTableRecordType, nameSignature, methodEntryPoint)

    private inner class TypeInfo(val name: ConstValue, val size: Int,
                                 val superType: ConstValue,
                                 val objOffsets: ConstValue,
                                 val objOffsetsCount: Int,
                                 val interfaces: ConstValue,
                                 val interfacesCount: Int,
                                 val methods: ConstValue,
                                 val methodsCount: Int,
                                 val fields: ConstValue,
                                 val fieldsCount: Int) :
            Struct(
                    runtime.typeInfoType,

                    name,
                    Int32(size),

                    superType,

                    objOffsets,
                    Int32(objOffsetsCount),

                    interfaces,
                    Int32(interfacesCount),

                    methods,
                    Int32(methodsCount),

                    fields,
                    Int32(fieldsCount)
            )

    private fun exportTypeInfoIfRequired(classDesc: ClassDescriptor, typeInfoGlobal: LLVMValueRef?) {
        val annot = classDesc.annotations.findAnnotation(FqName("konan.ExportTypeInfo"))
        if (annot != null) {
            val nameValue = annot.allValueArguments.values.single() as StringValue
            // TODO: use LLVMAddAlias?
            val global = LLVMAddGlobal(context.llvmModule, pointerType(runtime.typeInfoType), nameValue.value)
            LLVMSetInitializer(global, typeInfoGlobal)
        }
    }

    private val arrayClasses = mapOf(
            "kotlin.Array"        to -LLVMABISizeOfType(llvmTargetData, kObjHeaderPtr).toInt(),
            "kotlin.ByteArray"    to -1,
            "kotlin.CharArray"    to -2,
            "kotlin.ShortArray"   to -2,
            "kotlin.IntArray"     to -4,
            "kotlin.LongArray"    to -8,
            "kotlin.FloatArray"   to -4,
            "kotlin.DoubleArray"  to -8,
            "kotlin.BooleanArray" to -1,
            "kotlin.String"       to -2
    )

    private fun getInstanceSize(classType: LLVMTypeRef?, className: FqName) : Int {
        val arraySize = arrayClasses.get(className.asString());
        if (arraySize != null) return arraySize;
        return LLVMStoreSizeOfType(llvmTargetData, classType).toInt()
    }

    fun generate(classDesc: ClassDescriptor) {

        val className = classDesc.fqNameSafe

        val llvmDeclarations = context.llvmDeclarations.forClass(classDesc)

        val bodyType = llvmDeclarations.bodyType

        val name = className.globalHash

        val size = getInstanceSize(bodyType, className)

        val superTypeOrAny = classDesc.getSuperClassOrAny()
        val superType = if (KotlinBuiltIns.isAny(classDesc)) NullPointer(runtime.typeInfoType)
                else superTypeOrAny.typeInfoPtr

        val interfaces = classDesc.implementedInterfaces.map { it.typeInfoPtr }
        val interfacesPtr = staticData.placeGlobalConstArray("kintf:$className",
                pointerType(runtime.typeInfoType), interfaces)

        // TODO: reuse offsets obtained for 'fields' below
        val objOffsets = getStructElements(bodyType).mapIndexedNotNull { index, type ->
            if (isObjectType(type)) {
                LLVMOffsetOfElement(llvmTargetData, bodyType, index)
            } else {
                null
            }
        }

        val objOffsetsPtr = staticData.placeGlobalConstArray("krefs:$className", int32Type,
                objOffsets.map { Int32(it.toInt()) })

        val fields = llvmDeclarations.fields.mapIndexed { index, field ->
            // Note: using FQ name because a class may have multiple fields with the same name due to property overriding
            val nameSignature = field.fqNameSafe.localHash // FIXME: add signature
            val fieldOffset = LLVMOffsetOfElement(llvmTargetData, bodyType, index)
            FieldTableRecord(nameSignature, fieldOffset.toInt())
        }.sortedBy { it.nameSignature.value }

        val fieldsPtr = staticData.placeGlobalConstArray("kfields:$className",
                runtime.fieldTableRecordType, fields)

        println("GEN_TABLES: class = $classDesc")
        val methods = if (classDesc.isAbstract()) {
            emptyList()
        } else {
            val functionNames = mutableMapOf<Long, OverriddenFunctionDescriptor>()
            classDesc.methodTableEntries.map {
                val functionName = it.overriddenDescriptor.functionName
                val nameSignature = functionName.localHash
                val prev = functionNames.putIfAbsent(nameSignature.value, it)
                if (prev != null)
                    throw AssertionError("Duplicate method table entry: functionName = '$functionName', hash = '${nameSignature.value}', entry1 = $prev, entry2 = $it")

                // TODO: compile-time resolution limits binary compatibility
                val implementation = it.implementation
                println("METHOD_TABLE: function = $functionName")
                println("METHOD_TABLE: impl = $implementation")
                val methodEntryPoint =  implementation.entryPointAddress
                MethodTableRecord(nameSignature, methodEntryPoint)
            }.sortedBy { it.nameSignature.value }
        }

        val methodsPtr = staticData.placeGlobalConstArray("kmethods:$className",
                runtime.methodTableRecordType, methods)

        val typeInfo = TypeInfo(name, size,
                superType,
                objOffsetsPtr, objOffsets.size,
                interfacesPtr, interfaces.size,
                methodsPtr, methods.size,
                fieldsPtr, if (classDesc.isInterface) -1 else fields.size)

        val typeInfoGlobal = llvmDeclarations.typeInfoGlobal

        val typeInfoGlobalValue = if (classDesc.isAbstract()) {
            typeInfo
        } else {
            // TODO: compile-time resolution limits binary compatibility
            val vtableEntries = classDesc.vtableEntries.map {
                val implementation = it.implementation
                println("RAW_VTABLE: impl = $implementation")
                implementation.entryPointAddress
            }
            val vtable = ConstArray(int8TypePtr, vtableEntries)
            Struct(typeInfo, vtable)
        }

        typeInfoGlobal.setInitializer(typeInfoGlobalValue)
        typeInfoGlobal.setConstant(true)

        exportTypeInfoIfRequired(classDesc, classDesc.llvmTypeInfoPtr)
    }

    internal val OverriddenFunctionDescriptor.implementation: FunctionDescriptor
        get() {
            // TODO: what if target is defined in another module?
            if (descriptor.modality == Modality.ABSTRACT)
                return descriptor

            val target = descriptor.target

            println("IMPLEMENTATION: descriptor = $descriptor")
            println("IMPLEMENTATION: target = $target")
            println("IMPLEMENTATION: overriddenDescriptor = $overriddenDescriptor")
            println("IMPLEMENTATION: needBridge = $needBridge")

            if (!needBridge) return target
            val bridgeOwner = if (descriptor.bridgeDirection != null) descriptor else target
            val bridge = context.bridges[bridgeOwner]
                    ?: throw AssertionError("Bridge is not built for $descriptor, bridgeOwner = $bridgeOwner")
            println("IMPLEMENTATION: bridge = $bridge")
            return bridge
        }
}

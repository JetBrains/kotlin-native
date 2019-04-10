/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameSafe
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun createLlvmDeclarations(context: Context): LlvmDeclarations {
    val generator = DeclarationsGeneratorVisitor(context)
    context.ir.irModule.acceptChildrenVoid(generator)
    return with(generator) {
        LlvmDeclarations(
                functions, classes, fields, staticFields, uniques
        )
    }
}

// Please note, that llvmName is part of the ABI, and cannot be liberally changed.
enum class UniqueKind(val llvmName: String) {
    UNIT("theUnitInstance"),
    EMPTY_ARRAY("theEmptyArray")
}

internal class LlvmDeclarations(
    private val functions: Map<IrFunction, FunctionLlvmDeclarations>,
    private val classes: Map<IrClass, ClassLlvmDeclarations>,
    private val fields: Map<IrField, FieldLlvmDeclarations>,
    private val staticFields: Map<IrField, StaticFieldLlvmDeclarations>,
    private val unique: Map<UniqueKind, UniqueLlvmDeclarations>) {
    fun forFunction(function: IrFunction) = forFunctionOrNull(function) ?: with(function){error("$name in $file/${parent.fqNameSafe}")}
    fun forFunctionOrNull(function: IrFunction) = functions[function]

    fun forClass(irClass: IrClass) = classes[irClass] ?:
            error(irClass.descriptor.toString())

    fun forField(field: IrField) = fields[field] ?:
            error(field.descriptor.toString())

    fun forStaticField(field: IrField) = staticFields[field] ?:
            error(field.descriptor.toString())

    fun forSingleton(irClass: IrClass) = forClass(irClass).singletonDeclarations ?:
            error(irClass.descriptor.toString())

    fun forUnique(kind: UniqueKind) = unique[kind] ?: error("No unique $kind")

}

internal class ClassLlvmDeclarations(
        val bodyType: LLVMTypeRef,
        val fields: List<IrField>, // TODO: it is not an LLVM declaration.
        val typeInfoGlobal: StaticData.Global,
        val writableTypeInfoGlobal: StaticData.Global?,
        val typeInfo: ConstPointer,
        val singletonDeclarations: SingletonLlvmDeclarations?,
        val objCDeclarations: KotlinObjCClassLlvmDeclarations?)

internal class SingletonLlvmDeclarations(val instanceFieldRef: LLVMValueRef, val instanceShadowFieldRef: LLVMValueRef?)

internal class KotlinObjCClassLlvmDeclarations(
        val classPointerGlobal: StaticData.Global,
        val classInfoGlobal: StaticData.Global,
        val bodyOffsetGlobal: StaticData.Global
)

internal class FunctionLlvmDeclarations(val llvmFunction: LLVMValueRef)

internal class FieldLlvmDeclarations(val index: Int, val classBodyType: LLVMTypeRef)

internal class StaticFieldLlvmDeclarations(val storage: LLVMValueRef)

internal class UniqueLlvmDeclarations(val pointer: ConstPointer)

// TODO: rework getFields and getDeclaredFields.

/**
 * All fields of the class instance.
 * The order respects the class hierarchy, i.e. a class [fields] contains superclass [fields] as a prefix.
 */
internal fun ContextUtils.getFields(irClass: IrClass) = context.getFields(irClass)

internal fun Context.getFields(irClass: IrClass): List<IrField> {
    val superClass = irClass.getSuperClassNotAny() // TODO: what if Any has fields?
    val superFields = if (superClass != null) getFields(superClass) else emptyList()

    return superFields + getDeclaredFields(irClass)
}

/**
 * Fields declared in the class.
 */
private fun Context.getDeclaredFields(irClass: IrClass): List<IrField> {
    // TODO: Here's what is going on here:
    // The existence of a backing field for a property is only described in the IR,
    // but not in the PropertyDescriptor.
    //
    // We mark serialized properties with a Konan protobuf extension bit,
    // so it is present in DeserializedPropertyDescriptor.
    //
    // In this function we check the presence of the backing field
    // two ways: first we check IR, then we check the protobuf extension.

    val fields = irClass.declarations.mapNotNull {
        when (it) {
            is IrField -> it.takeIf { it.isReal }
            is IrProperty -> it.takeIf { it.isReal }?.konanBackingField
            else -> null
        }
    }
    // TODO: hack over missed parents in deserialized fields/property declarations.
    fields.forEach{it.parent = irClass}

    if (irClass.hasAnnotation(FqName.fromSegments(listOf("kotlin", "native", "internal", "NoReorderFields"))))
        return fields

    return fields.sortedBy {
        it.fqNameSafe.localHash.value
    }
}

private fun ContextUtils.createClassBodyType(name: String, fields: List<IrField>): LLVMTypeRef {
    val fieldTypes = listOf(runtime.objHeaderType) + fields.map { getLLVMType(it.type) }
    // TODO: consider adding synthetic ObjHeader field to Any.

    val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), name)!!

    LLVMStructSetBody(classType, fieldTypes.toCValues(), fieldTypes.size, 0)

    return classType
}

private class DeclarationsGeneratorVisitor(override val context: Context) :
        IrElementVisitorVoid, ContextUtils {

    val functions = mutableMapOf<IrFunction, FunctionLlvmDeclarations>()
    val classes = mutableMapOf<IrClass, ClassLlvmDeclarations>()
    val fields = mutableMapOf<IrField, FieldLlvmDeclarations>()
    val staticFields = mutableMapOf<IrField, StaticFieldLlvmDeclarations>()
    val uniques = mutableMapOf<UniqueKind, UniqueLlvmDeclarations>()

    private class Namer(val prefix: String) {
        private val names = mutableMapOf<IrDeclaration, Name>()
        private val counts = mutableMapOf<FqName, Int>()

        fun getName(parent: FqName, declaration: IrDeclaration): Name {
            return names.getOrPut(declaration) {
                val count = counts.getOrDefault(parent, 0) + 1
                counts[parent] = count
                Name.identifier(prefix + count)
            }
        }
    }

    val objectNamer = Namer("object-")

    private fun getLocalName(parent: FqName, declaration: IrDeclaration): Name {
        if (declaration.isAnonymousObject) {
            return objectNamer.getName(parent, declaration)
        }

        return declaration.name
    }

    private fun getFqName(declaration: IrDeclaration): FqName {
        val parent = declaration.parent
        val parentFqName = when (parent) {
            is IrPackageFragment -> parent.fqName
            is IrDeclaration -> getFqName(parent)
            else -> error(parent)
        }

        val localName = getLocalName(parentFqName, declaration)
        return parentFqName.child(localName)
    }

    /**
     * Produces the name to be used for non-exported LLVM declarations corresponding to [declaration].
     *
     * Note: since these declarations are going to be private, the name is only required not to clash with any
     * exported declarations.
     */
    private fun qualifyInternalName(declaration: IrDeclaration): String {
        return getFqName(declaration).asString() + "#internal"
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        this.classes[declaration] = createClassDeclarations(declaration)

        super.visitClass(declaration)
    }

    private fun createClassDeclarations(declaration: IrClass): ClassLlvmDeclarations {
        val internalName = qualifyInternalName(declaration)

        val fields = getFields(declaration)
        val bodyType = createClassBodyType("kclassbody:$internalName", fields)

        val typeInfoPtr: ConstPointer
        val typeInfoGlobal: StaticData.Global

        val typeInfoSymbolName = if (declaration.isExported()) {
            declaration.typeInfoSymbolName
        } else {
            "ktype:$internalName"
        }

        if (declaration.typeInfoHasVtableAttached) {
            // Create the special global consisting of TypeInfo and vtable.

            val typeInfoGlobalName = "ktypeglobal:$internalName"

            val typeInfoWithVtableType = structType(
                    runtime.typeInfoType,
                    LLVMArrayType(int8TypePtr, context.getVtableBuilder(declaration).vtableEntries.size)!!
            )

            typeInfoGlobal = staticData.createGlobal(typeInfoWithVtableType, typeInfoGlobalName, isExported = false)

            val llvmTypeInfoPtr = LLVMAddAlias(context.llvmModule,
                    kTypeInfoPtr,
                    typeInfoGlobal.pointer.getElementPtr(0).llvm,
                    typeInfoSymbolName)!!

            if (declaration.isExported()) {
                if (llvmTypeInfoPtr.name != typeInfoSymbolName) {
                    // So alias name has been mangled by LLVM to avoid name clash.
                    throw IllegalArgumentException("Global '$typeInfoSymbolName' already exists")
                }
            } else {
                LLVMSetLinkage(llvmTypeInfoPtr, LLVMLinkage.LLVMInternalLinkage)
            }

            typeInfoPtr = constPointer(llvmTypeInfoPtr)

        } else {
            typeInfoGlobal = staticData.createGlobal(runtime.typeInfoType,
                    typeInfoSymbolName,
                    isExported = declaration.isExported())

            typeInfoPtr = typeInfoGlobal.pointer
        }

        if (declaration.isUnit() || declaration.isKotlinArray())
            createUniqueDeclarations(declaration, typeInfoPtr, bodyType)

        val singletonDeclarations = if (declaration.kind.isSingleton) {
            createSingletonDeclarations(declaration)
        } else {
            null
        }

        val objCDeclarations = if (declaration.isKotlinObjCClass()) {
            createKotlinObjCClassDeclarations(declaration)
        } else {
            null
        }

        val writableTypeInfoType = runtime.writableTypeInfoType
        val writableTypeInfoGlobal = if (writableTypeInfoType == null) {
            null
        } else if (declaration.isExported()) {
            val name = declaration.writableTypeInfoSymbolName
            staticData.createGlobal(writableTypeInfoType, name, isExported = true).also {
                it.setLinkage(LLVMLinkage.LLVMCommonLinkage) // Allows to be replaced by other bitcode module.
            }
        } else {
            staticData.createGlobal(writableTypeInfoType, "")
        }.also {
            it.setZeroInitializer()
        }

        return ClassLlvmDeclarations(bodyType, fields, typeInfoGlobal, writableTypeInfoGlobal, typeInfoPtr,
                singletonDeclarations, objCDeclarations)
    }

    private fun createUniqueDeclarations(
            irClass: IrClass, typeInfoPtr: ConstPointer, bodyType: LLVMTypeRef) {
        when {
                irClass.isUnit() -> {
                    uniques[UniqueKind.UNIT] =
                            UniqueLlvmDeclarations(staticData.createUniqueInstance(UniqueKind.UNIT, bodyType, typeInfoPtr))
                }
                irClass.isKotlinArray() -> {
                    uniques[UniqueKind.EMPTY_ARRAY] =
                            UniqueLlvmDeclarations(staticData.createUniqueInstance(UniqueKind.EMPTY_ARRAY, bodyType, typeInfoPtr))
                }
                else -> TODO("Unsupported unique $irClass")
        }
    }

    private fun createSingletonDeclarations(irClass: IrClass): SingletonLlvmDeclarations? {

        if (irClass.isUnit()) {
            return null
        }

        val isExported = irClass.isExported()
        val symbolName = if (isExported) {
            irClass.objectInstanceFieldSymbolName
        } else {
            "kobjref:" + qualifyInternalName(irClass)
        }
        val threadLocal = !(irClass.objectIsShared && context.config.threadsAreAllowed)
        val instanceFieldRef = addGlobal(
                symbolName, getLLVMType(irClass.defaultType), isExported = isExported, threadLocal = threadLocal)

        LLVMSetInitializer(instanceFieldRef, kNullObjHeaderPtr)

        val instanceShadowFieldRef =
                if (threadLocal) null
                else {
                    val shadowSymbolName = if (isExported) {
                        irClass.objectInstanceShadowFieldSymbolName
                    } else {
                        "kshadowobjref:" + qualifyInternalName(irClass)
                    }
                    addGlobal(shadowSymbolName, getLLVMType(irClass.defaultType), isExported = isExported, threadLocal = true)
                }

        instanceShadowFieldRef?.let { LLVMSetInitializer(it, kNullObjHeaderPtr) }

        return SingletonLlvmDeclarations(instanceFieldRef, instanceShadowFieldRef)
    }

    private fun createKotlinObjCClassDeclarations(irClass: IrClass): KotlinObjCClassLlvmDeclarations {
        val internalName = qualifyInternalName(irClass)

        val classPointerGlobal = staticData.createGlobal(int8TypePtr, "kobjcclassptr:$internalName")

        val classInfoGlobal = staticData.createGlobal(
                context.llvm.runtime.kotlinObjCClassInfo,
                "kobjcclassinfo:$internalName"
        ).apply {
            setConstant(true)
        }

        val bodyOffsetGlobal = staticData.createGlobal(int32Type, "kobjcbodyoffs:$internalName")

        return KotlinObjCClassLlvmDeclarations(classPointerGlobal, classInfoGlobal, bodyOffsetGlobal)
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)

        val containingClass = declaration.parent as? IrClass
        if (containingClass != null) {
            val classDeclarations = this.classes[containingClass] ?:
                error(containingClass.descriptor.toString())
            val allFields = classDeclarations.fields
            this.fields[declaration] = FieldLlvmDeclarations(
                    allFields.indexOf(declaration) + 1, // First field is ObjHeader.
                    classDeclarations.bodyType
            )
        } else {
            // Fields are module-private, so we use internal name:
            val name = "kvar:" + qualifyInternalName(declaration)
            val storage = addGlobal(
                    name, getLLVMType(declaration.type), isExported = false,
                    threadLocal = declaration.storageClass == FieldStorage.THREAD_LOCAL)

            this.staticFields[declaration] = StaticFieldLlvmDeclarations(storage)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        if (!declaration.isReal) return

        val llvmFunctionType = getLlvmFunctionType(declaration)

        if ((declaration is IrConstructor && declaration.isObjCConstructor)) {
            return
        }

        val llvmFunction = if (declaration.isExternal) {
            if (declaration.isTypedIntrinsic || declaration.isObjCBridgeBased()
                    || declaration.annotations.hasAnnotation(RuntimeNames.cCall)) return

            context.llvm.externalFunction(declaration.symbolName, llvmFunctionType,
                    // Assume that `external fun` is defined in native libs attached to this module:
                    origin = declaration.llvmSymbolOrigin,
                    independent = declaration.hasAnnotation(RuntimeNames.independent)
            )
        } else {
            val symbolName = if (declaration.isExported()) {
                declaration.symbolName.also {
                    if (declaration.name.asString() != "main") {
                        assert(LLVMGetNamedFunction(context.llvm.llvmModule, it) == null) { it }
                    } else {
                        // As a workaround, allow `main` functions to clash because frontend accepts this.
                        // See [OverloadResolver.isTopLevelMainInDifferentFiles] usage.
                    }
                }
            } else {
                "kfun:" + qualifyInternalName(declaration)
            }
            val function = LLVMAddFunction(context.llvmModule, symbolName, llvmFunctionType)!!
            if (declaration.returnType.isNothing())
                setFunctionNoReturn(function)
            function
        }

        // TODO: do we still need it?
        if (!context.shouldOptimize()) {
            LLVMAddTargetDependentFunctionAttr(llvmFunction, "no-frame-pointer-elim", "true")
        }

        this.functions[declaration] = FunctionLlvmDeclarations(llvmFunction)
    }
}

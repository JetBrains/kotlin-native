/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.*
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.constructedClass
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.ObjCCodeGenerator
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class ObjCExportCodeGenerator(
        codegen: CodeGenerator,
        val namer: ObjCExportNamer,
        val mapper: ObjCExportMapper
) : ObjCCodeGenerator(codegen) {

    val runtime get() = codegen.runtime
    val staticData get() = codegen.staticData

    val rttiGenerator = RTTIGenerator(context)

    private val objcTerminate: LLVMValueRef by lazy {
        context.llvm.externalFunction(
                "objc_terminate",
                functionType(voidType, false),
                CurrentKonanModule
        ).also {
            setFunctionNoUnwind(it)
        }
    }

    // TODO: currently bridges don't have any custom `landingpad`s,
    // so it is correct to use [callAtFunctionScope] here.
    // However, exception handling probably should be refactored
    // (e.g. moved from `IrToBitcode.kt` to [FunctionGenerationContext]).
    fun FunctionGenerationContext.callFromBridge(
            function: LLVMValueRef,
            args: List<LLVMValueRef>,
            resultLifetime: Lifetime = Lifetime.IRRELEVANT
    ): LLVMValueRef {

        // TODO: it is required only for Kotlin-to-Objective-C bridges.
        this.forwardingForeignExceptionsTerminatedWith = objcTerminate

        return call(function, args, resultLifetime, ExceptionHandler.Caller)
    }

    fun FunctionGenerationContext.genSendMessage(
            returnType: LLVMTypeRef,
            receiver: LLVMValueRef,
            selector: String,
            vararg args: LLVMValueRef
    ): LLVMValueRef {

        val objcMsgSendType = functionType(
                returnType,
                false,
                listOf(int8TypePtr, int8TypePtr) + args.map { it.type }
        )

        return callFromBridge(msgSender(objcMsgSendType), listOf(receiver, genSelector(selector)) + args)
    }

    fun FunctionGenerationContext.kotlinToObjC(
            value: LLVMValueRef,
            valueType: ObjCValueType
    ): LLVMValueRef = when (valueType) {
        ObjCValueType.BOOL -> zext(value, int8Type) // TODO: zext behaviour may be strange on bit types.

        ObjCValueType.CHAR, ObjCValueType.UNSIGNED_SHORT, ObjCValueType.SHORT,
        ObjCValueType.INT, ObjCValueType.LONG_LONG, ObjCValueType.FLOAT, ObjCValueType.DOUBLE -> value
    }

    private fun FunctionGenerationContext.objCToKotlin(
            value: LLVMValueRef,
            valueType: ObjCValueType
    ): LLVMValueRef = when (valueType) {
        ObjCValueType.BOOL -> icmpNe(value, Int8(0).llvm)

        ObjCValueType.CHAR, ObjCValueType.UNSIGNED_SHORT, ObjCValueType.SHORT,
        ObjCValueType.INT, ObjCValueType.LONG_LONG, ObjCValueType.FLOAT, ObjCValueType.DOUBLE -> value
    }

    fun FunctionGenerationContext.kotlinReferenceToObjC(value: LLVMValueRef) =
            callFromBridge(context.llvm.Kotlin_ObjCExport_refToObjC, listOf(value))

    fun FunctionGenerationContext.objCReferenceToKotlin(value: LLVMValueRef, resultLifetime: Lifetime) =
            callFromBridge(context.llvm.Kotlin_ObjCExport_refFromObjC, listOf(value), resultLifetime)

    fun FunctionGenerationContext.kotlinToObjC(
            value: LLVMValueRef,
            typeBridge: TypeBridge
    ): LLVMValueRef = when (typeBridge) {
        is ReferenceBridge -> kotlinReferenceToObjC(value)
        is ValueTypeBridge -> kotlinToObjC(value, typeBridge.objCValueType)
        is HashCodeBridge -> {
            assert(codegen.context.is64Bit())
            zext(value, kInt64)
        }
    }

    fun FunctionGenerationContext.objCToKotlin(
            value: LLVMValueRef,
            typeBridge: TypeBridge,
            resultLifetime: Lifetime
    ): LLVMValueRef = when (typeBridge) {
        is ReferenceBridge -> objCReferenceToKotlin(value, resultLifetime)
        is ValueTypeBridge -> objCToKotlin(value, typeBridge.objCValueType)
        is HashCodeBridge -> {
            assert(codegen.context.is64Bit())
            val low = trunc(value, int32Type)
            val high = trunc(shr(value, 32, signed = false), int32Type)
            xor(low, high)
        }
    }

    fun FunctionGenerationContext.initRuntimeIfNeeded() {
        callFromBridge(context.llvm.initRuntimeIfNeeded, emptyList())
    }

    inline fun FunctionGenerationContext.convertKotlin(
            genValue: (Lifetime) -> LLVMValueRef,
            actualType: KotlinType,
            expectedType: KotlinType,
            resultLifetime: Lifetime
    ): LLVMValueRef {

        val conversion = context.ir.symbols.getTypeConversion(actualType, expectedType)
                ?: return genValue(resultLifetime)

        val value = genValue(Lifetime.ARGUMENT)

        return callFromBridge(conversion.owner.llvmFunction, listOf(value), resultLifetime)
    }

    internal fun emitRtti(
            generatedClasses: Collection<ClassDescriptor>,
            topLevel: Map<FqName, List<CallableMemberDescriptor>>
    ) {
        val objCTypeAdapters = mutableListOf<ObjCTypeAdapter>()

        generatedClasses.forEach {
            objCTypeAdapters += createTypeAdapter(it)
            val className = namer.getClassOrProtocolName(it)
            val superClass = it.getSuperClassOrAny()
            val superClassName = namer.getClassOrProtocolName(superClass)

            dataGenerator.emitEmptyClass(className, superClassName)
            // Note: it is generated only to be visible for linker.
            // Methods will be added at runtime.
        }

        topLevel.forEach { fqName, declarations ->
            objCTypeAdapters += createTypeAdapterForPackage(fqName, declarations)
            dataGenerator.emitEmptyClass(namer.getPackageName(fqName), namer.kotlinAnyName)
        }

        dataGenerator.exportClass("KotlinMutableSet")
        dataGenerator.exportClass("KotlinMutableDictionary")

        emitSpecialClassesConvertions()

        objCTypeAdapters += createTypeAdapter(context.builtIns.any)

        val placedClassAdapters = mutableMapOf<String, ConstPointer>()
        val placedInterfaceAdapters = mutableMapOf<String, ConstPointer>()

        objCTypeAdapters.forEach { adapter ->
            val typeAdapter = staticData.placeGlobal("", adapter).pointer
            val descriptor = adapter.descriptor

            val descriptorToAdapter = if (descriptor?.isInterface == true) {
                placedInterfaceAdapters
            } else {
                // Objective-C class for Kotlin class or top-level declarations.
                placedClassAdapters
            }
            descriptorToAdapter[adapter.objCName] = typeAdapter

            if (descriptor != null) {
                setObjCExportTypeInfo(descriptor, typeAdapter = typeAdapter)
            }
        }

        fun emitSortedAdapters(nameToAdapter: Map<String, ConstPointer>, prefix: String) {
            val sortedAdapters = nameToAdapter.toList().sortedBy { it.first }.map {
                it.second
            }

            if (sortedAdapters.isNotEmpty()) {
                val type = sortedAdapters.first().llvmType
                val sortedAdaptersPointer = staticData.placeGlobalConstArray("", type, sortedAdapters)

                // Note: this globals replace runtime globals with weak linkage:
                staticData.placeGlobal(prefix, sortedAdaptersPointer, isExported = true)
                staticData.placeGlobal("${prefix}Num", Int32(sortedAdapters.size), isExported = true)
            }
        }

        emitSortedAdapters(placedClassAdapters, "Kotlin_ObjCExport_sortedClassAdapters")
        emitSortedAdapters(placedInterfaceAdapters, "Kotlin_ObjCExport_sortedProtocolAdapters")

        context.llvm.kObjectReservedTailSize!!.setInitializer(Int32(runtime.pointerSize))

        dataGenerator.finishModule() // TODO: move to appropriate place.
    }

    private val impType = pointerType(functionType(int8TypePtr, true, int8TypePtr, int8TypePtr))

    internal val directMethodAdapters = mutableMapOf<DirectAdapterRequest, ObjCToKotlinMethodAdapter>()

    inner class ObjCToKotlinMethodAdapter(
            selector: String,
            encoding: String,
            imp: ConstPointer
    ) : Struct(
            runtime.objCToKotlinMethodAdapter,
            staticData.cStringLiteral(selector),
            staticData.cStringLiteral(encoding),
            imp.bitcast(impType)
    )

    inner class KotlinToObjCMethodAdapter(
            selector: String,
            nameSignature: Long,
            vtableIndex: Int,
            kotlinImpl: ConstPointer
    ) : Struct(
            runtime.kotlinToObjCMethodAdapter,
            staticData.cStringLiteral(selector),
            Int64(nameSignature),
            Int32(vtableIndex),
            kotlinImpl
    )

    inner class ObjCTypeAdapter(
            val descriptor: ClassDescriptor?,
            typeInfo: ConstPointer?,
            vtable: ConstPointer?,
            vtableSize: Int,
            methodTable: List<RTTIGenerator.MethodTableRecord>,
            val objCName: String,
            directAdapters: List<ObjCToKotlinMethodAdapter>,
            classAdapters: List<ObjCToKotlinMethodAdapter>,
            virtualAdapters: List<ObjCToKotlinMethodAdapter>,
            reverseAdapters: List<KotlinToObjCMethodAdapter>
    ) : Struct(
            runtime.objCTypeAdapter,
            typeInfo,

            vtable,
            Int32(vtableSize),

            staticData.placeGlobalConstArray("", runtime.methodTableRecordType, methodTable),
            Int32(methodTable.size),

            staticData.cStringLiteral(objCName),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.objCToKotlinMethodAdapter,
                    directAdapters
            ),
            Int32(directAdapters.size),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.objCToKotlinMethodAdapter,
                    classAdapters
            ),
            Int32(classAdapters.size),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.objCToKotlinMethodAdapter,
                    virtualAdapters
            ),
            Int32(virtualAdapters.size),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.kotlinToObjCMethodAdapter,
                    reverseAdapters
            ),
            Int32(reverseAdapters.size)
    )

}

private fun ObjCExportCodeGenerator.setObjCExportTypeInfo(
        descriptor: ClassDescriptor,
        converter: ConstPointer? = null,
        objCClass: ConstPointer? = null,
        typeAdapter: ConstPointer? = null
) {
    if (converter != null) {
        assert(converter.llvmType == pointerType(functionType(int8TypePtr, false, codegen.kObjHeaderPtr)))
    }

    val objCExportAddition = Struct(runtime.typeInfoObjCExportAddition,
            converter?.bitcast(int8TypePtr),
            objCClass,
            typeAdapter
    )

    val writableTypeInfoType = runtime.writableTypeInfoType!!
    val writableTypeInfoValue = Struct(writableTypeInfoType, objCExportAddition)

    val irClass = context.ir.get(descriptor)
    val global = if (codegen.isExternal(irClass)) {
        // Note: this global replaces the external one with common linkage.
        staticData.createGlobal(
                writableTypeInfoType,
                irClass.writableTypeInfoSymbolName,
                isExported = true
        )
    } else {
        context.llvmDeclarations.forClass(irClass).writableTypeInfoGlobal!!.also {
            it.setLinkage(LLVMLinkage.LLVMExternalLinkage)
        }
    }

    global.setInitializer(writableTypeInfoValue)
}

private val ObjCExportCodeGenerator.kotlinToObjCFunctionType: LLVMTypeRef
    get() = functionType(int8TypePtr, false, codegen.kObjHeaderPtr)

private fun ObjCExportCodeGenerator.emitBoxConverter(objCValueType: ObjCValueType) {
    val valueType = objCValueType.kotlinValueType

    val symbols = context.ir.symbols

    val name = "${valueType.classFqName.shortName()}To${objCValueType.nsNumberName}"

    val converter = generateFunction(codegen, kotlinToObjCFunctionType, name) {
        val unboxFunction = symbols.getUnboxFunction(valueType).owner.llvmFunction
        val kotlinValue = callFromBridge(
                unboxFunction,
                listOf(param(0)),
                Lifetime.IRRELEVANT
        )

        val value = kotlinToObjC(kotlinValue, objCValueType)

        val nsNumber = genGetSystemClass("NSNumber")
        ret(genSendMessage(int8TypePtr, nsNumber, objCValueType.nsNumberFactorySelector, value))
    }

    LLVMSetLinkage(converter, LLVMLinkage.LLVMPrivateLinkage)

    val boxClass = symbols.boxClasses[valueType]!!
    setObjCExportTypeInfo(boxClass.descriptor, constPointer(converter))
}

private fun ObjCExportCodeGenerator.emitFunctionConverters() {
    val generator = BlockAdapterToFunctionGenerator(this)

    (0 .. mapper.maxFunctionTypeParameterCount).forEach { numberOfParameters ->
        val converter = generator.run { generateConvertFunctionToBlock(numberOfParameters) }
        setObjCExportTypeInfo(context.builtIns.getFunction(numberOfParameters), constPointer(converter))
    }
}

private fun ObjCExportCodeGenerator.generateKotlinFunctionAdapterToBlock(numberOfParameters: Int): ConstPointer {
    val irInterface = context.ir.symbols.functions[numberOfParameters].owner
    val invokeMethod = irInterface.declarations.filterIsInstance<IrSimpleFunction>()
            .single { it.name == OperatorNameConventions.INVOKE }

    val invokeImpl = generateKotlinFunctionImpl(invokeMethod.descriptor)

    return rttiGenerator.generateSyntheticInterfaceImpl(
            irInterface,
            mapOf(invokeMethod to invokeImpl)
    )
}

private fun ObjCExportCodeGenerator.emitKotlinFunctionAdaptersToBlock() {
    val ptr = staticData.placeGlobalArray(
            "",
            pointerType(runtime.typeInfoType),
            (0 .. mapper.maxFunctionTypeParameterCount).map {
                generateKotlinFunctionAdapterToBlock(it)
            }
    ).pointer.getElementPtr(0)

    // Note: this global replaces the weak global defined in runtime.
    staticData.placeGlobal("Kotlin_ObjCExport_functionAdaptersToBlock", ptr, isExported = true)
}

private fun ObjCExportCodeGenerator.emitSpecialClassesConvertions() {
    setObjCExportTypeInfo(
            context.builtIns.string,
            constPointer(codegen.llvmFunction(context.ir.symbols.interopCreateNSStringFromKString.owner))
    )

    setObjCExportTypeInfo(
            context.builtIns.list,
            constPointer(context.llvm.Kotlin_Interop_CreateNSArrayFromKList)
    )

    setObjCExportTypeInfo(
            context.builtIns.mutableList,
            constPointer(context.llvm.Kotlin_Interop_CreateNSMutableArrayFromKList)
    )

    setObjCExportTypeInfo(
            context.builtIns.set,
            constPointer(context.llvm.Kotlin_Interop_CreateNSSetFromKSet)
    )

    setObjCExportTypeInfo(
            context.builtIns.mutableSet,
            constPointer(context.llvm.Kotlin_Interop_CreateKotlinMutableSetFromKSet)
    )

    setObjCExportTypeInfo(
            context.builtIns.map,
            constPointer(context.llvm.Kotlin_Interop_CreateNSDictionaryFromKMap)
    )

    setObjCExportTypeInfo(
            context.builtIns.mutableMap,
            constPointer(context.llvm.Kotlin_Interop_CreateKotlinMutableDictonaryFromKMap)
    )

    ObjCValueType.values().forEach {
        emitBoxConverter(it)
    }

    emitFunctionConverters()

    emitKotlinFunctionAdaptersToBlock()
}

private fun ObjCExportCodeGenerator.generateObjCImp(
        target: IrFunction?,
        methodBridge: MethodBridge,
        isVirtual: Boolean = false
): LLVMValueRef {
    // TODO: adapt exceptions.

    val returnType = methodBridge.returnBridge

    val result = LLVMAddFunction(context.llvmModule, "", objCFunctionType(methodBridge))!!

    generateFunction(codegen, result) {
        // TODO: call [NSObject init] if it is a constructor?
        // TODO: check for abstract class if it is a constructor.

        if (methodBridge.isKotlinTopLevel) {
            initRuntimeIfNeeded() // For instance methods it gets called when allocating.
        }

        if (target == null) {
            // IMP for abstract method.
            callFromBridge(
                    context.llvm.Kotlin_ObjCExport_AbstractMethodCalled,
                    listOf(param(0), param(1))
            )
            unreachable()
            return@generateFunction
        }

        val args = methodBridge.paramBridges.mapIndexedNotNull { index, typeBridge ->
            val isReceiver = index == 0
            if (isReceiver && methodBridge.isKotlinTopLevel) {
                null
            } else {
                val param = param(if (isReceiver) index else index + 1)
                objCToKotlin(param, typeBridge, Lifetime.ARGUMENT)
            }
        }

        val llvmTarget = if (!isVirtual) {
            codegen.llvmFunction(target)
        } else {
            lookupVirtualImpl(args.first(), target)
        }

        val targetResult = callFromBridge(llvmTarget, args, Lifetime.ARGUMENT)

        if (target is IrConstructor) {
            ret(param(0))
        } else when (returnType) {
            VoidBridge -> ret(null)
            is TypeBridge -> ret(kotlinToObjC(targetResult, returnType))
        }
    }

    LLVMSetLinkage(result, LLVMLinkage.LLVMPrivateLinkage)

    return result
}

private fun ObjCExportCodeGenerator.generateObjCImpForArrayConstructor(
        target: ConstructorDescriptor,
        methodBridge: MethodBridge
): LLVMValueRef {
    // TODO: adapt exceptions.

    val result = LLVMAddFunction(context.llvmModule, "", objCFunctionType(methodBridge))!!

    generateFunction(codegen, result) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.

        val kotlinValueArgs = methodBridge.paramBridges
                .drop(1) // Drop class method receiver.
                .mapIndexed { index, typeBridge ->
                    objCToKotlin(param(index + 2), typeBridge, Lifetime.ARGUMENT)
                }

        val targetIr = context.ir.get(target)

        val arrayInstance = callFromBridge(
                context.llvm.allocArrayFunction,
                listOf((targetIr as IrConstructor).constructedClass.llvmTypeInfoPtr, kotlinValueArgs.first()),
                resultLifetime = Lifetime.ARGUMENT
        )
        callFromBridge(
                targetIr.llvmFunction,
                listOf(arrayInstance) + kotlinValueArgs
        )

        ret(kotlinToObjC(arrayInstance, ReferenceBridge))
    }

    LLVMSetLinkage(result, LLVMLinkage.LLVMPrivateLinkage)

    return result
}

// TODO: cache bridges.
private fun ObjCExportCodeGenerator.generateKotlinToObjCBridge(
        descriptor: FunctionDescriptor,
        baseMethod: FunctionDescriptor
): ConstPointer {
    val methodBridge = mapper.bridgeMethod(baseMethod)

    val allBaseMethodParams = baseMethod.allParameters
    val paramBridges = methodBridge.paramBridges
    val returnBridge = methodBridge.returnBridge

    val objcMsgSend = msgSender(objCFunctionType(methodBridge))

    val functionType = codegen.getLlvmFunctionType(context.ir.get(descriptor))

    val result = generateFunction(codegen, functionType, "") {
        val args = mutableListOf<LLVMValueRef>()

        descriptor.allParameters.forEachIndexed { index, parameter ->

            val kotlinValue = convertKotlin(
                    { param(index) },
                    actualType = parameter.type,
                    expectedType = allBaseMethodParams[index].type,
                    resultLifetime = Lifetime.ARGUMENT
            )

            args += kotlinToObjC(kotlinValue, paramBridges[index])

            // TODO: if `convertKotlin` boxes Kotlin value, then it gets converted by `kotlinToObjC` to `NSNumber`,
            // and boxing directly to `NSNumber` would be much efficient.

            if (index == 0) {
                args += genSelector(namer.getSelector(baseMethod))
            }
        }

        val targetResult = callFromBridge(objcMsgSend, args)

        assert(baseMethod !is ConstructorDescriptor)

        when (returnBridge) {
            VoidBridge -> {
                if (LLVMGetReturnType(functionType) == voidType) {
                    ret(null)
                } else {
                    ret(staticData.theUnitInstanceRef.llvm)
                }
            }
            is TypeBridge -> {

                val genConvertedTargetResult = { lifetime: Lifetime ->
                    objCToKotlin(targetResult, returnBridge, lifetime)
                }

                ret(convertKotlin(
                        genConvertedTargetResult,
                        actualType = baseMethod.returnType!!,
                        expectedType = descriptor.returnType!!,
                        resultLifetime = Lifetime.RETURN_VALUE
                ))
            }
        }
    }

    LLVMSetLinkage(result, LLVMLinkage.LLVMPrivateLinkage)

    return constPointer(result)
}

private fun ObjCExportCodeGenerator.createReverseAdapter(
        descriptor: FunctionDescriptor,
        baseMethod: FunctionDescriptor,
        functionName: String,
        vtableIndex: Int?
): ObjCExportCodeGenerator.KotlinToObjCMethodAdapter {

    val nameSignature = functionName.localHash.value
    val selector = namer.getSelector(baseMethod)

    val kotlinToObjC = generateKotlinToObjCBridge(
            descriptor,
            baseMethod
    ).bitcast(int8TypePtr)

    return KotlinToObjCMethodAdapter(selector, nameSignature, vtableIndex ?: -1, kotlinToObjC)
}

private fun ObjCExportCodeGenerator.createMethodVirtualAdapter(
        baseMethod: FunctionDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(mapper.isBaseMethod(baseMethod))

    val selector = namer.getSelector(baseMethod)

    val methodBridge = mapper.bridgeMethod(baseMethod)
    val objCToKotlin = constPointer(generateObjCImp(context.ir.get(baseMethod), methodBridge, isVirtual = true))
    return ObjCToKotlinMethodAdapter(selector, getEncoding(methodBridge), objCToKotlin)
}

private fun ObjCExportCodeGenerator.createMethodAdapter(
        implementation: FunctionDescriptor?,
        baseMethod: FunctionDescriptor
) = createMethodAdapter(DirectAdapterRequest(implementation?.let { context.ir.get(it) }, baseMethod))

private fun ObjCExportCodeGenerator.createMethodAdapter(
        request: DirectAdapterRequest
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter = this.directMethodAdapters.getOrPut(request) {

    val selectorName = namer.getSelector(request.base)
    val methodBridge = mapper.bridgeMethod(request.base)
    val objCEncoding = getEncoding(methodBridge)
    val objCToKotlin = constPointer(generateObjCImp(request.implementation, methodBridge))

    ObjCToKotlinMethodAdapter(selectorName, objCEncoding, objCToKotlin)
}

private fun ObjCExportCodeGenerator.createConstructorAdapter(
        descriptor: ConstructorDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter = createMethodAdapter(descriptor, descriptor)

private fun ObjCExportCodeGenerator.createArrayConstructorAdapter(
        descriptor: ConstructorDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    val selectorName = namer.getSelector(descriptor)
    val methodBridge = mapper.bridgeMethod(descriptor)
    val objCEncoding = getEncoding(methodBridge)
    val objCToKotlin = constPointer(generateObjCImpForArrayConstructor(descriptor, methodBridge))

    return ObjCToKotlinMethodAdapter(selectorName, objCEncoding, objCToKotlin)
}

private fun ObjCExportCodeGenerator.vtableIndex(descriptor: FunctionDescriptor): Int? {
    assert(descriptor.isOverridable)
    val classDescriptor = descriptor.containingDeclaration as ClassDescriptor
    return if (classDescriptor.isInterface) {
        null
    } else {
        context.getVtableBuilder(context.ir.get(classDescriptor))
                .vtableIndex(context.ir.get(descriptor) as IrSimpleFunction)
    }
}

private fun ObjCExportCodeGenerator.createTypeAdapterForPackage(
        fqName: FqName,
        declarations: List<CallableMemberDescriptor>
): ObjCExportCodeGenerator.ObjCTypeAdapter {
    val name = namer.getPackageName(fqName)

    val adapters = declarations.toMethods().map { createMethodAdapter(it, it) }

    return ObjCTypeAdapter(
            descriptor = null,
            typeInfo = null,
            vtable = null,
            vtableSize = -1,
            methodTable = emptyList(),
            objCName = name,
            directAdapters = emptyList(),
            classAdapters = adapters,
            virtualAdapters = emptyList(),
            reverseAdapters = emptyList()
    )
}

private fun ObjCExportCodeGenerator.createTypeAdapter(
        descriptor: ClassDescriptor
): ObjCExportCodeGenerator.ObjCTypeAdapter {
    val adapters = mutableListOf<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter>()
    val classAdapters = mutableListOf<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter>()

    if (descriptor != context.builtIns.any) {
        descriptor.constructors.forEach {
            if (mapper.shouldBeExposed(it)) {
                if (it.constructedClass.isArray) {
                    classAdapters += createArrayConstructorAdapter(it)
                } else {
                    adapters += createConstructorAdapter(it)
                }
            }
        }
    }

    val categoryMethods = mapper.getCategoryMembersFor(descriptor).toMethods()

    val exposedMethods = descriptor.contributedMethods.filter { mapper.shouldBeExposed(it) } + categoryMethods

    exposedMethods.forEach {
        adapters += createDirectAdapters(it)
    }

    val reverseAdapters = mutableListOf<ObjCExportCodeGenerator.KotlinToObjCMethodAdapter>()

    exposedMethods.forEach { method ->
        val baseMethods = mapper.getBaseMethods(method)
        val hasSelectorClash = baseMethods.map { namer.getSelector(it) }.distinct().size > 1

        if (method.isOverridable && !hasSelectorClash) {
            val baseMethod = baseMethods.first()

            val presentVtableBridges = mutableSetOf<Int?>(null)
            val presentMethodTableBridges = mutableSetOf<String>()

            val allOverriddenDescriptors = method.allOverriddenDescriptors.map { it.original }

            val (inherited, uninherited) = allOverriddenDescriptors.partition {
                it != method && mapper.shouldBeExposed(it)
            }

            inherited.forEach {
                presentVtableBridges += vtableIndex(it)
                presentMethodTableBridges += context.ir.get(it).functionName
            }

            uninherited.forEach {
                val vtableIndex = vtableIndex(it)
                val functionName = context.ir.get(it).functionName

                if (vtableIndex !in presentVtableBridges || functionName !in presentMethodTableBridges) {
                    presentVtableBridges += vtableIndex
                    presentMethodTableBridges += functionName
                    reverseAdapters += createReverseAdapter(it, baseMethod, functionName, vtableIndex)
                }
            }

        } else {
            // Mark it as non-overridable:
            baseMethods.distinctBy { namer.getSelector(it) }.forEach { base ->
                reverseAdapters += KotlinToObjCMethodAdapter(
                        namer.getSelector(base),
                        -1,
                        -1,
                        NullPointer(int8Type)
                )
            }

            // TODO: some fake-overrides can be skipped.
        }
    }

    val virtualAdapters = exposedMethods
            .filter { mapper.isBaseMethod(it) && it.isOverridable }
            .map { createMethodVirtualAdapter(it) }

    val irClass = context.ir.get(descriptor)
    val typeInfo = constPointer(codegen.typeInfoValue(irClass))
    val objCName = namer.getClassOrProtocolName(descriptor)

    val vtableSize = if (descriptor.kind == ClassKind.INTERFACE) {
        -1
    } else {
        context.getVtableBuilder(irClass).vtableEntries.size
    }

    val vtable = if (!descriptor.isInterface && !irClass.typeInfoHasVtableAttached) {
        staticData.placeGlobal("", rttiGenerator.vtable(irClass)).also {
            it.setConstant(true)
        }.pointer.getElementPtr(0)
    } else {
        null
    }

    val methodTable = if (!descriptor.isInterface && descriptor.isAbstract()) {
        rttiGenerator.methodTableRecords(irClass)
    } else {
        emptyList()
    }

    when (descriptor.kind) {
        ClassKind.OBJECT -> {
            classAdapters += if (descriptor.isUnit()) {
                createUnitInstanceAdapter()
            } else {
                createObjectInstanceAdapter(descriptor)
            }
        }
        ClassKind.ENUM_CLASS -> {
            descriptor.enumEntries.mapTo(classAdapters) {
                createEnumEntryAdapter(it)
            }
        }
        else -> {
            // Nothing special.
        }
    }

    return ObjCTypeAdapter(
            descriptor,
            typeInfo,
            vtable,
            vtableSize,
            methodTable,
            objCName,
            adapters,
            classAdapters,
            virtualAdapters,
            reverseAdapters
    )
}

internal data class DirectAdapterRequest(val implementation: IrFunction?, val base: FunctionDescriptor)

private fun ObjCExportCodeGenerator.createDirectAdapters(
        method: FunctionDescriptor
): List<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter> {

    fun FunctionDescriptor.getAllRequiredDirectAdapters() = mapper.getBaseMethods(this).map { base ->
        val implementation = if (this.modality == Modality.ABSTRACT) {
            null
        } else {
            OverriddenFunctionDescriptor(
                    context.ir.get(this) as IrSimpleFunction,
                    context.ir.get(base) as IrSimpleFunction
            ).getImplementation(context)
        }
        DirectAdapterRequest(implementation, base)
    }

    val superClassMethod = method.overriddenDescriptors
            .atMostOne { !(it.containingDeclaration as ClassDescriptor).isInterface }?.original

    val inheritedAdapters = superClassMethod?.getAllRequiredDirectAdapters().orEmpty()
    val requiredAdapters = method.getAllRequiredDirectAdapters()

    return (requiredAdapters - inheritedAdapters)
            .distinctBy { namer.getSelector(it.base) }
            .map {
                createMethodAdapter(it)
            }
}

private inline fun ObjCExportCodeGenerator.generateObjCToKotlinMethodAdapter(
        methodBridge: MethodBridge,
        selector: String,
        block: FunctionGenerationContext.() -> Unit
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    val encoding = getEncoding(methodBridge)
    val imp = generateFunction(codegen, objCFunctionType(methodBridge), "") {
        block()
    }

    LLVMSetLinkage(imp, LLVMLinkage.LLVMPrivateLinkage)

    return ObjCToKotlinMethodAdapter(selector, encoding, constPointer(imp))
}

private fun ObjCExportCodeGenerator.createUnitInstanceAdapter() =
        generateObjCToKotlinMethodAdapter(
                MethodBridge(ReferenceBridge, listOf(ReferenceBridge, ReferenceBridge)),
                namer.getObjectInstanceSelector(context.builtIns.unit)
        ) {
            initRuntimeIfNeeded() // For instance methods it gets called when allocating.

            ret(callFromBridge(context.llvm.Kotlin_ObjCExport_convertUnit, listOf(codegen.theUnitInstanceRef.llvm)))
        }

private fun ObjCExportCodeGenerator.createObjectInstanceAdapter(
        descriptor: ClassDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(descriptor.kind == ClassKind.OBJECT)
    assert(!descriptor.isUnit())

    val selector = namer.getObjectInstanceSelector(descriptor)
    val methodBridge = MethodBridge(ReferenceBridge, listOf(ReferenceBridge, ReferenceBridge))

    return generateObjCToKotlinMethodAdapter(methodBridge, selector) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.

        val value = getObjectValue(context.ir.get(descriptor), shared = false, locationInfo = null, exceptionHandler = ExceptionHandler.Caller)
        ret(kotlinToObjC(value, ReferenceBridge))
    }
}

private fun ObjCExportCodeGenerator.createEnumEntryAdapter(
        descriptor: ClassDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(descriptor.kind == ClassKind.ENUM_ENTRY)

    val selector = namer.getEnumEntrySelector(descriptor)
    val methodBridge = MethodBridge(ReferenceBridge, listOf(ReferenceBridge, ReferenceBridge))

    return generateObjCToKotlinMethodAdapter(methodBridge, selector) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.

        val value = getEnumEntry(context.ir.getEnumEntry(descriptor), ExceptionHandler.Caller)
        ret(kotlinToObjC(value, ReferenceBridge))
    }
}

private fun List<CallableMemberDescriptor>.toMethods(): List<FunctionDescriptor> = this.flatMap {
    when (it) {
        is PropertyDescriptor -> listOfNotNull(it.getter, it.setter)
        is FunctionDescriptor -> listOf(it)
        else -> error(it)
    }
}

private fun objCFunctionType(methodBridge: MethodBridge): LLVMTypeRef {
    val paramTypes = mutableListOf<LLVMTypeRef>()

    methodBridge.paramBridges.forEachIndexed { index, typeBridge ->
        paramTypes += typeBridge.objCType
        if (index == 0) paramTypes += int8TypePtr // Selector.
    }

    val returnType = methodBridge.returnBridge.objCType

    return functionType(returnType, false, *(paramTypes.toTypedArray()))
}

private val ObjCValueType.llvmType: LLVMTypeRef get() = when (this) {
    ObjCValueType.BOOL -> int8Type
    ObjCValueType.CHAR -> int8Type
    ObjCValueType.UNSIGNED_SHORT -> int16Type
    ObjCValueType.SHORT -> int16Type
    ObjCValueType.INT -> int32Type
    ObjCValueType.LONG_LONG -> kInt64
    ObjCValueType.FLOAT -> LLVMFloatType()!!
    ObjCValueType.DOUBLE -> LLVMDoubleType()!!
}

private val ReturnableTypeBridge.objCType: LLVMTypeRef get() = when (this) {
    VoidBridge -> voidType
    is ReferenceBridge -> int8TypePtr
    is ValueTypeBridge -> this.objCValueType.llvmType
    is HashCodeBridge -> kInt64 // TODO: only for 64-bit platforms
}

internal fun ObjCExportCodeGenerator.getEncoding(methodBridge: MethodBridge): String {
    var paramOffset = 0
    val pointerSize = runtime.pointerSize

    val params = buildString {
        fun appendParam(encoding: String, size: Int) {
            append(encoding)
            append(paramOffset)
            paramOffset += size
        }

        methodBridge.paramBridges.forEachIndexed { index, typeBridge ->
            appendParam(
                    typeBridge.objCEncoding,
                    LLVMStoreSizeOfType(runtime.targetData, typeBridge.objCType).toInt()
            )
            if (index == 0) appendParam(":", pointerSize)
        }
    }

    val returnTypeEncoding = methodBridge.returnBridge.objCEncoding

    val paramSize = paramOffset
    return "$returnTypeEncoding$paramSize$params"
}

private val ReturnableTypeBridge.objCEncoding: String get() = when (this) {
    VoidBridge -> "v"
    ReferenceBridge -> "@"
    is ValueTypeBridge -> this.objCValueType.encoding
    HashCodeBridge -> "L" // NSUInteger = unsigned long; // TODO: `unsigned int` on watchOS
}

internal fun Context.is64Bit(): Boolean = this.config.target.architecture.bitness == 64

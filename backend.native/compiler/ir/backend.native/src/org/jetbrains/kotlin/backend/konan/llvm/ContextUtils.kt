/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.hash.GlobalHash
import org.jetbrains.kotlin.backend.konan.ir.isReal
import org.jetbrains.kotlin.backend.konan.ir.llvmSymbolOrigin
import org.jetbrains.kotlin.descriptors.konan.CompiledKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.CurrentKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal sealed class SlotType {
    // Frame local arena slot can be used.
    object ARENA : SlotType()

    // Return slot can be used.
    object RETURN : SlotType()

    // Return slot, if it is an arena, can be used.
    object RETURN_IF_ARENA : SlotType()

    // Param slot, if it is an arena, can be used.
    class PARAM_IF_ARENA(val parameter: Int) : SlotType()

    // Params slot, if it is an arena, can be used.
    class PARAMS_IF_ARENA(val parameters: IntArray, val useReturnSlot: Boolean) : SlotType()

    // Anonymous slot.
    object ANONYMOUS : SlotType()

    // Unknown slot type.
    object UNKNOWN : SlotType()
}

// Lifetimes class of reference, computed by escape analysis.
internal sealed class Lifetime(val slotType: SlotType) {
    // If reference is frame-local (only obtained from some call and never leaves).
    object LOCAL : Lifetime(SlotType.ARENA) {
        override fun toString(): String {
            return "LOCAL"
        }
    }

    // If reference is only returned.
    object RETURN_VALUE : Lifetime(SlotType.RETURN) {
        override fun toString(): String {
            return "RETURN_VALUE"
        }
    }

    // If reference is set as field of references of class RETURN_VALUE or INDIRECT_RETURN_VALUE.
    object INDIRECT_RETURN_VALUE : Lifetime(SlotType.RETURN_IF_ARENA) {
        override fun toString(): String {
            return "INDIRECT_RETURN_VALUE"
        }
    }

    // If reference is stored to the field of an incoming parameters.
    class PARAMETER_FIELD(val parameter: Int) : Lifetime(SlotType.PARAM_IF_ARENA(parameter)) {
        override fun toString(): String {
            return "PARAMETER_FIELD($parameter)"
        }
    }

    // If reference is stored to the field of an incoming parameters.
    class PARAMETERS_FIELD(val parameters: IntArray, val useReturnSlot: Boolean)
        : Lifetime(SlotType.PARAMS_IF_ARENA(parameters, useReturnSlot)) {
        override fun toString(): String {
            return "PARAMETERS_FIELD(${parameters.contentToString()}, useReturnSlot='$useReturnSlot')"
        }
    }

    // If reference refers to the global (either global object or global variable).
    object GLOBAL : Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "GLOBAL"
        }
    }

    // If reference used to throw.
    object THROW : Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "THROW"
        }
    }

    // If reference used as an argument of outgoing function. Class can be improved by escape analysis
    // of called function.
    object ARGUMENT : Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "ARGUMENT"
        }
    }

    // If reference class is unknown.
    object UNKNOWN : Lifetime(SlotType.UNKNOWN) {
        override fun toString(): String {
            return "UNKNOWN"
        }
    }

    // If reference class is irrelevant.
    object IRRELEVANT : Lifetime(SlotType.UNKNOWN) {
        override fun toString(): String {
            return "IRRELEVANT"
        }
    }
}

/**
 * Provides utility methods to the implementer.
 */
internal interface ContextUtils : RuntimeAware {
    val context: Context

    override val runtime: Runtime
        get() = context.llvm.runtime

    /**
     * Describes the target platform.
     *
     * TODO: using [llvmTargetData] usually results in generating non-portable bitcode.
     */
    val llvmTargetData: LLVMTargetDataRef
        get() = runtime.targetData

    val staticData: StaticData
        get() = context.llvm.staticData

    /**
     * TODO: maybe it'd be better to replace with [IrDeclaration::isEffectivelyExternal()],
     * or just drop all [else] branches of corresponding conditionals.
     */
    fun isExternal(declaration: IrDeclaration): Boolean {
        return false
    }

    /**
     * LLVM function generated from the Kotlin function.
     * It may be declared as external function prototype.
     */
    val IrFunction.llvmFunction: LLVMValueRef
    get() = llvmFunctionOrNull ?: error("$name in $file/${parent.fqNameForIrSerialization}")

    val IrFunction.llvmFunctionOrNull: LLVMValueRef?
        get() {
            assert(this.isReal)

            return if (isExternal(this)) {
                context.llvm.externalFunction(this.symbolName, getLlvmFunctionType(this),
                        origin = this.llvmSymbolOrigin)
            } else {
                context.llvmDeclarations.forFunctionOrNull(this)?.llvmFunction
            }
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val IrFunction.entryPointAddress: ConstPointer
        get() {
            val result = LLVMConstBitCast(this.llvmFunction, int8TypePtr)!!
            return constPointer(result)
        }

    val IrClass.typeInfoPtr: ConstPointer
        get() {
            return if (isExternal(this)) {
                constPointer(importGlobal(this.typeInfoSymbolName, runtime.typeInfoType,
                        origin = this.llvmSymbolOrigin))
            } else {
                context.llvmDeclarations.forClass(this).typeInfo
            }
        }

    /**
     * Pointer to type info for given class.
     * It may be declared as pointer to external variable.
     */
    val IrClass.llvmTypeInfoPtr: LLVMValueRef
        get() = typeInfoPtr.llvm

    /**
     * Returns contents of this [GlobalHash].
     *
     * It must be declared identically with [Runtime.globalHashType].
     */
    fun GlobalHash.getBytes(): ByteArray {
        val size = GlobalHash.size
        assert(size.toLong() == LLVMStoreSizeOfType(llvmTargetData, runtime.globalHashType))

        return this.bits.getBytes(size)
    }

    /**
     * Returns global hash of this string contents.
     */
    val String.globalHashBytes: ByteArray
        get() = memScoped {
            val hash = globalHash(stringAsBytes(this@globalHashBytes), memScope)
            hash.getBytes()
        }

    /**
     * Return base64 representation for global hash of this string contents.
     */
    val String.globalHashBase64: String
        get() {
            return base64Encode(globalHashBytes)
        }

    val String.globalHash: ConstValue
        get() = memScoped {
            val hashBytes = this@globalHash.globalHashBytes
            return Struct(runtime.globalHashType, ConstArray(int8Type, hashBytes.map { Int8(it) }))
        }

    val FqName.globalHash: ConstValue
        get() = this.toString().globalHash

}

/**
 * Converts this string to the sequence of bytes to be used for hashing/storing to binary/etc.
 */
internal fun stringAsBytes(str: String) = str.toByteArray(Charsets.UTF_8)

internal val String.localHash: LocalHash
    get() = LocalHash(localHash(stringAsBytes(this)))

internal val Name.localHash: LocalHash
    get() = this.toString().localHash

internal val FqName.localHash: LocalHash
    get() = this.toString().localHash


internal class Llvm(val context: Context, val llvmModule: LLVMModuleRef) {

    private fun importFunction(name: String, otherModule: LLVMModuleRef): LLVMValueRef {
        if (LLVMGetNamedFunction(llvmModule, name) != null) {
            throw IllegalArgumentException("function $name already exists")
        }

        val externalFunction = LLVMGetNamedFunction(otherModule, name)!!

        val functionType = getFunctionType(externalFunction)
        val function = LLVMAddFunction(llvmModule, name, functionType)!!

        copyFunctionAttributes(externalFunction, function)

        return function
    }

    private fun importGlobal(name: String, otherModule: LLVMModuleRef): LLVMValueRef {
        if (LLVMGetNamedGlobal(llvmModule, name) != null) {
            throw IllegalArgumentException("global $name already exists")
        }

        val externalGlobal = LLVMGetNamedGlobal(otherModule, name)!!
        val globalType = getGlobalType(externalGlobal)
        val global = LLVMAddGlobal(llvmModule, globalType, name)!!

        return global
    }

    private fun copyFunctionAttributes(source: LLVMValueRef, destination: LLVMValueRef) {
        // TODO: consider parameter attributes
        val attributeIndex = LLVMAttributeFunctionIndex
        val count = LLVMGetAttributeCountAtIndex(source, attributeIndex)
        memScoped {
            val attributes = allocArray<LLVMAttributeRefVar>(count)
            LLVMGetAttributesAtIndex(source, attributeIndex, attributes)
            (0 until count).forEach {
                LLVMAddAttributeAtIndex(destination, attributeIndex, attributes[it])
            }
        }
    }

    private fun importMemset(): LLVMValueRef {
        val parameterTypes = cValuesOf(int8TypePtr, int8Type, int32Type, int32Type, int1Type)
        val functionType = LLVMFunctionType(LLVMVoidType(), parameterTypes, 5, 0)
        return LLVMAddFunction(llvmModule, "llvm.memset.p0i8.i32", functionType)!!
    }

    private fun importMemcpy(): LLVMValueRef {
        val parameterTypes = cValuesOf(int8TypePtr, int8TypePtr, int32Type, int1Type)
        val functionType = LLVMFunctionType(LLVMVoidType(), parameterTypes, 4, 0)
        return LLVMAddFunction(llvmModule, "llvm.memcpy.p0i8.p0i8.i32", functionType)!!
    }

    private fun llvmIntrinsic(name: String, type: LLVMTypeRef, vararg attributes: String): LLVMValueRef {
        val result = LLVMAddFunction(llvmModule, name, type)!!
        attributes.forEach {
            val kindId = getLlvmAttributeKindId(it)
            val attribute = LLVMCreateEnumAttribute(LLVMGetTypeContext(type), kindId, 0)!!
            LLVMAddAttributeAtIndex(result, LLVMAttributeFunctionIndex, attribute)
        }
        return result
    }

    internal fun externalFunction(
            name: String,
            type: LLVMTypeRef,
            origin: CompiledKonanModuleOrigin,
            independent: Boolean = false
    ): LLVMValueRef {
        this.imports.add(origin, onlyBitcode = independent)

        val found = LLVMGetNamedFunction(llvmModule, name)
        if (found != null) {
            assert(getFunctionType(found) == type) {
                "Expected: ${LLVMPrintTypeToString(type)!!.toKString()} " +
                        "found: ${LLVMPrintTypeToString(getFunctionType(found))!!.toKString()}"
            }
            assert(LLVMGetLinkage(found) == LLVMLinkage.LLVMExternalLinkage)
            return found
        } else {
            // As exported functions are written in C++ they assume sign extension for promoted types -
            // mention that in attributes.
            val function = LLVMAddFunction(llvmModule, name, type)!!
            return memScoped {
                val paramCount = LLVMCountParamTypes(type)
                val paramTypes = allocArray<LLVMTypeRefVar>(paramCount)
                LLVMGetParamTypes(type, paramTypes)
                (0 until paramCount).forEach { index ->
                    val paramType = paramTypes[index]
                    addFunctionSignext(function, index + 1, paramType)
                }
                val returnType = LLVMGetReturnType(type)
                addFunctionSignext(function, 0, returnType)
                function
            }
        }
    }

    private fun externalNounwindFunction(name: String, type: LLVMTypeRef, origin: CompiledKonanModuleOrigin): LLVMValueRef {
        val function = externalFunction(name, type, origin)
        setFunctionNoUnwind(function)
        return function
    }

    val imports get() = context.llvmImports

    class ImportsImpl(private val context: Context) : LlvmImports {

        private val usedBitcode = mutableSetOf<KonanLibrary>()
        private val usedNativeDependencies = mutableSetOf<KonanLibrary>()

        private val allLibraries by lazy { context.librariesWithDependencies.toSet() }

        override fun add(origin: CompiledKonanModuleOrigin, onlyBitcode: Boolean) {
            val library = when (origin) {
                CurrentKonanModuleOrigin -> return
                is DeserializedKonanModuleOrigin -> origin.library
            }

            if (library !in allLibraries) {
                error("Library (${library.libraryName}) is used but not requested.\nRequested libraries: ${allLibraries.joinToString { it.libraryName }}")
            }

            usedBitcode.add(library)
            if (!onlyBitcode) {
                usedNativeDependencies.add(library)
            }
        }

        override fun bitcodeIsUsed(library: KonanLibrary) = library in usedBitcode

        override fun nativeDependenciesAreUsed(library: KonanLibrary) = library in usedNativeDependencies
    }

    val nativeDependenciesToLink: List<KonanLibrary> by lazy {
        context.config.resolvedLibraries
                .getFullList(TopologicalLibraryOrder)
                .filter { (!it.isDefault && !context.config.purgeUserLibs) || imports.nativeDependenciesAreUsed(it) }

    }

    val bitcodeToLink: List<KonanLibrary> by lazy {
        context.config.resolvedLibraries
                .getFullList(TopologicalLibraryOrder)
                .filter { (!it.isDefault && !context.config.purgeUserLibs) || imports.bitcodeIsUsed(it) }
    }

    val additionalProducedBitcodeFiles = mutableListOf<String>()

    val staticData = StaticData(context)

    private val target = context.config.target

    val runtimeFile = context.config.distribution.runtime(target)
    val runtime = Runtime(runtimeFile) // TODO: dispose

    val targetTriple = runtime.target

    init {
        LLVMSetDataLayout(llvmModule, runtime.dataLayout)
        LLVMSetTarget(llvmModule, targetTriple)
    }

    private fun importRtFunction(name: String) = importFunction(name, runtime.llvmModule)

    private fun importRtGlobal(name: String) = importGlobal(name, runtime.llvmModule)

    val allocInstanceFunction = importRtFunction("AllocInstance")
    val allocArrayFunction = importRtFunction("AllocArrayInstance")
    val initInstanceFunction = importRtFunction("InitInstance")
    val initSharedInstanceFunction = importRtFunction("InitSharedInstance")
    val updateHeapRefFunction = importRtFunction("UpdateHeapRef")
    val enterFrameFunction = importRtFunction("EnterFrame")
    val leaveFrameFunction = importRtFunction("LeaveFrame")
    val getReturnSlotIfArenaFunction = importRtFunction("GetReturnSlotIfArena")
    val getParamSlotIfArenaFunction = importRtFunction("GetParamSlotIfArena")
    val lookupOpenMethodFunction = importRtFunction("LookupOpenMethod")
    val isInstanceFunction = importRtFunction("IsInstance")
    val checkInstanceFunction = importRtFunction("CheckInstance")
    val throwExceptionFunction = importRtFunction("ThrowException")
    val appendToInitalizersTail = importRtFunction("AppendToInitializersTail")
    val initRuntimeIfNeeded = importRtFunction("Kotlin_initRuntimeIfNeeded")
    val mutationCheck = importRtFunction("MutationCheck")
    val freezeSubgraph = importRtFunction("FreezeSubgraph")
    val checkMainThread = importRtFunction("CheckIsMainThread")

    val createKotlinObjCClass by lazy { importRtFunction("CreateKotlinObjCClass") }
    val getObjCKotlinTypeInfo by lazy { importRtFunction("GetObjCKotlinTypeInfo") }
    val missingInitImp by lazy { importRtFunction("MissingInitImp") }

    val Kotlin_Interop_DoesObjectConformToProtocol by lazyRtFunction
    val Kotlin_Interop_IsObjectKindOfClass by lazyRtFunction

    val Kotlin_ObjCExport_refToObjC by lazyRtFunction
    val Kotlin_ObjCExport_refFromObjC by lazyRtFunction
    val Kotlin_ObjCExport_CreateNSStringFromKString by lazyRtFunction
    val Kotlin_Interop_CreateNSArrayFromKList by lazyRtFunction
    val Kotlin_Interop_CreateNSMutableArrayFromKList by lazyRtFunction
    val Kotlin_Interop_CreateNSSetFromKSet by lazyRtFunction
    val Kotlin_Interop_CreateKotlinMutableSetFromKSet by lazyRtFunction
    val Kotlin_Interop_CreateNSDictionaryFromKMap by lazyRtFunction
    val Kotlin_Interop_CreateKotlinMutableDictonaryFromKMap by lazyRtFunction
    val Kotlin_ObjCExport_convertUnit by lazyRtFunction
    val Kotlin_ObjCExport_GetAssociatedObject by lazyRtFunction
    val Kotlin_ObjCExport_AbstractMethodCalled by lazyRtFunction
    val Kotlin_ObjCExport_RethrowExceptionAsNSError by lazyRtFunction
    val Kotlin_ObjCExport_RethrowNSErrorAsException by lazyRtFunction
    val Kotlin_ObjCExport_AllocInstanceWithAssociatedObject by lazyRtFunction

    val tlsMode by lazy {
        when (target) {
            KonanTarget.WASM32,
            is KonanTarget.ZEPHYR -> LLVMThreadLocalMode.LLVMNotThreadLocal
            else -> LLVMThreadLocalMode.LLVMGeneralDynamicTLSModel
        }
    }

    private val personalityFunctionName = when (target) {
        KonanTarget.IOS_ARM32 -> "__gxx_personality_sj0"
        KonanTarget.MINGW_X64 -> "__gxx_personality_seh0"
        else -> "__gxx_personality_v0"
    }

    val cxxStdTerminate = externalNounwindFunction(
            "_ZSt9terminatev", // mangled C++ 'std::terminate'
            functionType(voidType, false),
            origin = context.standardLlvmSymbolsOrigin
    )

    val gxxPersonalityFunction = externalNounwindFunction(
            personalityFunctionName,
            functionType(int32Type, true),
            origin = context.standardLlvmSymbolsOrigin
    )
    val cxaBeginCatchFunction = externalNounwindFunction(
            "__cxa_begin_catch",
            functionType(int8TypePtr, false, int8TypePtr),
            origin = context.standardLlvmSymbolsOrigin
    )
    val cxaEndCatchFunction = externalNounwindFunction(
            "__cxa_end_catch",
            functionType(voidType, false),
            origin = context.standardLlvmSymbolsOrigin
    )

    val memsetFunction = importMemset()
    //val memcpyFunction = importMemcpy()

    val llvmTrap = llvmIntrinsic(
            "llvm.trap",
            functionType(voidType, false),
            "cold", "noreturn", "nounwind"
    )

    val llvmEhTypeidFor = llvmIntrinsic(
            "llvm.eh.typeid.for",
            functionType(int32Type, false, int8TypePtr),
            "nounwind", "readnone"
    )

    val usedFunctions = mutableListOf<LLVMValueRef>()
    val usedGlobals = mutableListOf<LLVMValueRef>()
    val compilerUsedGlobals = mutableListOf<LLVMValueRef>()
    val staticInitializers = mutableListOf<LLVMValueRef>()
    val fileInitializers = mutableListOf<IrField>()
    val objects = mutableSetOf<LLVMValueRef>()
    val sharedObjects = mutableSetOf<LLVMValueRef>()

    private object lazyRtFunction {
        operator fun provideDelegate(
                thisRef: Llvm, property: KProperty<*>
        ) = object : ReadOnlyProperty<Llvm, LLVMValueRef> {

            val value by lazy { thisRef.importRtFunction(property.name) }

            override fun getValue(thisRef: Llvm, property: KProperty<*>): LLVMValueRef = value
        }
    }
    val llvmInt8 = LLVMInt8Type()!!
    val llvmInt16 = LLVMInt16Type()!!
    val llvmInt32 = LLVMInt32Type()!!
    val llvmInt64 = LLVMInt64Type()!!
    val llvmFloat = LLVMFloatType()!!
    val llvmDouble = LLVMDoubleType()!!
}

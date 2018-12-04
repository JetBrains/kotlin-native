/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.common.DumpIrTreeWithDescriptorsVisitor
import org.jetbrains.kotlin.backend.common.validateIrModule
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_BRIDGE_METHOD
import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.getName
import java.lang.System.out
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KProperty

internal class SpecialDeclarationsFactory(val context: Context) {
    private val enumSpecialDeclarationsFactory by lazy { EnumSpecialDeclarationsFactory(context) }
    private val outerThisFields = mutableMapOf<ClassDescriptor, IrField>()
    private val bridgesDescriptors = mutableMapOf<Pair<IrSimpleFunction, BridgeDirections>, IrSimpleFunction>()
    private val loweredEnums = mutableMapOf<IrClass, LoweredEnum>()
    private val ordinals = mutableMapOf<ClassDescriptor, Map<ClassDescriptor, Int>>()

    object DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS :
            IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")

    fun getOuterThisField(innerClass: IrClass): IrField =
        if (!innerClass.descriptor.isInner) throw AssertionError("Class is not inner: ${innerClass.descriptor}")
        else outerThisFields.getOrPut(innerClass.descriptor) {
            val outerClass = innerClass.parent as? IrClass
                    ?: throw AssertionError("No containing class for inner class ${innerClass.descriptor}")

            val receiver = ReceiverParameterDescriptorImpl(
                    innerClass.descriptor,
                    ImplicitClassReceiver(innerClass.descriptor, null),
                    Annotations.EMPTY
            )
            val descriptor = PropertyDescriptorImpl.create(
                    innerClass.descriptor, Annotations.EMPTY, Modality.FINAL,
                    Visibilities.PRIVATE, false, "this$0".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE, false, false, false, false, false, false
            ).apply {
                this.setType(outerClass.descriptor.defaultType, emptyList(), receiver, null)
                initialize(null, null)
            }

            IrFieldImpl(
                    innerClass.descriptor.startOffsetOrUndefined,
                    innerClass.descriptor.endOffsetOrUndefined,
                    DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS,
                    descriptor,
                    outerClass.defaultType
            )
        }

    fun getLoweredEnum(enumClass: IrClass): LoweredEnum {
        assert(enumClass.kind == ClassKind.ENUM_CLASS, { "Expected enum class but was: ${enumClass.descriptor}" })
        return loweredEnums.getOrPut(enumClass) {
            enumSpecialDeclarationsFactory.createLoweredEnum(enumClass)
        }
    }

    private fun assignOrdinalsToEnumEntries(classDescriptor: ClassDescriptor): Map<ClassDescriptor, Int> {
        val enumEntryOrdinals = mutableMapOf<ClassDescriptor, Int>()
        classDescriptor.enumEntries.forEachIndexed { index, entry ->
            enumEntryOrdinals[entry] = index
        }
        return enumEntryOrdinals
    }

    fun getEnumEntryOrdinal(entryDescriptor: ClassDescriptor): Int {
        val enumClassDescriptor = entryDescriptor.containingDeclaration as ClassDescriptor
        // If enum came from another module then we need to get serialized ordinal number.
        // We serialize ordinal because current serialization cannot preserve enum entry order.
        if (enumClassDescriptor is DeserializedClassDescriptor) {
            return enumClassDescriptor.classProto.enumEntryList
                    .first { entryDescriptor.name == enumClassDescriptor.c.nameResolver.getName(it.name) }
                    .getExtension(KonanProtoBuf.enumEntryOrdinal)
        }
        return ordinals.getOrPut(enumClassDescriptor) { assignOrdinalsToEnumEntries(enumClassDescriptor) }[entryDescriptor]!!
    }

    fun getBridge(overriddenFunctionDescriptor: OverriddenFunctionDescriptor): IrSimpleFunction {
        val irFunction = overriddenFunctionDescriptor.descriptor
        assert(overriddenFunctionDescriptor.needBridge,
                { "Function ${irFunction.descriptor} is not needed in a bridge to call overridden function ${overriddenFunctionDescriptor.overriddenDescriptor.descriptor}" })
        val bridgeDirections = overriddenFunctionDescriptor.bridgeDirections
        return bridgesDescriptors.getOrPut(irFunction to bridgeDirections) {
            createBridge(irFunction, bridgeDirections)
        }
    }

    private fun createBridge(function: IrFunction, bridgeDirections: BridgeDirections): IrFunctionImpl {
        val returnType = when (bridgeDirections.array[0]) {
            BridgeDirection.TO_VALUE_TYPE,
            BridgeDirection.NOT_NEEDED -> function.returnType
            BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyNType
        }

        val dispatchReceiver = when (bridgeDirections.array[1]) {
            BridgeDirection.TO_VALUE_TYPE   -> function.dispatchReceiverParameter!!
            BridgeDirection.NOT_NEEDED      -> function.dispatchReceiverParameter
            BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyClass.owner.thisReceiver!!
        }

        val extensionReceiverType = when (bridgeDirections.array[2]) {
            BridgeDirection.TO_VALUE_TYPE   -> function.extensionReceiverParameter!!.type
            BridgeDirection.NOT_NEEDED      -> function.extensionReceiverParameter?.type
            BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyNType
        }

        val valueParameterTypes = function.valueParameters.mapIndexed { index, valueParameter ->
            when (bridgeDirections.array[index + 3]) {
                BridgeDirection.TO_VALUE_TYPE   -> valueParameter.type
                BridgeDirection.NOT_NEEDED      -> valueParameter.type
                BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyNType
            }
        }
        val bridgeDescriptor = createBridgeDescriptor(
                function,
                bridgeDirections,
                returnType,
                dispatchReceiver,
                extensionReceiverType,
                valueParameterTypes
        )

        val bridge = IrFunctionImpl(
                function.startOffset,
                function.endOffset,
                DECLARATION_ORIGIN_BRIDGE_METHOD(function),
                bridgeDescriptor,
                returnType
        ).apply {
            this.parent = function.parent
        }

        bridge.dispatchReceiverParameter = dispatchReceiver?.let {
            IrValueParameterImpl(
                    it.startOffset,
                    it.endOffset,
                    IrDeclarationOrigin.DEFINED,
                    it.descriptor,
                    it.type,
                    null
            )
        }

        extensionReceiverType?.let {
            val extensionReceiverParameter = function.extensionReceiverParameter!!
            bridge.extensionReceiverParameter = IrValueParameterImpl(
                    extensionReceiverParameter.startOffset,
                    extensionReceiverParameter.endOffset,
                    extensionReceiverParameter.origin,
                    bridge.descriptor.extensionReceiverParameter!!,
                    it, null
            )
        }
        function.valueParameters.mapIndexedTo(bridge.valueParameters) { index, valueParameter ->
            val type = valueParameterTypes[index]

            IrValueParameterImpl(
                    valueParameter.startOffset,
                    valueParameter.endOffset,
                    valueParameter.origin,
                    bridge.descriptor.valueParameters[index],
                    type, valueParameter.varargElementType
            )
        }

        function.typeParameters.mapTo(bridge.typeParameters) {
            IrTypeParameterImpl(it.startOffset, it.endOffset, it.origin, it.descriptor).apply {
                superTypes += it.superTypes
            }
        }

        return bridge
    }

    private fun createBridgeDescriptor(
            function: IrFunction,
            bridgeDirections: BridgeDirections,
            returnType: IrType,
            dispatchReceiverParameter: IrValueParameter?,
            extensionReceiverType: IrType?,
            valueParameterTypes: List<IrType>
    ): SimpleFunctionDescriptor {

        val descriptor = function.descriptor
        val bridgeDescriptor = SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration = */ descriptor.containingDeclaration,
                /* annotations           = */ Annotations.EMPTY,
                /* name                  = */ "<bridge-$bridgeDirections>${function.functionName}".synthesizedName,
                /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                /* source                = */ SourceElement.NO_SOURCE)

        val valueParameters = descriptor.valueParameters.mapIndexed { index, valueParameterDescriptor ->
            ValueParameterDescriptorImpl(
                    containingDeclaration = valueParameterDescriptor.containingDeclaration,
                    original = null,
                    index = index,
                    annotations = Annotations.EMPTY,
                    name = valueParameterDescriptor.name,
                    outType = valueParameterTypes[index].toKotlinType(),
                    declaresDefaultValue = valueParameterDescriptor.declaresDefaultValue(),
                    isCrossinline = valueParameterDescriptor.isCrossinline,
                    isNoinline = valueParameterDescriptor.isNoinline,
                    varargElementType = valueParameterDescriptor.varargElementType,
                    source = SourceElement.NO_SOURCE)
        }
        bridgeDescriptor.initialize(
                /* receiverParameterType        = */ extensionReceiverType?.toKotlinType(),
                /* dispatchReceiverParameter    = */ dispatchReceiverParameter?.descriptor as ReceiverParameterDescriptor?,
                /* typeParameters               = */ descriptor.typeParameters,
                /* unsubstitutedValueParameters = */ valueParameters,
                /* unsubstitutedReturnType      = */ returnType.toKotlinType(),
                /* modality                     = */ descriptor.modality,
                /* visibility                   = */ descriptor.visibility).apply {
            isSuspend = descriptor.isSuspend
        }
        return bridgeDescriptor
    }
}

internal class Context(config: KonanConfig) : KonanBackendContext(config) {
    override val declarationFactory
        get() = TODO("not implemented")

    override fun getClass(fqName: FqName): ClassDescriptor {
        TODO("not implemented")
    }

    lateinit var moduleDescriptor: ModuleDescriptor

    override val builtIns: KonanBuiltIns by lazy(PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    private val packageScope by lazy { builtIns.builtInsModule.getPackage(KonanFqNames.internalPackageName).memberScope }

    val nativePtr by lazy { packageScope.getContributedClassifier(NATIVE_PTR_NAME) as ClassDescriptor }
    val nativePtrPlusLong by lazy { nativePtr.unsubstitutedMemberScope.getContributedFunctions("plus").single() }
    val nativePtrToLong   by lazy { nativePtr.unsubstitutedMemberScope.getContributedFunctions("toLong").single() }
    val getNativeNullPtr  by lazy { packageScope.getContributedFunctions("getNativeNullPtr").single() }
    val immutableBlobOf by lazy {
        builtIns.builtInsModule.getPackage(KonanFqNames.packageName).memberScope.getContributedFunctions("immutableBlobOf").single()
    }

    val specialDeclarationsFactory = SpecialDeclarationsFactory(this)

    class LazyMember<T>(val initializer: Context.() -> T) {
        operator fun getValue(thisRef: Context, property: KProperty<*>): T = thisRef.getValue(this)
    }

    companion object {
        fun <T> lazyMember(initializer: Context.() -> T) = LazyMember<T>(initializer)

        fun <K, V> lazyMapMember(initializer: Context.(K) -> V): LazyMember<(K) -> V> = lazyMember {
            val storage = mutableMapOf<K, V>()
            val result: (K) -> V = {
                storage.getOrPut(it, { initializer(it) })
            }
            result
        }
    }

    private val lazyValues = mutableMapOf<LazyMember<*>, Any?>()

    fun <T> getValue(member: LazyMember<T>): T =
            @Suppress("UNCHECKED_CAST") (lazyValues.getOrPut(member, { member.initializer(this) }) as T)

    val reflectionTypes: KonanReflectionTypes by lazy(PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor, KonanFqNames.internalPackageName)
    }

    private val vtableBuilders = mutableMapOf<IrClass, ClassVtablesBuilder>()

    fun getVtableBuilder(classDescriptor: IrClass) = vtableBuilders.getOrPut(classDescriptor) {
        ClassVtablesBuilder(classDescriptor, this)
    }

    // We serialize untouched descriptor tree and inline IR bodies
    // right after the frontend.
    // But we have to wait until the code generation phase,
    // to dump this information into generated file.
    var serializedLinkData: LinkData? = null
    var dataFlowGraph: ByteArray? = null

    @Deprecated("")
    lateinit var psi2IrGeneratorContext: GeneratorContext

    val librariesWithDependencies by lazy {
        config.librariesWithDependencies(moduleDescriptor)
    }

    var functionReferenceCount = 0
    var coroutineCount = 0

    fun needGlobalInit(field: IrField): Boolean {
        if (field.descriptor.containingDeclaration !is PackageFragmentDescriptor) return false
        // TODO: add some smartness here. Maybe if package of the field is in never accessed
        // assume its global init can be actually omitted.
        return true
    }

    // TODO: make lateinit?
    var irModule: IrModuleFragment? = null
        set(module) {
            if (field != null) {
                throw Error("Another IrModule in the context.")
            }
            field = module!!

            ir = KonanIr(this, module)
        }

    override lateinit var ir: KonanIr

    override val irBuiltIns
        get() = ir.irModule.irBuiltins

    val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns, nativePtr)
    }

    var llvmModule: LLVMModuleRef? = null
        set(module) {
            if (field != null) {
                throw Error("Another LLVMModule in the context.")
            }
            field = module!!

            llvm = Llvm(this, module)
            debugInfo = DebugInfo(this)
        }

    lateinit var llvm: Llvm
    lateinit var llvmDeclarations: LlvmDeclarations
    lateinit var bitcodeFileName: String
    lateinit var library: KonanLibraryWriter

    var phase: KonanPhase? = null
    var depth: Int = 0

    lateinit var privateFunctions: List<Pair<IrFunction, DataFlowIR.FunctionSymbol.Declared>>
    lateinit var privateClasses: List<Pair<IrClass, DataFlowIR.Type.Declared>>

    // Cache used for source offset->(line,column) mapping.
    val fileEntryCache = mutableMapOf<String, SourceManager.FileEntry>()

    protected fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    fun verifyDescriptors() {
        // TODO: Nothing here for now.
    }

    fun printDescriptors() {
        // A workaround to check if the lateinit field is assigned, see KT-9327
        try { moduleDescriptor } catch (e: UninitializedPropertyAccessException) { return }

        separator("Descriptors after: ${phase?.description}")
        moduleDescriptor.deepPrint()
    }

    fun verifyIr() {
        val module = irModule ?: return
        validateIrModule(this, module)
    }

    fun printIr() {
        if (irModule == null) return
        separator("IR after: ${phase?.description}")
        irModule!!.accept(DumpIrTreeVisitor(out), "")
    }

    fun printIrWithDescriptors() {
        if (irModule == null) return
        separator("IR after: ${phase?.description}")
        irModule!!.accept(DumpIrTreeWithDescriptorsVisitor(out), "")
    }

    fun printLocations() {
        if (irModule == null) return
        separator("Locations after: ${phase?.description}")
        irModule!!.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFile(declaration: IrFile) {
                val fileEntry = declaration.fileEntry
                declaration.acceptChildren(object: IrElementVisitor<Unit, Int> {
                    override fun visitElement(element: IrElement, data: Int) {
                        for (i in 0..data) print("  ")
                        println("${element.javaClass.name}: ${fileEntry.range(element)}")
                        element.acceptChildren(this, data + 1)
                    }
                }, 0)
            }

            fun SourceManager.FileEntry.range(element:IrElement):String {
                try {
                    /* wasn't use multi line string to prevent appearing odd line
                     * breaks in the dump. */
                    return "${this.name}: ${this.line(element.startOffset)}" +
                          ":${this.column(element.startOffset)} - " +
                          "${this.line(element.endOffset)}" +
                          ":${this.column(element.endOffset)}"

                } catch (e:Exception) {
                    return "${this.name}: ERROR(${e.javaClass.name}): ${e.message}"
                }
            }
            fun SourceManager.FileEntry.line(offset:Int) = this.getLineNumber(offset)
            fun SourceManager.FileEntry.column(offset:Int) = this.getColumnNumber(offset)
        })
    }

    fun verifyBitCode() {
        if (llvmModule == null) return
        verifyModule(llvmModule!!)
    }

    fun printBitCode() {
        if (llvmModule == null) return
        separator("BitCode after: ${phase?.description}")
        LLVMDumpModule(llvmModule!!)
    }

    fun verify() {
        verifyDescriptors()
        verifyIr()
        verifyBitCode()
    }

    fun print() {
        printDescriptors()
        printIr()
        printBitCode()
    }

    fun shouldVerifyDescriptors() = config.configuration.getBoolean(KonanConfigKeys.VERIFY_DESCRIPTORS)

    fun shouldVerifyIr() = config.configuration.getBoolean(KonanConfigKeys.VERIFY_IR)

    fun shouldVerifyBitCode() = config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)

    fun shouldPrintDescriptors() = config.configuration.getBoolean(KonanConfigKeys.PRINT_DESCRIPTORS)

    fun shouldPrintIr() = config.configuration.getBoolean(KonanConfigKeys.PRINT_IR)

    fun shouldPrintIrWithDescriptors()=
            config.configuration.getBoolean(KonanConfigKeys.PRINT_IR_WITH_DESCRIPTORS)

    fun shouldPrintBitCode() = config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE)

    fun shouldPrintLocations() = config.configuration.getBoolean(KonanConfigKeys.PRINT_LOCATIONS)

    fun shouldProfilePhases() = config.configuration.getBoolean(KonanConfigKeys.TIME_PHASES)

    fun shouldContainDebugInfo() = config.configuration.getBoolean(KonanConfigKeys.DEBUG)

    fun shouldOptimize() = config.configuration.getBoolean(KonanConfigKeys.OPTIMIZATION)

    override fun log(message: () -> String) {
        if (phase?.verbose ?: false) {
            println(message())
        }
    }

    lateinit var debugInfo: DebugInfo

    val isNativeLibrary: Boolean by lazy {
        val kind = config.configuration.get(KonanConfigKeys.PRODUCE)
        kind == CompilerOutputKind.DYNAMIC || kind == CompilerOutputKind.STATIC
    }

    internal val stdlibModule
        get() = this.builtIns.any.module
}

private fun MemberScope.getContributedClassifier(name: String) =
        this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedFunctions(name: String) =
        this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

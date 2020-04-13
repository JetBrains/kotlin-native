/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.common.DumpIrTreeWithDescriptorsVisitor
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_BRIDGE_METHOD
import org.jetbrains.kotlin.backend.konan.optimizations.Devirtualization
import org.jetbrains.kotlin.backend.konan.optimizations.ExternalModulesDFG
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import java.lang.System.out
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyToWithoutSuperTypes
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.library.SerializedIrModule

/**
 * Offset for synthetic elements created by lowerings and not attributable to other places in the source code.
 */

internal class SpecialDeclarationsFactory(val context: Context) {
    private val enumSpecialDeclarationsFactory by lazy { EnumSpecialDeclarationsFactory(context) }
    private val outerThisFields = mutableMapOf<ClassDescriptor, IrField>()
    private val bridgesDescriptors = mutableMapOf<Pair<IrSimpleFunction, BridgeDirections>, IrSimpleFunction>()
    private val loweredEnums = mutableMapOf<IrClass, LoweredEnum>()
    private val ordinals = mutableMapOf<ClassDescriptor, Map<ClassDescriptor, Int>>()

    val loweredInlineFunctions = mutableSetOf<IrFunction>()

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
                    innerClass.startOffset,
                    innerClass.endOffset,
                    DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS,
                    descriptor,
                    outerClass.defaultType
            ).apply {
                parent = innerClass
            }
        }

    fun getLoweredEnum(enumClass: IrClass): LoweredEnum {
        assert(enumClass.kind == ClassKind.ENUM_CLASS) { "Expected enum class but was: ${enumClass.descriptor}" }
        return loweredEnums.getOrPut(enumClass) {
            enumSpecialDeclarationsFactory.createLoweredEnum(enumClass)
        }
    }

    fun getEnumEntryOrdinal(enumEntry: IrEnumEntry) =
            enumEntry.parentAsClass.declarations.filterIsInstance<IrEnumEntry>().indexOf(enumEntry)

    fun getBridge(overriddenFunction: OverriddenFunctionInfo): IrSimpleFunction {
        val irFunction = overriddenFunction.function
        assert(overriddenFunction.needBridge) {
            "Function ${irFunction.descriptor} is not needed in a bridge to call overridden function ${overriddenFunction.overriddenFunction.descriptor}"
        }
        val bridgeDirections = overriddenFunction.bridgeDirections
        return bridgesDescriptors.getOrPut(irFunction to bridgeDirections) {
            createBridge(irFunction, bridgeDirections)
        }
    }

    private fun createBridge(function: IrSimpleFunction,
                             bridgeDirections: BridgeDirections) = WrappedSimpleFunctionDescriptor().let { descriptor ->
        val startOffset = function.startOffset
        val endOffset = function.endOffset
        val returnType = when (bridgeDirections.array[0]) {
            BridgeDirection.TO_VALUE_TYPE,
            BridgeDirection.NOT_NEEDED -> function.returnType
            BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyNType
        }
        IrFunctionImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_BRIDGE_METHOD(function),
                IrSimpleFunctionSymbolImpl(descriptor),
                "<bridge-$bridgeDirections>${function.functionName}".synthesizedName,
                function.visibility,
                function.modality,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = function.isSuspend,
                returnType = returnType,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false
        ).apply {
            val bridge = this
            descriptor.bind(bridge)
            parent = function.parent

            val dispatchReceiver = when (bridgeDirections.array[1]) {
                BridgeDirection.TO_VALUE_TYPE -> function.dispatchReceiverParameter!!
                BridgeDirection.NOT_NEEDED -> function.dispatchReceiverParameter
                BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyClass.owner.thisReceiver!!
            }

            val extensionReceiver = when (bridgeDirections.array[2]) {
                BridgeDirection.TO_VALUE_TYPE -> function.extensionReceiverParameter!!
                BridgeDirection.NOT_NEEDED -> function.extensionReceiverParameter
                BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyClass.owner.thisReceiver!!
            }

            val valueParameterTypes = function.valueParameters.mapIndexed { index, valueParameter ->
                when (bridgeDirections.array[index + 3]) {
                    BridgeDirection.TO_VALUE_TYPE -> valueParameter.type
                    BridgeDirection.NOT_NEEDED -> valueParameter.type
                    BridgeDirection.FROM_VALUE_TYPE -> context.irBuiltIns.anyNType
                }
            }

            dispatchReceiverParameter = dispatchReceiver?.copyTo(bridge)
            extensionReceiverParameter = extensionReceiver?.copyTo(bridge)
            valueParameters += function.valueParameters.map { it.copyTo(bridge, type = valueParameterTypes[it.index]) }

            typeParameters += function.typeParameters.map { parameter ->
                parameter.copyToWithoutSuperTypes(bridge).also { it.superTypes += parameter.superTypes }
            }
        }
    }
}

internal class Context(config: KonanConfig) : KonanBackendContext(config) {
    override val lateinitNullableFields = mutableMapOf<IrField, IrField>()
    lateinit var frontendServices: FrontendServices
    lateinit var environment: KotlinCoreEnvironment
    lateinit var bindingContext: BindingContext

    override val declarationFactory
        get() = TODO("not implemented")

    lateinit var moduleDescriptor: ModuleDescriptor

    lateinit var objCExport: ObjCExport

    lateinit var cAdapterGenerator: CAdapterGenerator

    lateinit var expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>

    override val builtIns: KonanBuiltIns by lazy(PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    override val configuration get() = config.configuration

    override val internalPackageFqn: FqName = RuntimeNames.kotlinNativeInternalPackageName

    val phaseConfig = config.phaseConfig

    private val packageScope by lazy { builtIns.builtInsModule.getPackage(KonanFqNames.internalPackageName).memberScope }

    val nativePtr by lazy { packageScope.getContributedClassifier(NATIVE_PTR_NAME) as ClassDescriptor }
    val nonNullNativePtr by lazy { packageScope.getContributedClassifier(NON_NULL_NATIVE_PTR_NAME) as ClassDescriptor }
    val getNativeNullPtr  by lazy { packageScope.getContributedFunctions("getNativeNullPtr").single() }
    val immutableBlobOf by lazy {
        builtIns.builtInsModule.getPackage(KonanFqNames.packageName).memberScope.getContributedFunctions("immutableBlobOf").single()
    }

    val specialDeclarationsFactory = SpecialDeclarationsFactory(this)

    open class LazyMember<T>(val initializer: Context.() -> T) {
        operator fun getValue(thisRef: Context, property: KProperty<*>): T = thisRef.getValue(this)
    }

    class LazyVarMember<T>(initializer: Context.() -> T) : LazyMember<T>(initializer) {
        operator fun setValue(thisRef: Context, property: KProperty<*>, newValue: T) = thisRef.setValue(this, newValue)
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

        fun <T> nullValue() = LazyVarMember<T?>({ null })
    }

    private val lazyValues = mutableMapOf<LazyMember<*>, Any?>()

    fun <T> getValue(member: LazyMember<T>): T =
            @Suppress("UNCHECKED_CAST") (lazyValues.getOrPut(member, { member.initializer(this) }) as T)

    fun <T> setValue(member: LazyVarMember<T>, newValue: T) {
        lazyValues[member] = newValue
    }

    val reflectionTypes: KonanReflectionTypes by lazy(PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor, KonanFqNames.internalPackageName)
    }

    val layoutBuilders = mutableMapOf<IrClass, ClassLayoutBuilder>()

    fun getLayoutBuilder(irClass: IrClass) = layoutBuilders.getOrPut(irClass) {
        ClassLayoutBuilder(irClass, this)
    }

    lateinit var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    // We serialize untouched descriptor tree and IR.
    // But we have to wait until the code generation phase,
    // to dump this information into generated file.
    var serializedMetadata: SerializedMetadata? = null
    var serializedIr: SerializedIrModule? = null
    var dataFlowGraph: ByteArray? = null

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

    lateinit var irModules: Map<String, IrModuleFragment>

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
        InteropBuiltIns(this.builtIns)
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
    val llvmImports: LlvmImports = Llvm.ImportsImpl(this)
    lateinit var llvmDeclarations: LlvmDeclarations
    lateinit var bitcodeFileName: String
    lateinit var library: KonanLibraryLayout

    private var llvmDisposed = false

    fun disposeLlvm() {
        if (llvmDisposed) return
        if (::debugInfo.isInitialized)
            LLVMDisposeDIBuilder(debugInfo.builder)
        if (llvmModule != null)
            LLVMDisposeModule(llvmModule)
        if (::llvm.isInitialized)
            LLVMDisposeModule(llvm.runtime.llvmModule)
        tryDisposeLLVMContext()
        llvmDisposed = true
    }

    val cStubsManager = CStubsManager(config.target)

    val coverage = CoverageManager(this)

    protected fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    fun verifyDescriptors() {
        // TODO: Nothing here for now.
    }

    fun printDescriptors() {
        if (!::moduleDescriptor.isInitialized)
            return

        separator("Descriptors:")
        moduleDescriptor.deepPrint()
    }

    fun printIr() {
        if (irModule == null) return
        separator("IR:")
        irModule!!.accept(DumpIrTreeVisitor(out), "")
    }

    fun printIrWithDescriptors() {
        if (irModule == null) return
        separator("IR:")
        irModule!!.accept(DumpIrTreeWithDescriptorsVisitor(out), "")
    }

    fun printLocations() {
        if (irModule == null) return
        separator("Locations:")
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
        separator("BitCode:")
        LLVMDumpModule(llvmModule!!)
    }

    fun verify() {
        verifyDescriptors()
        verifyBitCode()
    }

    fun print() {
        printDescriptors()
        printIr()
        printBitCode()
    }
    fun shouldVerifyBitCode() = config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)

    fun shouldPrintBitCode() = config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE)

    fun shouldPrintLocations() = config.configuration.getBoolean(KonanConfigKeys.PRINT_LOCATIONS)

    fun shouldProfilePhases() = config.phaseConfig.needProfiling

    fun shouldContainDebugInfo() = config.debug
    fun shouldContainLocationDebugInfo() = shouldContainDebugInfo() || config.lightDebug
    fun shouldContainAnyDebugInfo() = shouldContainDebugInfo() || shouldContainLocationDebugInfo()

    fun shouldOptimize() = config.configuration.getBoolean(KonanConfigKeys.OPTIMIZATION)
    fun ghaEnabled() = ::globalHierarchyAnalysisResult.isInitialized

    val memoryModel = config.memoryModel

    override var inVerbosePhase = false
    override fun log(message: () -> String) {
        if (inVerbosePhase) {
            println(message())
        }
    }

    lateinit var debugInfo: DebugInfo
    var moduleDFG: ModuleDFG? = null
    var externalModulesDFG: ExternalModulesDFG? = null
    lateinit var lifetimes: MutableMap<IrElement, Lifetime>
    lateinit var codegenVisitor: CodeGeneratorVisitor
    var devirtualizationAnalysisResult: Devirtualization.AnalysisResult? = null

    var referencedFunctions: Set<IrFunction>? = null

    val isNativeLibrary: Boolean by lazy {
        val kind = config.configuration.get(KonanConfigKeys.PRODUCE)
        kind == CompilerOutputKind.DYNAMIC || kind == CompilerOutputKind.STATIC
    }

    internal val stdlibModule
        get() = this.builtIns.any.module

    lateinit var compilerOutput: List<ObjectFile>

    val llvmModuleSpecification: LlvmModuleSpecification = LlvmModuleSpecificationImpl(
            config.cachedLibraries,
            producingCache = config.produce.isCache,
            librariesToCache = config.librariesToCache
    )

    val declaredLocalArrays: MutableMap<String, LLVMTypeRef> = HashMap()
}

private fun MemberScope.getContributedClassifier(name: String) =
        this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedFunctions(name: String) =
        this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

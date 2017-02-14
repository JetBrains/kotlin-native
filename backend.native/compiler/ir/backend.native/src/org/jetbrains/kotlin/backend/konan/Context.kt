package org.jetbrains.kotlin.backend.konan

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.descriptors.BridgeDirection
import org.jetbrains.kotlin.backend.konan.descriptors.bridgeDirection
import org.jetbrains.kotlin.backend.konan.descriptors.deepPrint
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.Ir
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.backend.konan.llvm.LlvmDeclarations
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import java.lang.System.out

internal class SpecialDescriptorsFactory(val context: Context) {
    private val outerThisDescriptors = mutableMapOf<ClassDescriptor, PropertyDescriptor>()
    private val bridgesDescriptors = mutableMapOf<FunctionDescriptor, FunctionDescriptor>()

    fun getOuterThisFieldDescriptor(innerClassDescriptor: ClassDescriptor): PropertyDescriptor =
        if (!innerClassDescriptor.isInner) throw AssertionError("Class is not inner: $innerClassDescriptor")
        else outerThisDescriptors.getOrPut(innerClassDescriptor) {
            val outerClassDescriptor = DescriptorUtils.getContainingClass(innerClassDescriptor) ?:
                    throw AssertionError("No containing class for inner class $innerClassDescriptor")

            val receiver = ReceiverParameterDescriptorImpl(innerClassDescriptor, ImplicitClassReceiver(innerClassDescriptor))
            PropertyDescriptorImpl.create(innerClassDescriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE,
                false, "this$0".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).initialize(outerClassDescriptor.defaultType, dispatchReceiverParameter = receiver)
        }

    fun getBridgeDescriptor(descriptor: FunctionDescriptor): FunctionDescriptor {
        val bridgeDirection = descriptor.bridgeDirection ?: throw AssertionError("Function $descriptor is not needed in a bridge")

        return bridgesDescriptors.getOrPut(descriptor) {
            SimpleFunctionDescriptorImpl.create(
                    descriptor.containingDeclaration,
                    Annotations.EMPTY,
                    ("<bridge-to>" + descriptor.name.asString()).synthesizedName,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    SourceElement.NO_SOURCE).apply {
                if (descriptor !is PropertySetterDescriptor)
                    initializeBridgeDescriptor(this, bridgeDirection, descriptor)
                else
                    initializeSetterBridgeDescriptor(this, bridgeDirection, descriptor)
            }
        }
    }

    private fun initializeBridgeDescriptor(bridgeDescriptor: SimpleFunctionDescriptorImpl,
                                           bridgeDirection: BridgeDirection, descriptor: FunctionDescriptor) {
        val toType = when (bridgeDirection) {
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
            BridgeDirection.TO_VALUE_TYPE -> descriptor.returnType!!
        }
        bridgeDescriptor.initialize(
                    descriptor.extensionReceiverParameter?.type,
                    descriptor.dispatchReceiverParameter,
                    descriptor.typeParameters,
                    descriptor.valueParameters,
                    toType,
                    Modality.FINAL,
                    Visibilities.PRIVATE)
    }

    private fun initializeSetterBridgeDescriptor(bridgeDescriptor: SimpleFunctionDescriptorImpl,
                                                 bridgeDirection: BridgeDirection, descriptor: PropertySetterDescriptor) {
        val valueIndex = descriptor.valueParameters.size - 1
        val toType = when (bridgeDirection) {
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
            BridgeDirection.TO_VALUE_TYPE -> descriptor.valueParameters[valueIndex].type
        }
        bridgeDescriptor.initialize(
                descriptor.extensionReceiverParameter?.type,
                descriptor.dispatchReceiverParameter,
                descriptor.typeParameters,
                descriptor.valueParameters.mapIndexed { index, valueParameterDescriptor ->
                    if (index != valueIndex)
                        valueParameterDescriptor
                    else {
                        ValueParameterDescriptorImpl(
                                valueParameterDescriptor.containingDeclaration,
                                null,
                                valueIndex,
                                Annotations.EMPTY,
                                valueParameterDescriptor.name,
                                toType,
                                valueParameterDescriptor.declaresDefaultValue(),
                                valueParameterDescriptor.isCrossinline,
                                valueParameterDescriptor.isNoinline,
                                valueParameterDescriptor.varargElementType,
                                SourceElement.NO_SOURCE)
                    }
                },
                descriptor.returnType,
                Modality.FINAL,
                Visibilities.PRIVATE)
    }
}

internal final class Context(val config: KonanConfig) : KonanBackendContext() {

    var moduleDescriptor: ModuleDescriptor? = null

    val specialDescriptorsFactory = SpecialDescriptorsFactory(this)

    // TODO: make lateinit?
    var irModule: IrModuleFragment? = null
        set(module: IrModuleFragment?) {
            if (field != null) {
                throw Error("Another IrModule in the context.")
            }
            field = module!!

            ir = Ir(this, module)
        }

    lateinit var ir: Ir

    override val irBuiltIns
        get() = ir.irModule.irBuiltins

    val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    var llvmModule: LLVMModuleRef? = null
        set(module: LLVMModuleRef?) {
            if (field != null) {
                throw Error("Another LLVMModule in the context.")
            }
            field = module!!

            llvm = Llvm(this, module)
        }

    lateinit var llvm: Llvm
    lateinit var llvmDeclarations: LlvmDeclarations

    var phase: KonanPhase? = null
    var depth: Int = 0

    protected fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    fun verifyDescriptors() {
        // TODO: Nothing here for now.
    }

    fun printDescriptors() {
        if (moduleDescriptor == null) return
        separator("Descriptors after: ${phase?.description}")
        moduleDescriptor!!.deepPrint()
    }

    fun verifyIr() {
        if (irModule == null) return
        // TODO: We don't have it yet.
    }

    fun printIr() {
        if (irModule == null) return
        separator("IR after: ${phase?.description}")
        irModule!!.accept(DumpIrTreeVisitor(out), "")
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

    fun shouldVerifyDescriptors(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_DESCRIPTORS) 
    }

    fun shouldVerifyIr(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_IR) 
    }

    fun shouldVerifyBitCode(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE) 
    }

    fun shouldPrintDescriptors(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_DESCRIPTORS) 
    }

    fun shouldPrintIr(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_IR) 
    }

    fun shouldPrintBitCode(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE) 
    }

    fun shouldProfilePhases(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.TIME_PHASES) 
    }

    fun log(message: String) {
        if (phase?.verbose ?: false) {
            println(message)
        }
    }
}


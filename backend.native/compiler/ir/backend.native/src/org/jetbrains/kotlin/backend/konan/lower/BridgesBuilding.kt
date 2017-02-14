package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.BridgeDirection
import org.jetbrains.kotlin.backend.konan.descriptors.bridgeDirection
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.descriptors.target
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class BridgesBuilding(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitClass(declaration: IrClass): IrStatement {
                val irClass = super.visitClass(declaration) as IrClass

                val functions = irClass.declarations.filterIsInstance<IrFunction>()
                val properties = irClass.declarations.filterIsInstance<IrProperty>()
                functions.forEach { buildBridge(it.descriptor, irClass) }
                properties.forEach {
                    buildBridge(it.descriptor.getter, irClass)
                    buildSetterBridge(it.descriptor.setter, irClass)
                }

                val contributedDescriptors = irClass.descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                // (includes declarations from supers)

                contributedDescriptors.forEach {
                    when (it) {
                        is FunctionDescriptor -> buildBridge(it, irClass)
                        is PropertyDescriptor -> {
                            buildBridge(it.getter, irClass)
                            buildSetterBridge(it.setter, irClass)
                        }
                    }
                }

                return irClass
            }
        })

    }

    object DECLARATION_ORIGIN_BRIDGE_METHOD :
            IrDeclarationOriginImpl("BRIDGE_METHOD")

    private fun buildBridge(descriptor: FunctionDescriptor?, irClass: IrClass) {
        if (descriptor == null || context.bridges[descriptor] != null) return

        if (descriptor is PropertySetterDescriptor) {
            buildSetterBridge(descriptor, irClass)
            return
        }

        val bridgeDirection = descriptor.bridgeDirection ?: return

        println("BUILD_BRIDGE: $descriptor")
        println("BUILD_BRIDGE: $bridgeDirection")

        val toType = when (bridgeDirection) {
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
            BridgeDirection.TO_VALUE_TYPE -> descriptor.returnType!!
        }
        val bridgeDescriptor = SimpleFunctionDescriptorImpl.create(
                irClass.descriptor,
                Annotations.EMPTY,
                ("<bridge-to>" + descriptor.name.asString()).synthesizedName,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE)
        bridgeDescriptor.initialize(
                descriptor.extensionReceiverParameter?.type,
                descriptor.dispatchReceiverParameter,
                descriptor.typeParameters,
                descriptor.valueParameters,
                toType,
                Modality.FINAL,
                Visibilities.PRIVATE)

        context.bridges[descriptor] = bridgeDescriptor
        val delegatingCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, descriptor.target,
                superQualifier = descriptor.target.containingDeclaration as ClassDescriptor /* Call non-virtually */).apply {
            val dispatchReceiverParameter = bridgeDescriptor.dispatchReceiverParameter
            if (dispatchReceiverParameter != null)
                dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter)
            val extensionReceiverParameter = bridgeDescriptor.extensionReceiverParameter
            if (extensionReceiverParameter != null)
                extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, extensionReceiverParameter)
            bridgeDescriptor.valueParameters.forEach {
                this.putValueArgument(it.index, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it))
            }
        }
        val bridgeBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                listOf(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, bridgeDescriptor, delegatingCall))
        )
        irClass.declarations.add(IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                DECLARATION_ORIGIN_BRIDGE_METHOD, bridgeDescriptor, bridgeBody))
    }

    private fun buildSetterBridge(descriptor: PropertySetterDescriptor?, irClass: IrClass) {
        if (descriptor == null || context.bridges[descriptor] != null) return
        val bridgeDirection = descriptor.bridgeDirection ?: return

        println("BUILD_BRIDGE: $descriptor")
        println("BUILD_BRIDGE: $bridgeDirection")

        val valueIndex = descriptor.valueParameters.size - 1
        val toType = when (bridgeDirection) {
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType // TODO
            BridgeDirection.TO_VALUE_TYPE -> descriptor.valueParameters[valueIndex].type
        }
        val bridgeDescriptor = SimpleFunctionDescriptorImpl.create(
                irClass.descriptor,
                Annotations.EMPTY,
                ("<bridge-to>" + descriptor.name.asString()).synthesizedName,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE)
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

        context.bridges[descriptor] = bridgeDescriptor
        val delegatingCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, descriptor.target,
                superQualifier = descriptor.target.containingDeclaration as ClassDescriptor /* Call non-virtually */).apply {
            val dispatchReceiverParameter = bridgeDescriptor.dispatchReceiverParameter
            if (dispatchReceiverParameter != null)
                dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter)
            val extensionReceiverParameter = bridgeDescriptor.extensionReceiverParameter
            if (extensionReceiverParameter != null)
                extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, extensionReceiverParameter)
            bridgeDescriptor.valueParameters.forEach {
                this.putValueArgument(it.index, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it))
            }
        }
        val bridgeBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(delegatingCall))
        irClass.declarations.add(IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                DECLARATION_ORIGIN_BRIDGE_METHOD, bridgeDescriptor, bridgeBody))
    }
}
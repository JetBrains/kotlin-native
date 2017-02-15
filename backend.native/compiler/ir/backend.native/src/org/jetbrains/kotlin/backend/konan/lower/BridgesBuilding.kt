package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.bridgeDirection
import org.jetbrains.kotlin.backend.konan.descriptors.contributedMethods
import org.jetbrains.kotlin.backend.konan.descriptors.target
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
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

                val functions = mutableSetOf<FunctionDescriptor?>()
                irClass.declarations.forEach {
                    when (it) {
                        is IrFunction -> functions.add(it.descriptor)
                        is IrProperty -> {
                            functions.add(it.getter?.descriptor)
                            functions.add(it.setter?.descriptor)
                        }
                    }
                }

                irClass.descriptor.contributedMethods.forEach { functions.add(it) }

                functions.forEach { buildBridge(it, irClass) }

                return irClass
            }
        })

    }

    private object DECLARATION_ORIGIN_BRIDGE_METHOD :
            IrDeclarationOriginImpl("BRIDGE_METHOD")

    private fun buildBridge(descriptor: FunctionDescriptor?, irClass: IrClass) {
        if (descriptor == null || descriptor.bridgeDirection == null) return

        if (descriptor is PropertySetterDescriptor) {
            buildSetterBridge(descriptor, irClass)
            return
        }

        val bridgeDescriptor = context.specialDescriptorsFactory.getBridgeDescriptor(descriptor)

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

    private fun buildSetterBridge(descriptor: PropertySetterDescriptor, irClass: IrClass) {
        val bridgeDescriptor = context.specialDescriptorsFactory.getBridgeDescriptor(descriptor)

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

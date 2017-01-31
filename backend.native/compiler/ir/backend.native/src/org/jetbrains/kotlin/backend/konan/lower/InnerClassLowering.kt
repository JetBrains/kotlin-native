package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

internal class InnerClassLowering(val context: Context) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        InnerClassTransformer(irClass).lowerInnerClass()
    }

    private inner class InnerClassTransformer(val irClass: IrClass) {
        val classDescriptor = irClass.descriptor

        lateinit var outerThisFieldDescriptor: PropertyDescriptor

        fun lowerInnerClass() {
            if (!irClass.descriptor.isInner) return

            createOuterThisField()
            lowerOuterThisReferences()
            lowerConstructors()
        }

        object DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS :
                IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")

        private fun createOuterThisField() {
            outerThisFieldDescriptor = context.specialDescriptorsFactory.getOuterThisFieldDescriptor(classDescriptor)

            irClass.declarations.add(IrFieldImpl(
                    irClass.startOffset, irClass.endOffset,
                    DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS,
                    outerThisFieldDescriptor
            ))
        }

        private fun lowerConstructors() {
            irClass.declarations.transformFlat { irMember ->
                if (irMember is IrConstructor)
                    lowerConstructor(irMember).singletonList()
                else
                    null
            }
        }

        private fun lowerConstructor(irConstructor: IrConstructor): IrConstructor {
            val descriptor = irConstructor.descriptor
            val startOffset = irConstructor.startOffset
            val endOffset = irConstructor.endOffset

            val dispatchReceiver = descriptor.dispatchReceiverParameter!!

            val blockBody = irConstructor.body as? IrBlockBody ?: throw AssertionError("Unexpected constructor body: ${irConstructor.body}")

            val instanceInitializerIndex = blockBody.statements.indexOfFirst { it is IrInstanceInitializerCall }
            if (instanceInitializerIndex >= 0) {
                // Initializing constructor: initialize 'this.this$0' with '$outer'
                blockBody.statements.add(
                        instanceInitializerIndex,
                        IrSetFieldImpl(
                                startOffset, endOffset, outerThisFieldDescriptor,
                                IrGetValueImpl(startOffset, endOffset, classDescriptor.thisAsReceiverParameter),
                                IrGetValueImpl(startOffset, endOffset, dispatchReceiver)
                        )
                )
            }
            else {
                // Delegating constructor: invoke old constructor with dispatch receiver '$outer'
                val delegatingConstructorCall = (blockBody.statements.find { it is IrDelegatingConstructorCall } ?:
                        throw AssertionError("Delegating constructor call expected: ${irConstructor.dump()}")
                        ) as IrDelegatingConstructorCall
                delegatingConstructorCall.dispatchReceiver = IrGetValueImpl(
                        delegatingConstructorCall.startOffset, delegatingConstructorCall.endOffset, dispatchReceiver
                )
            }

            return IrConstructorImpl(
                    startOffset, endOffset,
                    irConstructor.origin, // TODO special origin for lowered inner class constructors?
                    descriptor,
                    blockBody
            )
        }

        private fun lowerOuterThisReferences() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val implicitThisClass = expression.descriptor.getClassDescriptorForImplicitThis() ?:
                            return expression

                    if (implicitThisClass == classDescriptor) return expression

                    val startOffset = expression.startOffset
                    val endOffset = expression.endOffset
                    val origin = expression.origin

                    var irThis: IrExpression = IrGetValueImpl(startOffset, endOffset, classDescriptor.thisAsReceiverParameter, origin)
                    var innerClass = classDescriptor

                    while (innerClass != implicitThisClass) {
                        if (!innerClass.isInner) {
                            // Captured 'this' unrelated to inner classes nesting hierarchy, leave it as is -
                            // should be transformed by closures conversion.
                            return expression
                        }

                        val outerThisField = context.specialDescriptorsFactory.getOuterThisFieldDescriptor(innerClass)
                        irThis = IrGetFieldImpl(startOffset, endOffset, outerThisField, irThis, origin)

                        val outer = classDescriptor.containingDeclaration
                        innerClass = outer as? ClassDescriptor ?:
                                throw AssertionError("Unexpected containing declaration for inner class $innerClass: $outer")
                    }

                    return irThis
                }
            })
        }

        private fun ValueDescriptor.getClassDescriptorForImplicitThis(): ClassDescriptor? {
            if (this is ReceiverParameterDescriptor) {
                val receiverValue = value
                if (receiverValue is ImplicitClassReceiver) {
                    return receiverValue.classDescriptor
                }
            }
            return null
        }
    }
}


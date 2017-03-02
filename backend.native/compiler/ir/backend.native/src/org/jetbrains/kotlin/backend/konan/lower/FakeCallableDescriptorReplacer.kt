package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject

class FakeCallableDescriptorReplacer : BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(object: IrElementTransformerVoid(){
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val descriptor = expression.descriptor
                if (descriptor is FakeCallableDescriptorForObject) {
                    val referencedObject = descriptor.getReferencedObject()
                    return IrGetObjectValueImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            type        = referencedObject.defaultType,
                            descriptor  = referencedObject)
                }
                return super.visitGetValue(expression)
            }
        })

    }
}
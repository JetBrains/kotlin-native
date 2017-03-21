package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType

internal class LateinitLowering(val context: Context): FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitProperty(declaration: IrProperty): IrStatement {
                if (declaration.descriptor.isLateInit)
                    transformGetter(declaration.descriptor, declaration.getter!!)
                return declaration
            }

            private fun transformGetter(propertyDescriptor: PropertyDescriptor, getter: IrFunction) {
                val startOffset = getter.startOffset
                val endOffset = getter.endOffset
                val irBuilder = context.createIrBuilder(getter.descriptor, startOffset, endOffset)
                irBuilder.run {
                    val block = irBlock(propertyDescriptor.type)
                    val resultVar = scope.createTemporaryVariable(irGetField(irThis(), propertyDescriptor))
                    block.statements.add(resultVar)
                    val throwIfNull = irIfThenElse(context.builtIns.nothingType,
                            irNotEquals(irGet(resultVar.descriptor), irNull()),
                            irReturn(irGet(resultVar.descriptor)),
                            irCall(throwErrorFunction))
                    block.statements.add(throwIfNull)
                    getter.body = IrExpressionBodyImpl(startOffset, endOffset, block)
                }
            }
        })
    }

    private val throwErrorFunction = context.builtIns.getKonanInternalFunctions("ThrowUninitializedPropertyAccessException").single()

    private fun IrBuilderWithScope.irBlock(type: KotlinType): IrBlock
            = IrBlockImpl(startOffset, endOffset, type)

    private fun IrBuilderWithScope.irGetField(receiver: IrExpression?, property: PropertyDescriptor): IrExpression
            = IrGetFieldImpl(startOffset, endOffset, property, receiver)
}
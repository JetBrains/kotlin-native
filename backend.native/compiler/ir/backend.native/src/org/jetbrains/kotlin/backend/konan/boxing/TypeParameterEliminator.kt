package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.irCall

internal class TypeParameterEliminator(val context: Context, val function: IrFunction, val typeParameter: IrTypeParameter, val type: IrType): IrBuildingTransformer(context) {

    private val variables = mutableMapOf<VariableDescriptor, IrVariable>()

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return IrVariableImpl(
                declaration.startOffset,
                declaration.endOffset,
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrVariableSymbolImpl(declaration.descriptor),
                type
        ).apply {
            initializer = declaration.initializer?.transform(this@TypeParameterEliminator, null)
            val ini = initializer
            parent = when (ini) {
                is IrGetValue -> ini.symbol.owner.parent
                else -> declaration.parent
            }
            variables[declaration.descriptor] = this
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return context.createIrBuilder(expression.symbol).run {
            val irFunction = expression.symbol.owner
            irCall(irFunction).apply {
                this.dispatchReceiver = expression.dispatchReceiver?.transform(this@TypeParameterEliminator, null)
                this.extensionReceiver = expression.extensionReceiver?.transform(this@TypeParameterEliminator, null)
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i)!!.transform(this@TypeParameterEliminator, null))
                }
            }
        }
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        return context.createIrBuilder(function.symbol).run {
            irReturn(expression.value.transform(this@TypeParameterEliminator, null))
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val symbol = expression.symbol
        val descriptor = symbol.descriptor
        return context.createIrBuilder(symbol).run {
            when (descriptor) {
                is ValueParameterDescriptor -> irGet(function.getIrValueParameter(descriptor))
                is VariableDescriptor -> irGet(variables[descriptor] ?: return expression)
                else -> expression
            }
        }
    }
}
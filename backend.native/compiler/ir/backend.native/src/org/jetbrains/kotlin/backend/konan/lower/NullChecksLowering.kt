package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class NullChecksLowering(val context: Context) : FunctionLoweringPass {

    override fun lower(irFunction: IrFunction) {
        irFunction.transformChildrenVoid(RemoveRedundantNullChecksTransformer(context, irFunction))
    }
}

internal class RemoveRedundantNullChecksTransformer(val context: Context, val function: IrFunction) : IrElementTransformerVoid() {

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin == IrStatementOrigin.ELVIS) {
            assert(expression.statements.size == 2)
            val lhs = (expression.statements[0] as IrVariable).initializer as? IrBlock ?: return super.visitBlock(expression)
            if (lhs.origin == IrStatementOrigin.SAFE_CALL) {
                val rhs = expression.statements[1] as IrWhen
                return lowerElvisWithSafeCallLhs(lhs, rhs) ?: super.visitBlock(expression)
            }
        }
        return super.visitBlock(expression)
    }

    private fun lowerElvisWithSafeCallLhs(lhs: IrBlock, rhs: IrWhen): IrExpression? {
        assert(lhs.statements.size == 2)
        assert(rhs.branches.size == 2)
        val (receiver, call) = lhs.destructSafeCall()

        if (call !is IrCall) {
            return null
        }

        return with(context.createIrBuilder(function.symbol)) {
            irBlock {
                +receiver
                +irIfNull(
                        call.type,
                        irGet(receiver),
                        rhs.branches[0].result,
                        call
                )
            }
        }
    }

    // Transforms expr1?.expr2 to (expr1, expr1.expr2)
    private fun IrBlock.destructSafeCall(): Pair<IrVariable, IrExpression> {
        val receiver = statements[0] as IrVariable
        val nullCheck = statements[1] as IrWhen
        return receiver to nullCheck.branches[1].result
    }
}
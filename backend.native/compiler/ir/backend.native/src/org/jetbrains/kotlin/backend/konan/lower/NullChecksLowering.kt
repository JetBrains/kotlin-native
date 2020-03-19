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
import org.jetbrains.kotlin.ir.types.isMarkedNullable
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
            require(expression.statements.size == 2)
            return lowerIfPossible(expression)
        }
        return super.visitBlock(expression)
    }

    private fun lowerIfPossible(elvisBlock: IrBlock): IrExpression {
        val temporaryElvisVariableValue = (elvisBlock.statements[0] as IrVariable).initializer ?: return super.visitBlock(elvisBlock)
        val elvisNullCheck = elvisBlock.statements[1] as IrWhen
        // Assumed that first branch is equality to null, and the second one is else-branch
        require(elvisNullCheck.branches.size == 2)

        return lowerElvisWithSafeCallLhs(elvisNullCheck, temporaryElvisVariableValue)
                ?: lowerElvisWithNonNullableLhs(elvisNullCheck, temporaryElvisVariableValue)
                ?: super.visitBlock(elvisBlock)
    }

    private fun lowerElvisWithNonNullableLhs(elvisNullCheck: IrWhen, elvisVariableValue: IrExpression): IrExpression? {
        val isNull = elvisNullCheck.branches[0].condition
        require(isNull is IrCall && isNull.valueArgumentsCount == 2)
        return if (elvisVariableValue.type.isMarkedNullable()) null else elvisVariableValue
    }

    private fun lowerElvisWithSafeCallLhs(elvisNullCheck: IrWhen, elvisVariableValue: IrExpression): IrExpression? {
        val lhs = elvisVariableValue as? IrBlock
                ?: return null

        if (lhs.origin != IrStatementOrigin.SAFE_CALL) {
            return null
        }
        require(lhs.statements.size == 2)

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
                        elvisNullCheck.branches[0].result,
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
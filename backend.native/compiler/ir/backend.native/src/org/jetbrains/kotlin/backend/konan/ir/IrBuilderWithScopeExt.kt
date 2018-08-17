package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi2ir.intermediate.*

fun IrBuilderWithScope.irAnd(l: IrExpression, r: IrExpression): IrExpression {
    val boolType = this.irFalse().type
    return irIfThenElse(boolType, l, irIfThenElse(boolType, r, irTrue(), irFalse()), irFalse())
}

fun IrBuilderWithScope.irEquals(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.EQ, l, r)
fun IrBuilderWithScope.irNotEquals2(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.NE, l, r)

fun IrBuilderWithScope.irLessThan(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.LT, l, r)
fun IrBuilderWithScope.irGreaterThan(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.GT, l, r)
fun IrBuilderWithScope.irLessEqual(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.LE, l, r)
fun IrBuilderWithScope.irGreaterEqual(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.GE, l, r)

fun IrBuilderWithScope.irCompare(type: CompType, l: IrExpression, r: IrExpression): IrExpression {
    return when (type) {
        CompType.NE -> irNot(irCompare(CompType.EQ, l, r))
        CompType.EQ -> irCallWithArguments(context.irBuiltIns.eqeqSymbol, null, l, r)
        else -> {
            // Only implemented for
            // val primitiveTypesWithComparisons = listOf(int, long, float, double)
            val op = when (type) {
                CompType.LT -> context.irBuiltIns.lessFunByOperandType
                CompType.LE -> context.irBuiltIns.lessOrEqualFunByOperandType
                CompType.GT -> context.irBuiltIns.greaterFunByOperandType
                CompType.GE -> context.irBuiltIns.greaterOrEqualFunByOperandType
                else -> unreachable()
            }
            val func = op[l.type.toKotlinType()]
            val symbol = func?.symbol ?: error("Can't find symbol for func=$func, type=${l.type.toKotlinType()}")
            irCallWithArguments(symbol, null, l, r)
        }
    }
}

private fun unreachable(): Nothing = throw RuntimeException("Unreachable")

enum class CompType {
    EQ, NE, LT, LE, GT, GE;

    val inverted
        get() = when (this) {
            EQ -> NE
            NE -> EQ
            LT -> GE
            LE -> GT
            GT -> LE
            GE -> LT
        }
}

inline fun IrBuilderWithScope.irCallWithArguments(callee: IrFunctionSymbol, receiver: IrExpression?, vararg args: IrExpression): IrCall {
    return irCall(callee).apply {
        this.dispatchReceiver = receiver
        for ((index, arg) in args.withIndex()) putValueArgument(index, arg)
    }
}

internal fun IrBuilderWithScope.castToInt(context: Context, e: IrExpression): IrExpression {
    val exprType = e.type.toKotlinType()
    val func by lazy { context.ir.symbols.getFunction(Name.identifier("toInt"), exprType) }
    return when (exprType) {
        context.builtIns.intType -> e
        context.builtIns.byteType,
        context.builtIns.charType,
        context.builtIns.shortType,
        context.builtIns.longType,
        context.builtIns.floatType,
        context.builtIns.doubleType -> irCallWithArguments(func, e)
        else -> error("Unsupported type $exprType for .toInt")
    }
}

data class IrBlockBuilderWithScope(val builder: IrBuilderWithScope, val irBlock: IrBlockImpl)

fun IrBlockBuilderWithScope.createTemp(expr: IrExpression): IntermediateValue {
    return builder.scope.createTemporaryVariableInBlock(builder.context, expr, irBlock)
}

fun IrBuilderWithScope.createTempBlock(
    blockType: IrType,
    callback: IrBlockBuilderWithScope.() -> IrExpression
): IrBlock {
    val irBlock =
        IrBlockImpl(startOffset, endOffset, blockType, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL)
    irBlock.addIfNotNull(callback(IrBlockBuilderWithScope(this, irBlock)))
    return irBlock
}

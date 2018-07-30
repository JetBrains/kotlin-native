package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*

fun IrBuilderWithScope.irEquals(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.EQ, l, r)
fun IrBuilderWithScope.irNotEquals2(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.NE, l, r)

fun IrBuilderWithScope.irLessThan(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.LT, l, r)
fun IrBuilderWithScope.irGreaterThan(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.GT, l, r)
fun IrBuilderWithScope.irLessEqual(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.LE, l, r)
fun IrBuilderWithScope.irGreaterEqual(l: IrExpression, r: IrExpression): IrExpression = irCompare(CompType.GE, l, r)

fun IrBuilderWithScope.irCompare(type: CompType, l: IrExpression, r: IrExpression): IrExpression {
    return when (type) {
        CompType.NE -> irNot(irCompare(CompType.EQ, l, r))
        CompType.EQ -> irCallWithArguments(context.irBuiltIns.eqeqSymbol, l, r)
        else -> irCallWithArguments(
            when (type) {
                CompType.LT -> context.irBuiltIns.lessFunByOperandType
                CompType.LE -> context.irBuiltIns.lessOrEqualFunByOperandType
                CompType.GT -> context.irBuiltIns.greaterFunByOperandType
                CompType.GE -> context.irBuiltIns.greaterOrEqualFunByOperandType
                else -> unreachable()
            }[l.type.toKotlinType()]?.symbol!!,
            l, r
        )
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

inline fun IrBuilderWithScope.irCallWithArguments(callee: IrFunctionSymbol, vararg args: IrExpression): IrCall {
    return irCall(callee).apply {
        for ((index, arg) in args.withIndex()) putValueArgument(index, arg)
    }
}

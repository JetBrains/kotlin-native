package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.*

fun IrBuilderWithScope.irNotEquals2(left: IrExpression, right: IrExpression): IrExpression {
    return irNot(irEquals(left, right))
}

fun IrBuilderWithScope.irEquals(left: IrExpression, right: IrExpression): IrExpression {
    return irCallWithArguments(context.irBuiltIns.eqeqSymbol, left, right)
}

fun IrBuilderWithScope.irLessThan(left: IrExpression, right: IrExpression): IrExpression {
    return irCompare(context.irBuiltIns.lessFunByOperandType, left, right)
}

fun IrBuilderWithScope.irGreaterThan(left: IrExpression, right: IrExpression): IrExpression {
    return irCompare(context.irBuiltIns.greaterFunByOperandType, left, right)
}

fun IrBuilderWithScope.irLessEqual(left: IrExpression, right: IrExpression): IrExpression {
    return irCompare(context.irBuiltIns.lessOrEqualFunByOperandType, left, right)
}

fun IrBuilderWithScope.irGreaterEqual(left: IrExpression, right: IrExpression): IrExpression {
    return irCompare(context.irBuiltIns.greaterOrEqualFunByOperandType, left, right)
}


fun IrBuilderWithScope.irCompare(map: Map<SimpleType, IrSimpleFunction>, left: IrExpression, right: IrExpression): IrExpression {
    return irCallWithArguments(map[left.type.toKotlinType()]?.symbol!!, left, right)
}

fun IrBuilderWithScope.irCallWithArguments(callee: IrFunctionSymbol, vararg args: IrExpression): IrCall {
    return irCall(callee).apply {
        for ((index, arg) in args.withIndex()) putValueArgument(index, arg)
    }
}

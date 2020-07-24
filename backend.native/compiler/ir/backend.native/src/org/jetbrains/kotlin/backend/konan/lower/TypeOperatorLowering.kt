/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.containsNull
import org.jetbrains.kotlin.backend.konan.ir.isObjCObjectType
import org.jetbrains.kotlin.backend.konan.ir.isSubtypeOf
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This lowering pass lowers some [IrTypeOperatorCall]s.
 */
internal class TypeOperatorLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = TypeOperatorTransformer(context)
        irFile.transform(transformer, data = null)
    }
}

private class TypeOperatorTransformer(val context: Context) : IrBuildingTransformer(context) {
    private val symbols = context.ir.symbols

    private fun IrType.erasure(): IrType {
        if (this !is IrSimpleType) return this

        return when (val classifier = this.classifier) {
            is IrClassSymbol -> this
            is IrTypeParameterSymbol -> {
                val upperBound = classifier.owner.superTypes.firstOrNull() ?:
                        TODO("${classifier.descriptor} : ${classifier.descriptor.upperBounds}")

                if (this.hasQuestionMark) {
                    // `T?`
                    upperBound.erasure().makeNullable()
                } else {
                    upperBound.erasure()
                }
            }
            else -> TODO(classifier.toString())
        }
    }

    fun IrBuilderWithScope.irCast(argument: IrExpression, type: IrType, argumentIsNotNull: Boolean = false) = irBlock {
        val value = irTemporary(argument)
        val error = if (type.isObjCObjectType())
            irCall(symbols.ThrowTypeCastException)
        else {
            irCall(symbols.throwClassCastException).apply {
                putValueArgument(0, irGet(value))
                putValueArgument(1, irCall(symbols.getClassTypeInfo, listOf(type)))
            }
        }
        +irIfThenElse(
                type,
                condition = irInstanceOf(value, type, argumentIsNotNull),
                thenPart = irImplicitCast(irGet(value), type),
                elsePart = error
        )
    }

    private fun IrBuilderWithScope.irInstanceOf(
            value: IrValueDeclaration,
            operand: IrType,
            valueIsNotNull: Boolean = false
    ): IrExpression {
        val instanceOf = if (operand.isObjCObjectType()) symbols.interopObjCInstanceOf else symbols.instanceOf
        val instanceCheck = irCall(instanceOf, listOf(operand)).apply {
            putValueArgument(0, irGet(value))
        }
        if (valueIsNotNull)
            return instanceCheck
        val checkValueIsNull = irEqeqeq(irGet(value), irNull())
        return if (operand.containsNull())
            context.oror(startOffset, endOffset, checkValueIsNull, instanceCheck)
        else
            context.andand(startOffset, endOffset, irNot(checkValueIsNull), instanceCheck)
    }

    private fun lowerCast(expression: IrTypeOperatorCall): IrExpression {
        builder.at(expression)
        val typeOperand = expression.typeOperand.erasure()

//        assert (!TypeUtils.hasNullableSuperType(typeOperand)) // So that `isNullable()` <=> `isMarkedNullable`.

        // TODO: consider the case when expression type is wrong e.g. due to generics-related unchecked casts.

        return when {
            expression.argument.type.isSubtypeOf(typeOperand) -> expression.argument

            expression.argument.type.containsNull() -> {
                with (builder) {
                    irLetS(expression.argument) { argument ->
                        irIfThenElse(
                                type = expression.type,
                                condition = irEqeqeq(irGet(argument.owner), irNull()),

                                thenPart = if (typeOperand.isSimpleTypeWithQuestionMark)
                                    irNull()
                                else
                                    irCall(symbols.ThrowNullPointerException.owner),

                                elsePart = irCast(irGet(argument.owner), typeOperand.makeNotNull(),
                                        argumentIsNotNull = true)
                        )
                    }
                }
            }

            typeOperand.isSimpleTypeWithQuestionMark -> builder.irCast(expression.argument, typeOperand.makeNotNull())

            else -> builder.irCast(expression.argument, typeOperand)
        }
    }

    private fun lowerSafeCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = expression.typeOperand.erasure()

        return builder.irBlock(expression) {
            +irLetS(expression.argument) { variable ->
                irIfThenElse(expression.type,
                        condition = irInstanceOf(variable.owner, typeOperand),
                        thenPart = irImplicitCast(irGet(variable.owner), typeOperand),
                        elsePart = irNull())
            }
        }
    }

    private fun lowerInstanceOf(expression: IrTypeOperatorCall) = builder.at(expression).irBlock {
        val value = irTemporary(expression.argument)
        +irInstanceOf(value, expression.typeOperand.erasure())
    }

    private fun lowerNotInstanceOf(expression: IrTypeOperatorCall) = builder.at(expression).irBlock {
        val value = irTemporary(expression.argument)
        +irNot(irInstanceOf(value, expression.typeOperand.erasure()))
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.operator) {
            IrTypeOperator.SAFE_CAST -> lowerSafeCast(expression)
            IrTypeOperator.CAST -> lowerCast(expression)
            IrTypeOperator.INSTANCEOF -> lowerInstanceOf(expression)
            IrTypeOperator.NOT_INSTANCEOF -> lowerNotInstanceOf(expression)
            else -> expression
        }
    }
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.backend.konan.irasdescriptors.containsNull
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isSubtypeOf
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

/**
 * This lowering pass lowers some [IrTypeOperatorCall]s.
 */
internal class TypeOperatorLowering(val context: KonanBackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        val transformer = TypeOperatorTransformer(context, irFunction.symbol)
        irFunction.transformChildrenVoid(transformer)
    }
}

private fun constructCallToThrowTypeCastException(context: KonanBackendContext,
                                                   builder: DeclarationIrBuilder,
                                                   argument: IrValueDeclaration,
                                                   typeOperand: IrType): IrCall {
    val symbols = context.ir.symbols
    val throwTypeCastException = symbols.ThrowTypeCastException

    return with(builder) {
        val constructTypeOperandClass = irCall(symbols.kClassImplConstructor, listOf(typeOperand)).apply {
            putValueArgument(0, irCall(symbols.getClassTypeInfo, listOf(typeOperand)))
        }
        irCall(throwTypeCastException.owner).apply {
            putValueArgument(0, builder.irGet(argument))
            putValueArgument(1, constructTypeOperandClass)
        }
    }
}

private class TypeOperatorTransformer(val context: KonanBackendContext, val function: IrFunctionSymbol) : IrElementTransformerVoid() {

    private val builder = context.createIrBuilder(function)

    override fun visitFunction(declaration: IrFunction): IrStatement {
        // ignore inner functions during this pass
        return declaration
    }

    private fun IrType.erasure(): IrType {
        if (this !is IrSimpleType) return this

        val classifier = this.classifier
        return when (classifier) {
            is IrClassSymbol -> this
            is IrTypeParameterSymbol -> {
                val upperBound = classifier.owner.superTypes.singleOrNull() ?:
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
                                    constructCallToThrowTypeCastException(this@TypeOperatorTransformer.context, builder, argument.owner, typeOperand),

                                elsePart = irAs(irGet(argument.owner), typeOperand.makeNotNull())
                        )
                    }
                }
            }

            typeOperand.isSimpleTypeWithQuestionMark -> builder.irAs(expression.argument, typeOperand.makeNotNull())

            typeOperand == expression.typeOperand -> expression

            else -> builder.irAs(expression.argument, typeOperand)
        }
    }

    private fun KotlinType.isNullable() = TypeUtils.isNullableType(this)

    private fun lowerSafeCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = expression.typeOperand.erasure()

        return builder.irBlock(expression) {
            +irLetS(expression.argument) { variable ->
                irIfThenElse(expression.type,
                        condition = irIs(irGet(variable.owner), typeOperand),
                        thenPart = irImplicitCast(irGet(variable.owner), typeOperand),
                        elsePart = irNull())
            }
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.operator) {
            IrTypeOperator.SAFE_CAST -> lowerSafeCast(expression)
            IrTypeOperator.CAST -> lowerCast(expression)
            else -> expression
        }
    }
}
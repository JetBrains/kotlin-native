/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.containsNull
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isSubtypeOf
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.defaultOrNullableType
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This lowering pass lowers some calls to [IrBuiltinOperatorDescriptor]s.
 */
internal class BuiltinOperatorLowering(val context: Context) : FileLoweringPass, IrBuildingTransformer(context) {

    private val builtIns = context.builtIns
    private val irBuiltins = context.irModule!!.irBuiltins
    private val symbols = context.ir.symbols

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        val descriptor = expression.descriptor

        if (descriptor is IrBuiltinOperatorDescriptor) {
            return transformBuiltinOperator(expression)
        }

        return expression
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression.argument.type.isNothing()) {
            return expression.argument
        }
        return expression
    }

    private fun ieee754EqualsDescriptors(): List<FunctionDescriptor> =
            irBuiltins.ieee754equalsFunByOperandType.values.map(IrSimpleFunction::descriptor)

    private fun transformBuiltinOperator(expression: IrCall): IrExpression = when (expression.descriptor) {
        irBuiltins.eqeq, in ieee754EqualsDescriptors() -> lowerEqeq(expression)

        irBuiltins.eqeqeq -> lowerEqeqeq(expression)

        irBuiltins.throwNpe -> IrCallImpl(expression.startOffset, expression.endOffset,
                context.ir.symbols.ThrowNullPointerException.owner.returnType,
                context.ir.symbols.ThrowNullPointerException)

        irBuiltins.noWhenBranchMatchedException -> IrCallImpl(expression.startOffset, expression.endOffset,
                context.ir.symbols.ThrowNoWhenBranchMatchedException.owner.returnType,
                context.ir.symbols.ThrowNoWhenBranchMatchedException)

        else -> expression
    }

    private fun lowerEqeqeq(expression: IrCall): IrExpression {
        val lhs = expression.getValueArgument(0)!!
        val rhs = expression.getValueArgument(1)!!

        return if (lhs.type.isInlined() && rhs.type.isInlined()) {
            // Achieve the same behavior as with JVM BE: if both sides of `===` are values, then compare by value:
            lowerEqeq(expression)
            // Note: such comparisons are deprecated.
        } else {
            expression
        }
    }

    private fun IrBuilderWithScope.reinterpret(expression: IrExpression, toType: IrType) =
            reinterpret(expression, expression.type, toType)

    private fun IrBuilderWithScope.reinterpret(expression: IrExpression, fromType: IrType, toType: IrType) =
            irCall(symbols.reinterpret.owner, listOf(fromType, toType)).apply {
                extensionReceiver = expression
            }

    private fun lowerEqeq(expression: IrCall): IrExpression {
        // TODO: optimize boxing?

        builder.at(expression).run {
            val lhs = expression.getValueArgument(0)!!
            val rhs = expression.getValueArgument(1)!!

            if (rhs.isNullConst()) {
                return irEqeqNull(lhs)
            }

            if (lhs.isNullConst()) {
                return irEqeqNull(rhs)
            }

            if (expression.symbol == irBuiltins.eqeqSymbol) {
                lhs.type.getInlinedClass()?.let {
                    if (it == rhs.type.getInlinedClass()) {
                        return genInlineClassEquals(expression.descriptor, rhs, lhs)
                    }
                }
            }

            return genFloatingOrReferenceEquals(expression.descriptor, lhs, rhs)
        }
    }

    fun IrBuilderWithScope.genInlineClassEquals(
            descriptor: FunctionDescriptor,
            rhs: IrExpression,
            lhs: IrExpression
    ): IrExpression {
        val lhsBinaryType = lhs.type.computeBinaryType()
        return when (lhsBinaryType) {
            is BinaryType.Primitive -> {
                val areEqualByValue = symbols.areEqualByValue[lhsBinaryType.type]!!.owner
                irCall(areEqualByValue).apply {
                    putValueArgument(0, reinterpret(lhs, areEqualByValue.valueParameters[0].type))
                    putValueArgument(1, reinterpret(rhs, areEqualByValue.valueParameters[1].type))
                }
            }

            is BinaryType.Reference -> {
                // TODO: don't use binaryType.nullable.
                val lhsRawType = irBuiltins.anyClass.owner.defaultOrNullableType(lhsBinaryType.nullable)
                val rhsBinaryType = rhs.type.computeBinaryType() as BinaryType.Reference<*>
                val rhsRawType = irBuiltins.anyClass.owner.defaultOrNullableType(rhsBinaryType.nullable)

                genFloatingOrReferenceEquals(
                        descriptor,
                        reinterpret(lhs, lhsRawType),
                        reinterpret(rhs, rhsRawType)
                )
            }
        }
    }

    private fun IrBuilderWithScope.irEqeqNull(expression: IrExpression): IrExpression {
        val type = expression.type.makeNullable()
        val primitiveBinaryTypeOrNull = type.computePrimitiveBinaryTypeOrNull()
        return when (primitiveBinaryTypeOrNull) {
            null -> irEqeqeq(reinterpret(expression, type, irBuiltins.anyNType), irNull())
            PrimitiveBinaryType.POINTER -> irCall(symbols.areEqualByValue[PrimitiveBinaryType.POINTER]!!.owner).apply {
                putValueArgument(0, reinterpret(expression, type, symbols.nativePtrType))
                putValueArgument(1, reinterpret(irNull(), type, symbols.nativePtrType))
            }
            else -> error("Nullable type ${type.toKotlinType()} is $primitiveBinaryTypeOrNull")
        }
    }

    private fun IrBuilderWithScope.irLogicalAnd(lhs: IrExpression, rhs: IrExpression) = context.andand(lhs, rhs)
    private fun IrBuilderWithScope.irIsNull(exp: IrExpression) = irEqeqeq(exp, irNull())
    private fun IrBuilderWithScope.irIsNotNull(exp: IrExpression) = irNot(irEqeqeq(exp, irNull()))

    private fun IrBuilderWithScope.genFloatingOrReferenceEquals(descriptor: FunctionDescriptor, lhs: IrExpression, rhs: IrExpression): IrExpression {
        // TODO: areEqualByValue and ieee754Equals intrinsics are specially treated by code generator
        // and thus can be declared synthetically in the compiler instead of explicitly in the runtime.
        fun callEquals(lhs: IrExpression, rhs: IrExpression) =
                if (descriptor in ieee754EqualsDescriptors())
                // Find a type-compatible `konan.internal.ieee754Equals` intrinsic:
                    irCall(selectIntrinsic(symbols.ieee754Equals, lhs.type, rhs.type, true)!!).apply {
                        putValueArgument(0, lhs)
                        putValueArgument(1, rhs)
                    }
                else
                    irCall(symbols.equals).apply {
                        dispatchReceiver = lhs
                        putValueArgument(0, rhs)
                    }

        val lhsIsNotNullable = !lhs.type.containsNull()
        val rhsIsNotNullable = !rhs.type.containsNull()

        return if (descriptor in ieee754EqualsDescriptors()) {
            if (lhsIsNotNullable && rhsIsNotNullable)
                callEquals(lhs, rhs)
            else irBlock {
                val lhsTemp = irTemporary(lhs)
                val rhsTemp = irTemporary(rhs)
                if (lhsIsNotNullable xor rhsIsNotNullable) { // Exactly one nullable.
                    +irLogicalAnd(
                            irIsNotNull(irGet(if (lhsIsNotNullable) rhsTemp else lhsTemp)),
                            callEquals(irGet(lhsTemp), irGet(rhsTemp))
                    )
                } else { // Both are nullable.
                    +irIfThenElse(context.irBuiltIns.booleanType, irIsNull(irGet(lhsTemp)),
                            irIsNull(irGet(rhsTemp)),
                            irLogicalAnd(
                                    irIsNotNull(irGet(rhsTemp)),
                                    callEquals(irGet(lhsTemp), irGet(rhsTemp))
                            )
                    )
                }
            }
        } else {
            if (lhsIsNotNullable)
                callEquals(lhs, rhs)
            else {
                irBlock {
                    val lhsTemp = irTemporary(lhs)
                    if (rhsIsNotNullable)
                        +irLogicalAnd(irIsNotNull(irGet(lhsTemp)), callEquals(irGet(lhsTemp), rhs))
                    else {
                        val rhsTemp = irTemporary(rhs)
                        +irIfThenElse(irBuiltins.booleanType, irIsNull(irGet(lhsTemp)),
                                irIsNull(irGet(rhsTemp)),
                                callEquals(irGet(lhsTemp), irGet(rhsTemp))
                        )
                    }
                }
            }
        }
    }

    private fun selectIntrinsic(from: List<IrSimpleFunctionSymbol>, lhsType: IrType, rhsType: IrType, allowNullable: Boolean) =
            from.atMostOne {
                val leftParamType = it.owner.valueParameters[0].type
                val rightParamType = it.owner.valueParameters[1].type
                (lhsType.isSubtypeOf(leftParamType) || (allowNullable && lhsType.isSubtypeOf(leftParamType.makeNullable())))
                        && (rhsType.isSubtypeOf(rightParamType) || (allowNullable && rhsType.isSubtypeOf(rightParamType.makeNullable())))
            }
}

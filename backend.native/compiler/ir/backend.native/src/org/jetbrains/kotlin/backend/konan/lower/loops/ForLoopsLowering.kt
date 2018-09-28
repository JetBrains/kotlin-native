/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower.loops

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.util.OperatorNameConventions

/**  This lowering pass optimizes range-based for loops. */
internal class ForLoopsLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = ForLoopsTransformer(context)
        // Lower loops
        irFile.transformChildrenVoid(transformer)

        // Update references in break/continue.
        irFile.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
                transformer.oldLoopToNewLoop[jump.loop]?.let { jump.loop = it }
                return jump
            }
        })
    }
}

private class ForLoopsTransformer(val context: Context) : IrElementTransformerVoidWithContext() {

    private val symbols = context.ir.symbols
    private val iteratorToLoopInfo = mutableMapOf<IrVariableSymbol, ForLoopInfo>()
    internal val oldLoopToNewLoop = mutableMapOf<IrLoop, IrLoop>()

    private val iteratorType = symbols.iterator.descriptor.defaultType.replaceArgumentsWithStarProjections()

    private val scopeOwnerSymbol
        get() = currentScope!!.scope.scopeOwnerSymbol

    //region Util methods ==============================================================================================
    private fun IrExpression.castIfNecessary(progressionType: ProgressionType): IrExpression {
        val type = this.type.toKotlinType()
        // TODO: Fix asserts
//        assert(type in progressionElementClassesTypes || type in progressionElementClassesNullableTypes)
        return if (type == progressionType.elementType) {
            this
        } else {
            val function = symbols.getFunction(progressionType.numberCastFunctionName, type)
            IrCallImpl(startOffset, endOffset, function.owner.returnType, function)
                    .apply { dispatchReceiver = this@castIfNecessary }
        }
    }

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression): IrExpression {
        return if (expression.type.isSimpleTypeWithQuestionMark) {
            irImplicitCast(expression, expression.type.makeNotNull())
        } else {
            expression
        }
    }

    private fun IrExpression.unaryMinus(): IrExpression {
        val unaryOperator = symbols.getUnaryOperator(OperatorNameConventions.UNARY_MINUS, type.toKotlinType())
        return IrCallImpl(startOffset, endOffset, unaryOperator.owner.returnType, unaryOperator).apply {
            dispatchReceiver = this@unaryMinus
        }
    }

    private fun ProgressionInfo.defaultStep(startOffset: Int, endOffset: Int): IrExpression =
        progressionType.elementType.let { type ->
            val step = if (increasing) 1 else -1
            when {
                KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isChar(type) ->
                    IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, step)
                KotlinBuiltIns.isLong(type) ->
                    IrConstImpl.long(startOffset, endOffset, context.irBuiltIns.longType, step.toLong())
                else -> throw IllegalArgumentException()
            }
        }

    private fun irGetProgressionLast(progressionType: ProgressionType,
                                     first: IrVariable,
                                     lastExpression: IrExpression,
                                     step: IrVariable): IrExpression {
        val symbol = symbols.getProgressionLast[progressionType.elementType]
                ?: throw IllegalArgumentException("Unknown progression element type: ${lastExpression.type}")
        val startOffset = lastExpression.startOffset
        val endOffset = lastExpression.endOffset
        return IrCallImpl(startOffset, lastExpression.endOffset, symbol.owner.returnType, symbol).apply {
            putValueArgument(0, IrGetValueImpl(startOffset, endOffset, first.type, first.symbol))
            putValueArgument(1, lastExpression.castIfNecessary(progressionType))
            putValueArgument(2, IrGetValueImpl(startOffset, endOffset, step.type, step.symbol))
        }
    }
    //endregion

    //region Lowering ==================================================================================================
    // Lower a loop header.
    private fun processHeader(variable: IrVariable, initializer: IrCall): IrStatement? {
        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        val symbol = variable.symbol
        if (!variable.descriptor.type.isSubtypeOf(iteratorType)) {
            return null
        }
        assert(symbol !in iteratorToLoopInfo)

        val builder = context.createIrBuilder(scopeOwnerSymbol, variable.startOffset, variable.endOffset)
        // Collect loop info and form the loop header composite.
        val progressionInfo = initializer.dispatchReceiver?.accept(ProgressionInfoBuilder(context), null) ?: return null

        with(builder) {
            with(progressionInfo) {
                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast them to non-nullable.
                val statements = mutableListOf<IrStatement>()

                /**
                 * For this loop:
                 * `for (i in a() .. b() step c() step d())`
                 * We need to call functions in the following order: a, b, c, d.
                 * So we call b() before step calculations and then call last element calculation function (if required).
                 */
                val inductionVariable = scope.createTemporaryVariable(first.castIfNecessary(progressionType),
                        nameHint = "inductionVariable",
                        isMutable = true,
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE).also {
                    statements.add(it)
                }

                val boundValue = scope.createTemporaryVariable(bound.castIfNecessary(progressionType),
                        nameHint = "bound",
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)
                        .also { statements.add(it) }


                val stepExpression = (if (increasing) step else step?.unaryMinus()) ?: defaultStep(startOffset, endOffset)
                val stepValue = scope.createTemporaryVariable(ensureNotNullable(stepExpression),
                        nameHint = "step",
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE).also {
                    statements.add(it)
                }

                // Calculate the last element of the progression
                // The last element can be:
                //    boundValue, if step is 1 and the range is closed.
                //    boundValue - 1, if step is 1 and the range is open.
                //    getProgressionLast(inductionVariable, boundValue, step), if step != 1 and the range is closed.
                //    getProgressionLast(inductionVariable, boundValue - 1, step), if step != 1 and the range is open.
                var lastExpression: IrExpression? = null
                if (!closed) {
                    val decrementSymbol = symbols.getUnaryOperator(OperatorNameConventions.DEC, boundValue.descriptor.type)
                    lastExpression = irCall(decrementSymbol.owner).apply {
                        dispatchReceiver = irGet(boundValue)
                    }
                }
                if (needLastCalculation) {
                    lastExpression = irGetProgressionLast(progressionType,
                            inductionVariable,
                            lastExpression ?: irGet(boundValue),
                            stepValue)
                }
                val lastValue = if (lastExpression != null) {
                    scope.createTemporaryVariable(lastExpression,
                            nameHint = "last",
                            origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE).also {
                        statements.add(it)
                    }
                } else {
                    boundValue
                }

                iteratorToLoopInfo[symbol] = ForLoopInfo(progressionInfo,
                        inductionVariable,
                        boundValue,
                        lastValue,
                        stepValue)

                return IrCompositeImpl(startOffset, endOffset, context.irBuiltIns.unitType, null, statements)
            }
        }
    }

    // Lower getting a next induction variable value.
    private fun processNext(variable: IrVariable, initializer: IrCall): IrExpression? {
        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE
                || variable.origin == IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)
        val irIteratorAccess = initializer.dispatchReceiver as? IrGetValue ?: throw AssertionError()
        val forLoopInfo = iteratorToLoopInfo[irIteratorAccess.symbol] ?: return null  // If we didn't lower a corresponding header.
        val builder = context.createIrBuilder(scopeOwnerSymbol, initializer.startOffset, initializer.endOffset)

        val plusOperator = symbols.getBinaryOperator(
                OperatorNameConventions.PLUS,
                forLoopInfo.inductionVariable.descriptor.type,
                forLoopInfo.step.descriptor.type
        )
        forLoopInfo.loopVariable = variable

        with(builder) {
            variable.initializer = irGet(forLoopInfo.inductionVariable)
            val increment = irSetVar(forLoopInfo.inductionVariable,
                    irCallOp(plusOperator.owner, irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.step)))
            return IrCompositeImpl(variable.startOffset,
                    variable.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.FOR_LOOP_NEXT,
                    listOf(variable, increment))
        }
    }

    private fun DeclarationIrBuilder.buildMinValueCondition(forLoopInfo: ForLoopInfo): IrExpression {
        // Condition for a corner case: for (i in a until Int.MIN_VALUE) {}.
        // Check if forLoopInfo.bound > MIN_VALUE.
        val progressionType = forLoopInfo.progressionInfo.progressionType
        return irCall(context.irBuiltIns.greaterFunByOperandType[context.irBuiltIns.int]?.symbol!!).apply {
            val minConst = when {
                progressionType.isIntProgression() -> IrConstImpl
                        .int(startOffset, endOffset, context.irBuiltIns.intType, Int.MIN_VALUE)
                progressionType.isCharProgression() -> IrConstImpl
                        .char(startOffset, endOffset, context.irBuiltIns.charType, 0.toChar())
                progressionType.isLongProgression() -> IrConstImpl
                        .long(startOffset, endOffset, context.irBuiltIns.longType, Long.MIN_VALUE)
                else -> throw IllegalArgumentException("Unknown progression type")
            }
            val compareToCall = irCall(symbols.getBinaryOperator(OperatorNameConventions.COMPARE_TO,
                    forLoopInfo.bound.descriptor.type,
                    minConst.type.toKotlinType())).apply {
                dispatchReceiver = irGet(forLoopInfo.bound)
                putValueArgument(0, minConst)
            }
            putValueArgument(0, compareToCall)
            putValueArgument(1, irInt(0))
        }
    }

    // TODO: Eliminate the loop if we can prove that it will not be executed.
    private fun DeclarationIrBuilder.buildEmptyCheck(loop: IrLoop, forLoopInfo: ForLoopInfo): IrExpression {
        val builtIns = context.irBuiltIns
        val increasing = forLoopInfo.progressionInfo.increasing
        val comparingBuiltIn = if (increasing) builtIns.lessOrEqualFunByOperandType[builtIns.int]?.symbol
        else builtIns.greaterOrEqualFunByOperandType[builtIns.int]?.symbol

        // Check if inductionVariable <= last.
        val compareTo = symbols.getBinaryOperator(OperatorNameConventions.COMPARE_TO,
                forLoopInfo.inductionVariable.descriptor.type,
                forLoopInfo.last.descriptor.type)

        val check: IrExpression = irCall(comparingBuiltIn!!).apply {
            putValueArgument(0, irCallOp(compareTo.owner, irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.last)))
            putValueArgument(1, irInt(0))
        }

        // Process closed and open ranges in different manners.
        return if (forLoopInfo.progressionInfo.closed) {
            irIfThen(check, loop)   // if (inductionVariable <= last) { loop }
        } else {
            // Take into account a corner case: for (i in a until Int.MIN_VALUE) {}.
            // if (inductionVariable <= last && bound > MIN_VALUE) { loop }
            return irIfThen(check, irIfThen(buildMinValueCondition(forLoopInfo), loop))
        }
    }

    private fun DeclarationIrBuilder.buildNewCondition(oldCondition: IrExpression): Pair<IrExpression, ForLoopInfo>? {
        if (oldCondition !is IrCall || oldCondition.origin != IrStatementOrigin.FOR_LOOP_HAS_NEXT) {
            return null
        }

        val irIteratorAccess = oldCondition.dispatchReceiver as? IrGetValue ?: throw AssertionError()
        // Return null if we didn't lower a corresponding header.
        val forLoopInfo = iteratorToLoopInfo[irIteratorAccess.symbol] ?: return null
        assert(forLoopInfo.loopVariable != null)

        return irCall(context.irBuiltIns.booleanNotSymbol).apply {
            val eqeqCall = irCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irGet(forLoopInfo.loopVariable!!))
                putValueArgument(1, irGet(forLoopInfo.last))
            }
            putValueArgument(0, eqeqCall)
        } to forLoopInfo
    }

    /**
     * This loop
     *
     * for (i in first..last step foo) { ... }
     *
     * is represented in IR in such a manner:
     *
     * val it = (first..last step foo).iterator()
     * while (it.hasNext()) {
     *     val i = it.next()
     *     ...
     * }
     *
     * We transform it into the following loop:
     *
     * var it = first
     * if (it <= last) {  // (it >= last if the progression is decreasing)
     *     do {
     *         val i = it++
     *         ...
     *     } while (i != last)
     * }
     */
    // TODO:  Lower `for (i in a until b)` to loop with precondition: for (i = a; i < b; a++);
    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (loop.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
            return super.visitWhileLoop(loop)
        }

        with(context.createIrBuilder(scopeOwnerSymbol, loop.startOffset, loop.endOffset)) {
            // Transform accesses to the old iterator (see visitVariable method). Store loopVariable in loopInfo.
            // Replace not transparent containers with transparent ones (IrComposite)
            val newBody = loop.body?.transform(this@ForLoopsTransformer, null)?.let {
                if (it is IrContainerExpression && !it.isTransparentScope) {
                    with(it) { IrCompositeImpl(startOffset, endOffset, type, origin, statements) }
                } else {
                    it
                }
            }
            val (newCondition, forLoopInfo) = buildNewCondition(loop.condition) ?: return super.visitWhileLoop(loop)

            val newLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
                label = loop.label
                condition = newCondition
                body = newBody
            }
            oldLoopToNewLoop[loop] = newLoop
            // Build a check for an empty progression before the loop.
            return buildEmptyCheck(newLoop, forLoopInfo)
        }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        val initializer = declaration.initializer
        if (initializer == null || initializer !is IrCall) {
            return super.visitVariable(declaration)
        }
        val result = when (initializer.origin) {
            IrStatementOrigin.FOR_LOOP_ITERATOR -> processHeader(declaration, initializer)
            IrStatementOrigin.FOR_LOOP_NEXT -> processNext(declaration, initializer)
            else -> null
        }
        return result ?: super.visitVariable(declaration)
    }
    //endregion
}


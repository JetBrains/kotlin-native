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
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.asSimpleType
import org.jetbrains.kotlin.types.typeUtil.*
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

    private val progressionElementClasses: List<IrClassSymbol> = mutableListOf(symbols.char).apply {
        addAll(symbols.integerClasses)
    }

    private val progressionElementClassesTypes: List<SimpleType> = mutableListOf<SimpleType>().apply {
        progressionElementClasses.mapTo(this) { it.descriptor.defaultType }
    }

    private val progressionElementClassesNullableTypes: List<SimpleType> = mutableListOf<SimpleType>().apply {
        progressionElementClassesTypes.mapTo(this) { it.makeNullableAsSpecified(true) }
    }

    //region Symbols for progression building functions ================================================================
    private fun getProgressionBuildingMethods(name: String): Set<IrFunctionSymbol> =
            getMethodsForProgressionElements(name) {
                it.valueParameters.size == 1 && it.valueParameters[0].type in progressionElementClassesTypes
            }

    private fun getProgressionBuildingExtensions(name: String, pkg: FqName): Set<IrFunctionSymbol> =
            getExtensionsForProgressionElements(name, pkg) {
                it.extensionReceiverParameter?.type in progressionElementClassesTypes &&
                it.valueParameters.size == 1 &&
                it.valueParameters[0].type in progressionElementClassesTypes
            }

    private fun getMethodsForProgressionElements(name: String,
                                                 filter: (SimpleFunctionDescriptor) -> Boolean): Set<IrFunctionSymbol> =
            mutableSetOf<IrFunctionSymbol>().apply {
                progressionElementClasses.flatMapTo(this) { receiver ->
                    receiver.descriptor.unsubstitutedMemberScope
                            .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                            .filter(filter).map { symbols.symbolTable.referenceFunction(it) }
                }
            }

    private fun getExtensionsForProgressionElements(name: String,
                                                    pkg: FqName,
                                                    filter: (SimpleFunctionDescriptor) -> Boolean): Set<IrFunctionSymbol> =
            mutableSetOf<IrFunctionSymbol>().apply {
                progressionElementClasses.flatMapTo(this) { _ /* receiver */ ->
                    context.builtIns.builtInsModule.getPackage(pkg).memberScope
                            .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                            .filter(filter).map { symbols.symbolTable.referenceFunction(it) }
                }
            }

    private val rangeToSymbols by lazy { getProgressionBuildingMethods("rangeTo") }
    private val untilSymbols by lazy { getProgressionBuildingExtensions("until", FqName("kotlin.ranges")) }
    private val downToSymbols by lazy { getProgressionBuildingExtensions("downTo", FqName("kotlin.ranges")) }
    private val stepSymbols by lazy {
        getExtensionsForProgressionElements("step", FqName("kotlin.ranges")) {
            it.extensionReceiverParameter?.type in symbols.progressionClassesTypes &&
                    it.valueParameters.size == 1 &&
                    (KotlinBuiltIns.isLong(it.valueParameters[0].type) || KotlinBuiltIns.isInt(it.valueParameters[0].type))
        }
    }
    //endregion

    //region Util methods ==============================================================================================
    private fun IrExpression.castIfNecessary(progressionType: ProgressionType): IrExpression {
        assert(type in progressionElementClassesTypes || type in progressionElementClassesNullableTypes)
        return if (type == progressionType.elementType) {
            this
        } else {
            IrCallImpl(startOffset, endOffset, symbols.getFunction(progressionType.numberCastFunctionName, type))
                    .apply { dispatchReceiver = this@castIfNecessary }
        }
    }

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression): IrExpression {
        return if (expression.type.isMarkedNullable) {
            irImplicitCast(expression, expression.type.makeNotNullable())
        } else {
            expression
        }
    }

    private fun IrExpression.unaryMinus(): IrExpression =
            IrCallImpl(startOffset, endOffset, symbols.getUnaryOperator(OperatorNameConventions.UNARY_MINUS, type)).apply {
                dispatchReceiver = this@unaryMinus
            }

    private fun ProgressionInfo.defaultStep(startOffset: Int, endOffset: Int): IrExpression =
        progressionType.elementType.let { type ->
            val step = if (increasing) 1 else -1
            when {
                KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isChar(type) ->
                    IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, step)
                KotlinBuiltIns.isLong(type) ->
                    IrConstImpl.long(startOffset, endOffset, context.builtIns.longType, step.toLong())
                else -> throw IllegalArgumentException()
            }
        }

    private fun ProgressionInfo.isStepOne(): Boolean = step == null || step is IrConst<*> && step.isOne()

    private fun IrConst<*>.isOne() =
        when (kind) {
            IrConstKind.Long -> value as Long == 1L
            IrConstKind.Int  -> value as Int == 1
            else -> false
        }

    // Used only by the assert.
    private fun stepHasRightType(step: IrExpression, progressionType: ProgressionType) =
            ((progressionType.isCharProgression() || progressionType.isIntProgression()) &&
                    KotlinBuiltIns.isInt(step.type.makeNotNullable())) ||
            (progressionType.isLongProgression() &&
                    KotlinBuiltIns.isLong(step.type.makeNotNullable()))

    private fun irCheckProgressionStep(progressionType: ProgressionType,
                                       step: IrExpression): Pair<IrExpression, Boolean> {
        if (step is IrConst<*> &&
            ((step.kind == IrConstKind.Long && step.value as Long > 0) ||
            (step.kind == IrConstKind.Int && step.value as Int > 0))) {
            return step to !step.isOne()
        }
        // The frontend checks if the step has a right type (Long for LongProgression and Int for {Int/Char}Progression)
        // so there is no need to cast it.
        assert(stepHasRightType(step, progressionType))

        val symbol = symbols.checkProgressionStep[step.type.makeNotNullable()]
                ?: throw IllegalArgumentException("Unknown progression element type: ${step.type}")
        return IrCallImpl(step.startOffset, step.endOffset, symbol).apply {
            putValueArgument(0, step)
        } to true
    }

    private fun irGetProgressionLast(progressionType: ProgressionType,
                                     first: IrVariableSymbol,
                                     lastExpression: IrExpression,
                                     step: IrVariableSymbol): IrExpression {
        val symbol = symbols.getProgressionLast[progressionType.elementType]
                ?: throw IllegalArgumentException("Unknown progression element type: ${lastExpression.type}")
        val startOffset = lastExpression.startOffset
        val endOffset = lastExpression.endOffset
        return IrCallImpl(startOffset, lastExpression.endOffset, symbol).apply {
            putValueArgument(0, IrGetValueImpl(startOffset, endOffset, first))
            putValueArgument(1, lastExpression.castIfNecessary(progressionType))
            putValueArgument(2, IrGetValueImpl(startOffset, endOffset, step))
        }
    }
    //endregion

    //region Util classes ==============================================================================================
    // TODO: Replace with a cast when such support is added in the boxing lowering.
    private data class ProgressionType(val elementType: KotlinType,
                                       val numberCastFunctionName: Name,
                                       val typeSymbol : IrClassSymbol)
    {
        fun isIntProgression()  = KotlinBuiltIns.isInt(elementType)
        fun isLongProgression() = KotlinBuiltIns.isLong(elementType)
        fun isCharProgression() = KotlinBuiltIns.isChar(elementType)
    }

    private data class ProgressionInfo(
            val progressionType: ProgressionType,
            val first: IrExpression,
            val bound: IrExpression,
            val step: IrExpression? = null,
            val increasing: Boolean = true,
            var needLastCalculation: Boolean = false,
            val closed: Boolean = true,

            // Mean that first, bound and step of the progression are
            // received with calls of appropriate properties of already created progression.
            val isCalculatedWithCalls: Boolean = false,

            // If calculated with calls then this will be call progression.isEmpty()
            // else it will be null.
            val isEmptyCond: IrExpression? = null,

            // TODO: Rename?
            // If induction variable doesn't equal to loop variable
            // for example in for loops on containers
            // it's necessary to create another one temporary variable
            // to check the end condition in do-while loop.
            val isEqualInductionVariableAndLoopVariable: Boolean = true,

            // A temporary variable of a container (e.g. an array if the loop is on the array) to get
            // elements from. Null if the container is a variable and created not in the scope of the FOR-loop.
            val containerVariable: IrVariable? = null,

            // Expression to get the container on which the loop is.
            // Null if the loop is not on a container.
            val getContainerExpr: IrGetValue? = null
    )

    /** Contains information about variables used in the loop. */
    private data class ForLoopInfo(
            val progressionInfo: ProgressionInfo,
            val inductionVariable: IrVariableSymbol,
            val bound: IrVariableSymbol,
            val last: IrVariableSymbol,
            val step: IrVariableSymbol,
            var loopVariable: IrVariableSymbol? = null)
    {
        // Mean that first, bound and step of the progression are
        // received with calls of appropriate properties of already created progression.
        val isCalculatedWithCalls: Boolean
            get() = progressionInfo.isCalculatedWithCalls

        // If calculated with calls then this will be call progression.isEmpty()
        // else it will be null.
        val isEmptyCond: IrExpression?
            get() = progressionInfo.isEmptyCond

        // TODO: Rename?
        // If induction variable doesn't equal to loop variable
        // for example in for loops on containers
        // it's necessary to create another one temporary variable
        // to check the end condition in do-while loop.
        val isEqualsInductionVariableAndLoopVariable: Boolean
            get() = progressionInfo.isEqualInductionVariableAndLoopVariable

        // Expression to get the container on which the loop is.
        // Null if the loop is not on a container.
        val getContainerExpr: IrGetValue?
            get() = progressionInfo.getContainerExpr
    }

    private inner class ProgressionInfoBuilder : IrElementVisitor<ProgressionInfo?, Nothing?> {

        val INT_PROGRESSION = ProgressionType(context.builtIns.intType, Name.identifier("toInt"), symbols.intProgression)
        val LONG_PROGRESSION = ProgressionType(context.builtIns.longType, Name.identifier("toLong"), symbols.longProgression)
        val CHAR_PROGRESSION = ProgressionType(context.builtIns.charType, Name.identifier("toChar"), symbols.charProgression)

        private fun buildRangeTo(expression: IrCall, progressionType: ProgressionType) =
                ProgressionInfo(progressionType,
                        expression.dispatchReceiver!!,
                        expression.getValueArgument(0)!!)

        private fun buildUntil(expression: IrCall, progressionType: ProgressionType): ProgressionInfo =
                ProgressionInfo(progressionType,
                        expression.extensionReceiver!!,
                        expression.getValueArgument(0)!!,
                        closed = false)

        private fun buildDownTo(expression: IrCall, progressionType: ProgressionType) =
                ProgressionInfo(progressionType,
                        expression.extensionReceiver!!,
                        expression.getValueArgument(0)!!,
                        increasing = false)

        private fun buildStep(expression: IrCall, progressionType: ProgressionType) =
                expression.extensionReceiver!!.accept(this, null)?.let {
                    if (it.isCalculatedWithCalls || it.step != null) {
                        return null
                    }
                    val step = expression.getValueArgument(0)!!
                    val (stepCheck, needBoundCalculation) = irCheckProgressionStep(progressionType, step)
                    ProgressionInfo(progressionType, it.first, it.bound, stepCheck,
                            it.increasing, needBoundCalculation, it.closed,
                            it.isCalculatedWithCalls, it.isEmptyCond,
                            it.isEqualInductionVariableAndLoopVariable,
                            it.containerVariable, it.getContainerExpr)
                }

        private fun buildProgressionInfoFromGetIndices(expression: IrCall, progressionType: ProgressionType) : ProgressionInfo {
            val builder = context.createIrBuilder(scopeOwnerSymbol, expression.startOffset, expression.endOffset)
            with (builder) {
                val const0 = IrConstImpl.int(expression.startOffset, expression.endOffset, context.builtIns.intType, 0)
                val const1 = IrConstImpl.int(expression.startOffset, expression.endOffset, context.builtIns.intType, 1)

                val size = irCall(symbols.collectionSize).apply {
                    dispatchReceiver = expression.extensionReceiver
                }

                assert(size.type.isInt() && progressionType.isIntProgression())
                val minusOperator = symbols.getBinaryOperator(
                        OperatorNameConventions.MINUS,
                        size.type,
                        const1.type
                )
                val bound = irCallOp(minusOperator, size, const1)

                return ProgressionInfo(progressionType, const0, bound)
            }
        }

        override fun visitElement(element: IrElement, data: Nothing?): ProgressionInfo? = null

        fun KotlinType.progressionType(): ProgressionType? = when {
            isSubtypeOf(symbols.charProgression.descriptor.defaultType) -> CHAR_PROGRESSION
            isSubtypeOf(symbols.intProgression.descriptor.defaultType) -> INT_PROGRESSION
            isSubtypeOf(symbols.longProgression.descriptor.defaultType) -> LONG_PROGRESSION
            else -> null
        }

        fun buildProgressionInfoContainerCase(varValuesContainer: IrVariable?,
                                              exprValuesContainer : IrGetValue,
                                              containerSizeSymbol : IrSimpleFunctionSymbol,
                                              builder : DeclarationIrBuilder) : ProgressionInfo
        {
            with (builder) {
                val first = IrConstImpl.int(exprValuesContainer.startOffset, exprValuesContainer.endOffset,
                        context.builtIns.intType, 0)
                val callArraySize1 = irCall(containerSizeSymbol).apply {
                    dispatchReceiver = exprValuesContainer.copy()
                }
                val callArraySize2 = irCall(containerSizeSymbol).apply {
                    dispatchReceiver = exprValuesContainer.copy()
                }
                val const0 = IrConstImpl.int(exprValuesContainer.startOffset, exprValuesContainer.endOffset, context.builtIns.intType, 0)
                val bound = callArraySize1
                val isEmpty = irCall(context.irBuiltIns.eqeqSymbol).apply {
                    putValueArgument(0, callArraySize2)
                    putValueArgument(1, const0)
                }
                return ProgressionInfo(INT_PROGRESSION, first, bound,
                        containerVariable = varValuesContainer,
                        getContainerExpr = exprValuesContainer,
                        isCalculatedWithCalls = true,
                        isEmptyCond = isEmpty,
                        isEqualInductionVariableAndLoopVariable = false)
            }
        }

        override fun visitGetValue(expression: IrGetValue, data: Nothing?): ProgressionInfo? {
            val progressionType = expression.type.progressionType()

            if (progressionType != null) {
                val builder = context.createIrBuilder(scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                with (builder) {
                    val typeSymbol = progressionType.typeSymbol
                    val first = irCall(symbols.progressionFirst.getValue(typeSymbol)).apply {
                        dispatchReceiver = expression.copy()
                    }
                    val bound = irCall(symbols.progressionLast.getValue(typeSymbol)).apply {
                        dispatchReceiver = expression.copy()
                    }
                    val step = irCall(symbols.progressionStep.getValue(typeSymbol)).apply {
                        dispatchReceiver = expression.copy()
                    }
                    val isEmpty = irCall(symbols.progressionIsEmpty.getValue(typeSymbol)).apply {
                        dispatchReceiver = expression.copy()
                    }
                    return ProgressionInfo(progressionType, first, bound, step,
                            isCalculatedWithCalls = true,
                            isEmptyCond = isEmpty)
                }
            }

            // Process a foreach loop on an array: 'for (v in array) { ... }'
            if (expression.type.isSubtypeOf(symbols.array.descriptor.defaultType.replaceArgumentsWithStarProjections())) {
                val builder = context.createIrBuilder(scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                return buildProgressionInfoContainerCase(null, expression, symbols.arraySize, builder)
            }
            // TODO: other container cases

            return null
        }

        override fun visitBlock(expression: IrBlock, data: Nothing?): ProgressionInfo? {
            val type = expression.type
            if (type.isSubtypeOf(symbols.array.descriptor.defaultType.replaceArgumentsWithStarProjections())) {
                val builder = context.createIrBuilder(scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                with (builder) {
                    val varValuesContainer = scope.createTemporaryVariable(expression,
                            nameHint = "valuesContainer",
                            isMutable = true,
                            origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)
                    val exprValuesContainer = irGet(varValuesContainer.symbol)
                    return buildProgressionInfoContainerCase(varValuesContainer, exprValuesContainer, symbols.arraySize, this)
                }
            }
            // TODO: other container cases

            return null
        }

        override fun visitCall(expression: IrCall, data: Nothing?): ProgressionInfo? {
            val progressionType = expression.type.progressionType() ?: return null

            return when (expression.symbol) {
                in rangeToSymbols -> buildRangeTo(expression, progressionType)
                in untilSymbols -> buildUntil(expression, progressionType)
                in downToSymbols -> buildDownTo(expression, progressionType)
                in stepSymbols -> buildStep(expression, progressionType)
                symbols.collectionIndices -> buildProgressionInfoFromGetIndices(expression, progressionType)
                else -> null
            }
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
        val progressionInfo = initializer.dispatchReceiver?.accept(ProgressionInfoBuilder(), null) ?: return null

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
                containerVariable?.let {
                    statements.add(it)
                }

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


                assert(!isCalculatedWithCalls || increasing)
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
                    lastExpression = irCall(decrementSymbol).apply {
                        dispatchReceiver = irGet(boundValue.symbol)
                    }
                }
                if (needLastCalculation) {
                    lastExpression = irGetProgressionLast(progressionType,
                            inductionVariable.symbol,
                            lastExpression ?: irGet(boundValue.symbol),
                            stepValue.symbol)
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
                        inductionVariable.symbol,
                        boundValue.symbol,
                        lastValue.symbol,
                        stepValue.symbol)

                return IrCompositeImpl(startOffset, endOffset, context.builtIns.unitType, null, statements)
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
        forLoopInfo.loopVariable = variable.symbol // always

        with(builder) {
            val increment = irSetVar(forLoopInfo.inductionVariable,
                    irCallOp(plusOperator, irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.step)))

            variable.initializer = if (forLoopInfo.isEqualsInductionVariableAndLoopVariable) {
                irGet(forLoopInfo.inductionVariable)
            } else {
                irCall(symbols.arrayGet).apply {
                    dispatchReceiver = forLoopInfo.getContainerExpr!!.copy()
                    putValueArgument(0, irGet(forLoopInfo.inductionVariable))
                }
            }
            return IrCompositeImpl(variable.startOffset,
                    variable.endOffset,
                    context.irBuiltIns.unit,
                    IrStatementOrigin.FOR_LOOP_NEXT,
                    listOf(variable, increment))
        }

    }

    fun buildComparison(lhs: IrExpression,
                        rhs: IrExpression,
                        type : SimpleType,
                        comparingBuiltins : Map<SimpleType, IrSimpleFunction>) : IrCall
    {
        val irBuilder = context.createIrBuilder(scopeOwnerSymbol, lhs.startOffset, lhs.endOffset)
        with (irBuilder) {
            var comparingBuiltIn = comparingBuiltins[type]?.symbol
            return if (comparingBuiltIn != null) {
                irCall(comparingBuiltIn).apply {
                    putValueArgument(0, lhs)
                    putValueArgument(1, rhs)
                }
            } else {
                val builtIns = context.irBuiltIns
                comparingBuiltIn = comparingBuiltins.getValue(builtIns.int).symbol

                val compareTo = symbols.getBinaryOperator(OperatorNameConventions.COMPARE_TO,
                        lhs.type, rhs.type)

                irCall(comparingBuiltIn).apply {
                    putValueArgument(0, irCallOp(compareTo, lhs, rhs))
                    putValueArgument(1, irInt(0))
                }
            }
        }
    }

    private fun DeclarationIrBuilder.buildMinValueCondition(forLoopInfo: ForLoopInfo): IrExpression {
        // Condition for a corner case: for (i in a until Int.MIN_VALUE) {}.
        // Check if forLoopInfo.bound > MIN_VALUE.
        val expressionType = forLoopInfo.progressionInfo.progressionType.elementType.asSimpleType()

        val minConst = when {
            expressionType.isInt() -> IrConstImpl
                    .int(startOffset, endOffset, context.builtIns.intType, Int.MIN_VALUE)
            expressionType.isChar() -> IrConstImpl
                    .char(startOffset, endOffset, context.builtIns.charType, 0.toChar())
            expressionType.isLong() -> IrConstImpl
                    .long(startOffset, endOffset, context.builtIns.longType, Long.MIN_VALUE)
            else -> throw IllegalArgumentException("Unknown progression type")
        }

        val comparingBuiltins = context.irBuiltIns.greaterFunByOperandType
        return buildComparison(irGet(forLoopInfo.bound), minConst, expressionType, comparingBuiltins)
    }

    // TODO: Eliminate the loop if we can prove that it will not be executed.
    private fun DeclarationIrBuilder.buildEmptyCheck(loop: IrLoop, forLoopInfo: ForLoopInfo): IrExpression {
        if (forLoopInfo.isCalculatedWithCalls) {
            val check = irCall(context.irBuiltIns.booleanNotSymbol).apply {
                putValueArgument(0, forLoopInfo.isEmptyCond)
            }
            return irIfThen(check, loop)
        }

        val builtIns = context.irBuiltIns
        val increasing = forLoopInfo.progressionInfo.increasing

        val expressionType = forLoopInfo.progressionInfo.progressionType.elementType.asSimpleType()
        assert(expressionType.isInt() || expressionType.isChar() || expressionType.isLong())

        val comparingBuiltIns = if (increasing) builtIns.lessOrEqualFunByOperandType
                                else builtIns.greaterOrEqualFunByOperandType
        val check = buildComparison(irGet(forLoopInfo.inductionVariable), irGet(forLoopInfo.last),
                    expressionType, comparingBuiltIns)

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

        val comparingWithLast = if (forLoopInfo.isEqualsInductionVariableAndLoopVariable) {
            irGet(forLoopInfo.loopVariable!!)
        }
        else {
            irGet(forLoopInfo.inductionVariable)
        }
        return irCall(context.irBuiltIns.booleanNotSymbol).apply {
            val eqeqCall = irCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, comparingWithLast)
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


    override fun visitCall(expression: IrCall) : IrExpression {
        expression.transformChildrenVoid(this)

        if (expression.origin != IrStatementOrigin.IN) {
            return expression
        }

        val dispatchReceiverOfCall = expression.dispatchReceiver
        val extensionReceiverOfCall = expression.extensionReceiver
        val progressionInfo = when {
            dispatchReceiverOfCall is IrCall || dispatchReceiverOfCall is IrGetValue ->
                dispatchReceiverOfCall.accept(ProgressionInfoBuilder(), null) ?: return expression
            extensionReceiverOfCall is IrCall || extensionReceiverOfCall is IrGetValue ->
                extensionReceiverOfCall.accept(ProgressionInfoBuilder(), null) ?: return expression
            else -> return expression
        }

        // Checking that types is equals
        assert(progressionInfo.first.type == progressionInfo.bound.type)
        if (progressionInfo.first.type != expression.getValueArgument(0)!!) {
            return expression
        }
        val comparisonType = progressionInfo.first.type.asSimpleType()
        // TODO: consider cases with different types
        // TODO: e.x. (long in int .. int) or (int in long .. long)

        if (!progressionInfo.isStepOne() || !progressionInfo.isEqualInductionVariableAndLoopVariable) {
            return expression
        }

        val irBuilder = context.createIrBuilder(scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val constFalse = IrConstImpl.boolean(expression.startOffset, expression.endOffset, context.builtIns.booleanType, false)
        with (irBuilder) {
            val statements = mutableListOf<IrStatement>()
            val builtIns = context.irBuiltIns

            // Creating variables to ensure correct initialization order

            val varLeft = scope.createTemporaryVariable(progressionInfo.first,
                    nameHint = "left bound",
                    isMutable = false,
                    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE).also {
                statements.add(it)
            }.symbol

            val varRight = scope.createTemporaryVariable(progressionInfo.bound,
                    nameHint = "right bound",
                    isMutable = false,
                    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE).also {
                statements.add(it)
            }.symbol

            val varComp = scope.createTemporaryVariable(expression.getValueArgument(0)!!,
                    nameHint = "variable to compare",
                    isMutable = false,
                    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE).also {
                statements.add(it)
            }.symbol

            val comparingLeft = when {
                progressionInfo.increasing -> builtIns.lessOrEqualFunByOperandType
                else -> builtIns.greaterOrEqualFunByOperandType
            }
            val callCheckLeft = buildComparison(irGet(varLeft), irGet(varComp), comparisonType, comparingLeft)

            val comparingRight = when {
                progressionInfo.increasing && progressionInfo.closed -> builtIns.lessOrEqualFunByOperandType
                progressionInfo.increasing && !progressionInfo.closed -> builtIns.lessFunByOperandType
                !progressionInfo.increasing && progressionInfo.closed -> builtIns.greaterOrEqualFunByOperandType
                else -> builtIns.greaterFunByOperandType
            }
            val callCheckRight = buildComparison(irGet(varComp), irGet(varRight), comparisonType, comparingRight)

            irIfThenElse(context.builtIns.booleanType, callCheckLeft, callCheckRight, constFalse).also {
                statements.add(it)
            }

            return IrCompositeImpl(startOffset, endOffset, context.builtIns.booleanType, null, statements)
        }
    }
    //endregion
}

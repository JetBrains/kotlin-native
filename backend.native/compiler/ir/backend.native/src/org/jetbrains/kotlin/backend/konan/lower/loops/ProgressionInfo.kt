package org.jetbrains.kotlin.backend.konan.lower.loops

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable


// TODO: Replace with a cast when such support is added in the boxing lowering.
internal data class ProgressionType(val elementType: KotlinType, val numberCastFunctionName: Name) {
    fun isIntProgression()  = KotlinBuiltIns.isInt(elementType)
    fun isLongProgression() = KotlinBuiltIns.isLong(elementType)
    fun isCharProgression() = KotlinBuiltIns.isChar(elementType)
}

/** Contains information about variables used in the loop. */
internal data class ForLoopInfo(
        val progressionInfo: ProgressionInfo,
        val inductionVariable: IrVariable,
        val bound: IrVariable,
        val last: IrVariable,
        val step: IrVariable,
        var loopVariable: IrVariable? = null)

internal data class ProgressionInfo(
        val progressionType: ProgressionType,
        val first: IrExpression,
        val bound: IrExpression,
        val step: IrExpression? = null,
        val increasing: Boolean = true,
        var needLastCalculation: Boolean = false,
        val closed: Boolean = true)

internal class ProgressionInfoBuilder(val context: Context) : IrElementVisitor<ProgressionInfo?, Nothing?> {

    private val symbols = context.ir.symbols

    private val intProgression = ProgressionType(context.builtIns.intType, Name.identifier("toInt"))
    private val longProgression = ProgressionType(context.builtIns.longType, Name.identifier("toLong"))
    private val charProgression = ProgressionType(context.builtIns.charType, Name.identifier("toChar"))

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

    private val progressionElementClasses: List<IrClassSymbol> = mutableListOf(symbols.char).apply {
        addAll(symbols.integerClasses)
    }

    private val progressionElementClassesTypes: List<SimpleType> = mutableListOf<SimpleType>().apply {
        progressionElementClasses.mapTo(this) { it.descriptor.defaultType }
    }

    private val progressionElementClassesNullableTypes: List<SimpleType> = mutableListOf<SimpleType>().apply {
        progressionElementClassesTypes.mapTo(this) { it.makeNullableAsSpecified(true) }
    }

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
                val newStep = expression.getValueArgument(0)!!
                val (newStepCheck, needBoundCalculation) = irCheckProgressionStep(progressionType, newStep)
                val step = when {
                    it.step == null -> newStepCheck
                    // There were step calls before. Just add our check in the container or create a new one.
                    it.step is IrStatementContainer -> {
                        it.step.statements.add(newStepCheck)
                        it.step
                    }
                    else -> IrCompositeImpl(expression.startOffset, expression.endOffset, newStep.type).apply {
                        statements.add(it.step)
                        statements.add(newStepCheck)
                    }
                }
                ProgressionInfo(progressionType, it.first, it.bound, step, it.increasing, needBoundCalculation, it.closed)
            }

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

        val symbol = symbols.checkProgressionStep[step.type.toKotlinType().makeNotNullable()]
                ?: throw IllegalArgumentException("Unknown progression element type: ${step.type}")
        return IrCallImpl(step.startOffset, step.endOffset, symbol.owner.returnType, symbol).apply {
            putValueArgument(0, step)
        } to true
    }

    private fun IrConst<*>.isOne() =
            when (kind) {
                IrConstKind.Long -> value as Long == 1L
                IrConstKind.Int  -> value as Int == 1
                else -> false
            }

    // Used only by the assert.
    private fun stepHasRightType(step: IrExpression, progressionType: ProgressionType) =
            ((progressionType.isCharProgression() || progressionType.isIntProgression()) &&
                    KotlinBuiltIns.isInt(step.type.toKotlinType().makeNotNullable())) ||
                    (progressionType.isLongProgression() &&
                            KotlinBuiltIns.isLong(step.type.toKotlinType().makeNotNullable()))

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

    override fun visitElement(element: IrElement, data: Nothing?): ProgressionInfo? = null

    override fun visitCall(expression: IrCall, data: Nothing?): ProgressionInfo? {
        val type = expression.type.toKotlinType()
        val progressionType = when {
            type.isSubtypeOf(symbols.charProgression.descriptor.defaultType) -> charProgression
            type.isSubtypeOf(symbols.intProgression.descriptor.defaultType) -> intProgression
            type.isSubtypeOf(symbols.longProgression.descriptor.defaultType) -> longProgression
            else -> return null
        }

        // TODO: Process constructors and other factory functions.
        return when (expression.symbol) {
            in rangeToSymbols -> buildRangeTo(expression, progressionType)
            in untilSymbols -> buildUntil(expression, progressionType)
            in downToSymbols -> buildDownTo(expression, progressionType)
            in stepSymbols -> buildStep(expression, progressionType)
            else -> null
        }
    }
}
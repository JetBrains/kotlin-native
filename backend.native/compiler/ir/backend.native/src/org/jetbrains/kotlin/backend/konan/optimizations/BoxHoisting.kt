package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.cfg.*
import org.jetbrains.kotlin.backend.konan.cfg.analysis.AnalysisResult
import org.jetbrains.kotlin.backend.konan.cfg.analysis.ReversePostorderWorkList
import org.jetbrains.kotlin.backend.konan.cfg.analysis.WorkListIntraproceduralCfgAnalyzer
import org.jetbrains.kotlin.backend.konan.getBoxFunction
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.defaultOrNullableType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import java.io.File
import kotlin.collections.set

internal object BoxHoisting {

    fun run(context: Context, function: IrFunction) {
        val valuesBoxedInSeveralPlaces = function.extractValuesBoxedInSeveralPlaces()
        if (valuesBoxedInSeveralPlaces.isEmpty()) {
            return
        }
        println("Analyzing ${function.fqNameForIrSerialization.asString()}")
        println(valuesBoxedInSeveralPlaces.joinToString { it.owner.name.asString() })

        val (functionEntry, _) = function.buildCfg(CfgServiceFunctionsFilter(listOf(context.ir.symbols.reinterpret)))

        val analysis = RepeatedBoxingsWorkListAnalysis(valuesBoxedInSeveralPlaces, functionEntry)
        val analysisResult = analysis.analyze(functionEntry)
        val boxingVariables = analysisResult.values.flatMap { it.resultOut }.map { it.owner }
        val boxedVariables = boxingVariables.map { it.createBoxedVariable(context) }

        val unboxedToBoxedValueMap = mutableMapOf(*boxingVariables.zip(boxedVariables).toTypedArray())
        val newFunction = replaceBoxOperations(context, function, functionEntry, analysisResult, unboxedToBoxedValueMap)
        printCfgWithAnalysisResultOnLabels(newFunction, context, function, functionEntry, analysisResult)
    }

    private fun printCfgWithAnalysisResultOnLabels(newFunction: IrStatement, context: Context, function: IrFunction, functionEntry: BasicBlock, analysisResult: Map<BasicBlock, RepeatedBoxingsAnalysisResult>) {
        val (newFunctionEntry, _) = newFunction.buildCfg(CfgServiceFunctionsFilter(listOf(context.ir.symbols.reinterpret)))
        File("dot/${function.name}_before.dot").appendText(functionEntry.printDotGraph(function.name.asString()) { analysisResult[it.from]?.resultOut?.joinToString { it.owner.name.asString() } })
        File("dot/${function.name}_after.dot").appendText(newFunctionEntry.printDotGraph(function.name.asString()))
    }

    private fun IrFunction.extractValuesBoxedInSeveralPlaces(): Set<IrValueSymbol> {
        val overallBoxingsInfo = OverallBoxingsInfo()
        acceptChildren(OverallBoxingsCountVisitor(), overallBoxingsInfo)
        return overallBoxingsInfo.getInfo().filterValues { it > 1 }.keys
    }

    private class OverallBoxingsInfo {
        private val overallBoxings: MutableMap<IrValueSymbol, Int> = mutableMapOf()

        fun newBoxing(symbol: IrValueSymbol) {
            if (symbol !in overallBoxings) {
                overallBoxings[symbol] = 1
            } else {
                overallBoxings[symbol] = overallBoxings[symbol]!! + 1
            }
        }

        fun getInfo() = overallBoxings
    }

    private class OverallBoxingsCountVisitor : IrElementVisitor<Unit, OverallBoxingsInfo> {
        override fun visitElement(element: IrElement, data: OverallBoxingsInfo) {
            element.acceptChildren(this, data)
        }

        override fun visitCall(expression: IrCall, data: OverallBoxingsInfo) {
            expression.getArgumentIfCallsBoxFunction()?.let { argument ->
                data.newBoxing(argument)
            }
            super.visitCall(expression, data)
        }
    }

    class RepeatedBoxingsAnalysisResult(val resultIn: Set<IrValueSymbol>, val resultOut: Set<IrValueSymbol>) : AnalysisResult<RepeatedBoxingsAnalysisResult> {
        override fun bottom(): RepeatedBoxingsAnalysisResult {
            return of(emptyList())
        }

        override fun differsFrom(other: RepeatedBoxingsAnalysisResult): Boolean {
            return resultIn != other.resultIn || resultOut != other.resultOut
        }

        companion object {
            fun of(collection: Collection<IrValueSymbol>): RepeatedBoxingsAnalysisResult {
                val collectionAsSet = if (collection is Set<IrValueSymbol>) collection else collection.toSet()
                return RepeatedBoxingsAnalysisResult(collectionAsSet, collectionAsSet)
            }

            fun empty(): RepeatedBoxingsAnalysisResult {
                return of(emptyList())
            }
        }
    }

    class RepeatedBoxingsWorkListAnalysis(val valuesToAnalyze: Set<IrValueSymbol>, val entryBlock: BasicBlock)
        : WorkListIntraproceduralCfgAnalyzer<RepeatedBoxingsAnalysisResult>(ReversePostorderWorkList(entryBlock)) {
        override fun initialFor(basicBlock: BasicBlock): RepeatedBoxingsAnalysisResult {
            if (basicBlock === entryBlock) {
                return RepeatedBoxingsAnalysisResult.empty()
            }
            return RepeatedBoxingsAnalysisResult.of(valuesToAnalyze)
        }

        override fun evaluate(basicBlock: BasicBlock, currentAnalysisResult: Map<BasicBlock, RepeatedBoxingsAnalysisResult>): RepeatedBoxingsAnalysisResult {
            val resultIn = basicBlock.incomingEdges.map {
                currentAnalysisResult[it.from]!!.resultOut
            }.reduceOrNull { acc, next -> acc intersect next } ?: setOf()

            val gen = mutableSetOf<IrValueSymbol>()
            val kill = mutableSetOf<IrValueSymbol>()
            for (element in basicBlock.elements) {
                when (val statement = element.statement) {
                    is IrCall -> {
                        val argument = statement.getArgumentIfCallsBoxFunction()
                        // values in valuesToAnalyze must already be primitive
                        if (argument in valuesToAnalyze) {
                            gen += argument as IrValueSymbol
                        }
                    }

                    is IrSetVariable -> {
                        val argument = statement.symbol
                        if (argument in valuesToAnalyze) {
                            kill += argument
                        }
                    }
                }
            }

            val resultOut = resultIn.toMutableSet()
            resultOut += gen
            resultOut -= kill
            return RepeatedBoxingsAnalysisResult(resultIn, resultOut)
        }
    }

    private fun IrValueDeclaration.createBoxedVariable(context: Context): IrVariableImpl {
        val boxFunction = context.getBoxFunction(type.getClass()!!)
        val descriptor = WrappedVariableDescriptor()
        return IrVariableImpl(
                startOffset,
                endOffset,
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrVariableSymbolImpl(descriptor),
                Name.identifier("$name-Boxed"),
                context.ir.symbols.any.owner.defaultOrNullableType(type.isNullable()),
                if (this is IrVariable) isVar else true,
                if (this is IrVariable) isConst else false,
                if (this is IrVariable) isLateinit else false
        ).apply {
            initializer = context.createIrBuilder(symbol).run {
                (irCall(boxFunction) as IrCall).apply {
                    putValueArgument(0, irGet(this@createBoxedVariable))
                }
            }
            descriptor.bind(this)
            parent = this@createBoxedVariable.parent
        }
    }

    private fun replaceBoxOperations(
            context: Context,
            function: IrFunction,
            functionEntryBlock: BasicBlock,
            globalAnalysisResult: Map<BasicBlock, RepeatedBoxingsAnalysisResult>,
            unboxedToBoxedValueMap: MutableMap<IrValueDeclaration, IrVariableImpl>
    ): IrStatement {

        class BoxVariableInsertionData(val call: IrCall, val container: IrStatementContainer, val index: Int)
        abstract class BoxReplacementStrategy
        class JustReplaceBoxingStrategy(val boxingValueSymbol: IrValueSymbol) : BoxReplacementStrategy()
        class ReplaceBoxAndInsertVariableStrategy(val boxingValueSymbol: IrValueSymbol, val insertionData: BoxVariableInsertionData) : BoxReplacementStrategy()

        val boxReplacementsInfo = mutableMapOf<IrCall, BoxReplacementStrategy>()
        val probableFirstBoxInfo = mutableMapOf<IrValueSymbol, BoxVariableInsertionData>()

        functionEntryBlock.traverseBfs(boxReplacementsInfo) { currentBoxReplacementsInfo, nextBasicBlock ->
            val globalResultForBlock = globalAnalysisResult[nextBasicBlock]!!
            val localResultForBlock = mutableSetOf<IrValueSymbol>()
            localResultForBlock += globalResultForBlock.resultIn

            for (element in nextBasicBlock.elements) {
                when (val statement = element.statement) {
                    is IrCall -> {
                        val argument = statement.getArgumentIfCallsBoxFunction()
                        if (argument?.isPrimitive() == true) {
                            if (argument in localResultForBlock) {
                                currentBoxReplacementsInfo[statement] = JustReplaceBoxingStrategy(argument)
                                probableFirstBoxInfo[argument]?.let { insertionData ->
                                    currentBoxReplacementsInfo[insertionData.call] = ReplaceBoxAndInsertVariableStrategy(argument, insertionData)
                                    probableFirstBoxInfo.remove(argument)
                                }
                            } else {
                                probableFirstBoxInfo[argument] = BoxVariableInsertionData(statement, element.container, element.index)
                                localResultForBlock += argument
                            }
                        }
                    }

                    is IrSetVariable -> {
                        val argument = statement.symbol
                        if (argument.isPrimitive()) {
                            localResultForBlock.remove(argument)
                            probableFirstBoxInfo.remove(argument)
                        }
                    }
                }
            }
        }

        boxReplacementsInfo.forEach { call, replacementStrategy ->
            println(call.render())
            when (replacementStrategy) {
                is JustReplaceBoxingStrategy -> println("replace ${replacementStrategy.boxingValueSymbol.owner.name}")
                is ReplaceBoxAndInsertVariableStrategy -> println("replace & insert ${replacementStrategy.boxingValueSymbol.owner.name}")
            }
        }

        return function.transform(object : IrElementTransformerVoid() {

            override fun visitBlock(expression: IrBlock): IrExpression {
                return super.visitBlock(expression).withInsertedVariables()
            }

            override fun visitBlockBody(body: IrBlockBody): IrBody {
                return super.visitBlockBody(body).withInsertedVariables()
            }

            override fun visitComposite(expression: IrComposite): IrExpression {
                return super.visitComposite(expression).withInsertedVariables()
            }

            private fun <T : IrElement> T.withInsertedVariables(): T {
                if (this !is IrStatementContainer) {
                    return this
                }
                val boxesToBeCreatedHere: List<Pair<Int, IrVariable>> = boxReplacementsInfo
                        .values
                        .filterIsInstance<ReplaceBoxAndInsertVariableStrategy>()
                        .filter { it.insertionData.container === this }
                        .map { it.insertionData.index to unboxedToBoxedValueMap[it.boxingValueSymbol.owner]!! }
                if (boxesToBeCreatedHere.isNotEmpty()) {
                    var currentOffset = 0
                    boxesToBeCreatedHere.forEach { (index, variable) ->
                        statements.add(index + currentOffset, variable)
                        currentOffset++
                    }
                }
                return this
            }

            override fun visitElement(element: IrElement): IrElement {
                val result = super.visitElement(element)
                if (result is IrStatementContainer) {
                    val boxesToBeCreatedHere: List<Pair<Int, IrVariable>> = boxReplacementsInfo
                            .values
                            .filterIsInstance<ReplaceBoxAndInsertVariableStrategy>()
                            .filter { it.insertionData.container === result }
                            .map { it.insertionData.index to unboxedToBoxedValueMap[it.boxingValueSymbol.owner]!! }
                    if (boxesToBeCreatedHere.isNotEmpty()) {
                        var currentOffset = 0
                        boxesToBeCreatedHere.forEach { (index, variable) ->
                            result.statements.add(index + currentOffset, variable)
                            currentOffset++
                        }
                    }
                }
                return result
            }

            override fun visitCall(expression: IrCall): IrExpression =
                    expression.getArgumentIfCallsBoxFunction()?.let { valueSymbol ->
                        boxReplacementsInfo[expression]?.let {
                            unboxedToBoxedValueMap[valueSymbol.owner]?.let {
                                return context.createIrBuilder(valueSymbol).irGet(it)
                            }
                        }
                    } ?: super.visitCall(expression)
        }, data = null)
    }

    private fun IrValueSymbol.isPrimitive() = owner.type.isPrimitiveType()

    private fun IrCall.getArgumentIfCallsBoxFunction(): IrValueSymbol? {
        if (this.isBoxFunctionCall()) {
            when (val argument = getValueArgument(0)!!) {
                is IrGetValue -> return argument.symbol
            }
        }
        return null
    }
    private fun IrCall.isBoxFunctionCall() = symbol.owner.name.asString().endsWith("-box>")
}
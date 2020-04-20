package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.cfg.BasicBlock
import org.jetbrains.kotlin.backend.konan.cfg.CfgServiceFunctionsFilter
import org.jetbrains.kotlin.backend.konan.cfg.buildCfg
import org.jetbrains.kotlin.backend.konan.cfg.reduceOrNull
import org.jetbrains.kotlin.backend.konan.getBoxFunction
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.defaultOrNullableType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

internal object BoxHoisting {
    fun run(context: Context, function: IrFunction) {
        val (functionEntry, _) = function.buildCfg(CfgServiceFunctionsFilter(listOf(context.ir.symbols.reinterpret)))
        @Suppress("UNUSED_VARIABLE")
        val allValues = collectValues(function)
        // TODO use allValues as starting approximation
        val analysisResult = analyze(functionEntry)
        val boxingVariables = analysisResult.values.flatMap { it.resultIn }.map { it.owner }
        val boxedVariables = boxingVariables.map { it.createBoxedVariable(context) }

        val unboxedToBoxedValueMap = mutableMapOf(*boxingVariables.zip(boxedVariables).toTypedArray())

        function.transform(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                return (super.visitFunction(declaration) as IrFunction).also {
                    val statements = it.body?.statements?.toMutableList()
                    if (statements != null) {
                        unboxedToBoxedValueMap.filterKeys { it is IrVariable }.forEach { (boxingVariable, boxedVariable) ->
                            val indexOfVariable = statements.indexOf(boxingVariable)
                            if (indexOfVariable != -1) {
                                statements.add(indexOfVariable + 1, boxedVariable)
                            }
                        }
                        it.valueParameters.forEach { valueParameter ->
                            unboxedToBoxedValueMap[valueParameter]?.let {
                                statements.add(0, it)
                            }
                        }
                        it.body = context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                            statements.forEach {
                                +it
                            }
                        }
                    }
                }
            }

            override fun visitCall(expression: IrCall): IrExpression =
                expression.getArgumentIfCallsBoxFunction()?.let { valueSymbol ->
                    unboxedToBoxedValueMap[valueSymbol.owner]?.let {
                        return context.createIrBuilder(valueSymbol).irGet(it)
                    }
                } ?: super.visitCall(expression)
        }, data = null)
    }

    private fun collectValues(function: IrFunction): List<IrValueDeclaration> {
        val result = mutableListOf<IrValueDeclaration>()
        result += function.valueParameters
        function.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitVariable(declaration: IrVariable) {
                result += declaration
            }
        })
        return result
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

//    private class AvailableBoxCall(
//            val valueSymbol: IrValueSymbol,
//            val call: IrCall,
//            val last
//    )
//    private fun BasicBlock.evaluateAvailableCalls() {
//
//    }
    class AnalysisData(val resultIn: Set<IrValueSymbol>, val resultOut: Set<IrValueSymbol>)

    fun analyze(functionEntry: BasicBlock): Map<BasicBlock, AnalysisData> {
        val analysisResult = mutableMapOf<BasicBlock, AnalysisData>()

        fun BasicBlock.evaluateLocal() {
            if (this in analysisResult) {
                return
            }
            val resultIn = incomingEdges.map {
                if (it.from !in analysisResult) {
                    it.from.evaluateLocal()
                }
                analysisResult[it.from]!!.resultOut
            }.reduceOrNull { acc, next -> acc.intersect(next) } ?: setOf()

            val gen = mutableSetOf<IrValueSymbol>()
            val kill = mutableSetOf<IrValueSymbol>()
            for (statement in statements) {
                when (statement) {
                    is IrCall -> {
                        val argument = statement.getArgumentIfCallsBoxFunction()
                        if (argument?.isPrimitive() == true) {
                            gen += argument
                        }
                    }

                    is IrSetVariable -> {
                        val argument = statement.symbol
                        if (argument.isPrimitive()) {
                            kill += argument
                        }
                    }
                }
            }
            val resultOut = resultIn.toMutableSet()
            resultOut += gen
            resultOut -= kill
            analysisResult[this] = AnalysisData(resultIn, resultOut)
            for (basicBlock in outgoingEdges.map { it.to }) {
                basicBlock.evaluateLocal()
            }
        }
        functionEntry.evaluateLocal()
        return analysisResult
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
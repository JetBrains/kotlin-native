package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.getBoxFunction
import org.jetbrains.kotlin.fir.resolve.dfa.stackOf
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.defaultOrNullableType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.filterValues
import kotlin.collections.forEach
import kotlin.collections.intersect
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.plus
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.toMutableList

internal class BoxHoistingLowering(val context: Context) : FunctionLoweringPass {

    override fun lower(irFunction: IrFunction) {
        val hoistingVisitor = BoxHoistingVisitor(context)
        irFunction.accept(hoistingVisitor, data = null)
        val immutableVariablesBoxOccurrences = hoistingVisitor.immutableVariablesBoxOccurrences
        val blocksInfo = hoistingVisitor.blocksInfo
        irFunction.transform(BoxHoistingTransformer(context, immutableVariablesBoxOccurrences, blocksInfo), data = null)
    }
}

internal class BoxHoistingTransformer(val context: Context,
                                     val immutableVariablesBoxOccurrences: MutableMap<IrValueSymbol, Int>,
                                     val blocksInfo: MutableMap<IrElement, MutableSet<IrValueSymbol>>
) : IrElementTransformerVoid() {

    val functions = stackOf<IrFunction>()

    val vars = mutableSetOf<IrValueSymbol>()
    val originalToBoxedVariablesMap = mutableMapOf<IrValueSymbol, IrValueDeclaration>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        functions.push(declaration)

        vars += blocksInfo[declaration]!!.intersect(immutableVariablesBoxOccurrences.filterValues { it > 1 }.keys)

        val statements = declaration.body?.statements?.toMutableList()
        if (statements != null) {
            originalToBoxedVariablesMap += addBoxedVariables(statements, declaration.valueParameters)
            declaration.body = context.createIrBuilder(declaration.symbol).irBlockBody(declaration) {
                statements.forEach {
                    +it
                }
            }
        }
        return super.visitFunction(declaration).also {
            functions.pop()
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (inBoxedVariable) {
            return super.visitCall(expression)
        }

        fun IrCall.isBoxFunctionCall() = symbol.owner.name.asString().endsWith("-box>")
        fun IrCall.getArgumentIfCallsBoxFunction(): IrValueSymbol? {
            if (this.isBoxFunctionCall()) {
                when (val argument = getValueArgument(0)!!) {
                    is IrGetValue -> return argument.symbol
                }
            }
            return null
        }

        expression.getArgumentIfCallsBoxFunction()?.let { valueSymbol ->
            if (valueSymbol in originalToBoxedVariablesMap) {
//                println("haHAA ${valueSymbol.owner.name} ${valueSymbol.owner.file.name}")
                return context.createIrBuilder(valueSymbol).irGet(originalToBoxedVariablesMap[valueSymbol]!!)
            }
        }
        return super.visitCall(expression)
    }

    private var inBoxedVariable = false

    override fun visitVariable(declaration: IrVariable): IrStatement {
        if (declaration.name.asString().endsWith("-Boxed")) {
            inBoxedVariable = true
            return super.visitVariable(declaration).also {
                inBoxedVariable = false
            }
        } else {
            return super.visitVariable(declaration)
        }
    }

    private fun addBoxedVariables(statements: MutableList<IrStatement>, valueParameters: MutableList<IrValueParameter> = mutableListOf()): Map<IrValueSymbol, IrValueDeclaration> {
        val newDeclarations = mutableMapOf<IrValueSymbol, IrValueDeclaration>()

        fun createBoxedVariable(statement: IrValueDeclaration) {
            val boxFunction = context.getBoxFunction(statement.type.getClass()!!)
            val descriptor = WrappedVariableDescriptor()
            val declaration = IrVariableImpl(
                    statement.startOffset,
                    statement.endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                    IrVariableSymbolImpl(descriptor),
                    Name.identifier("${statement.name}-Boxed"),
                    context.ir.symbols.any.owner.defaultOrNullableType(statement.type.isNullable()),
                    if (statement is IrVariable) statement.isVar else true,
                    if (statement is IrVariable) statement.isConst else false,
                    if (statement is IrVariable) statement.isLateinit else false
            ).apply {
                initializer = context.createIrBuilder(statement.symbol).run {
                    (irCall(boxFunction) as IrCall).apply {
                        putValueArgument(0, irGet(statement))
                    }
                }
                descriptor.bind(this)
                parent = statement.parent
            }
            newDeclarations[statement.symbol] = declaration
        }

        for (statement in statements) {
            if (statement is IrValueDeclaration && statement.symbol in vars) {
                createBoxedVariable(statement)
            }
        }

        for (valueParameter in valueParameters) {
            if (valueParameter.symbol in vars) {
                createBoxedVariable(valueParameter)
            }
        }

        newDeclarations.forEach { (originalVariable, newVariable) ->
            if (originalVariable.owner is IrVariable) {
                val indexOfStatement = statements.indexOf(originalVariable.owner)
                statements.add(indexOfStatement + 1, newVariable)
            } else {
                statements.add(0, newVariable)
            }
        }

        return newDeclarations
    }
}

internal class BoxHoistingVisitor(val context: Context) : IrElementTransformerVoid() {
    val immutableVariablesBoxOccurrences = mutableMapOf<IrValueSymbol, Int>()
    val blocksInfo = mutableMapOf<IrElement, MutableSet<IrValueSymbol>>()
    private val currentBlocks = stackOf<IrElement>()
    private val currentBlock: IrElement
        get() = currentBlocks.top()
    val bb = mutableMapOf<IrElement, MutableMap<IrValueSymbol, MutableList<IrCall>>>()

    override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        bb[currentBlock]!![declaration.symbol] = mutableListOf()
        return super.visitValueParameter(declaration)
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        bb[currentBlock]!![declaration.symbol] = mutableListOf()
        return super.visitVariable(declaration)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentBlocks.push(declaration)
        blocksInfo[currentBlock] = mutableSetOf()
        bb[currentBlock] = mutableMapOf()
        return super.visitFunction(declaration).also {
            currentBlocks.pop()
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        expression.branches.forEach {
            it.condition.accept(this, data = null)
            currentBlocks.push(it.result)
            blocksInfo[currentBlock] = mutableSetOf()
            bb[currentBlock] = mutableMapOf()
            it.result.accept(this, data = null)
            currentBlocks.pop()
        }
        if (expression.branches.size > 1) {
            var joint = blocksInfo[expression.branches[0].result]!!.intersect(blocksInfo[expression.branches[1].result]!!)
            for (i in 2 until expression.branches.size) {
                joint = joint.intersect(blocksInfo[expression.branches[i].result]!!)
            }
            blocksInfo[currentBlock]!! += joint

            var joint2 = bb[expression.branches[0].result]!!.intersect(bb[expression.branches[1].result]!!)
            for (i in 2 until expression.branches.size) {
                joint2 = joint2.intersect(bb[expression.branches[i].result]!!)
            }
            bb[currentBlock]!! += joint2
        }
        return expression
    }

    private fun MutableMap<IrValueSymbol, MutableList<IrCall>>.intersect(other: MutableMap<IrValueSymbol, MutableList<IrCall>>): MutableMap<IrValueSymbol, MutableList<IrCall>> {
        val result = mutableMapOf<IrValueSymbol, MutableList<IrCall>>()
        val commonVars = keys.intersect(other.keys)
        for (commonVar in commonVars) {
            result[commonVar] = (this[commonVar]!! + other[commonVar]!!).toMutableList()
        }
        return result
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val probablyBoxedArgument = expression.getArgumentIfCallsBoxFunction()
        probablyBoxedArgument?.let {
            if (it.isImmutablePrimitive()) {
                immutableVariablesBoxOccurrences.compute(probablyBoxedArgument) { _, count -> count?.plus(1) ?: 1 }
                blocksInfo[currentBlock]!!.add(it)
                bb[currentBlock]!!.let { map ->
                    if (it !in map) {
                        map[it] = mutableListOf()
                    }
                    map[it]!! += expression
                }
            }
        }
        return super.visitCall(expression)
    }

    private fun IrValueSymbol.isImmutablePrimitive() = when (this) {
        is IrValueParameterSymbol -> owner.type.isPrimitiveType()
        is IrVariableSymbol -> !owner.isVar && owner.type.isPrimitiveType()
        else -> throw AssertionError("Unknown implementation of IrValueSymbol: ${this::class.qualifiedName}")
    }

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
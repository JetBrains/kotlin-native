package org.jetbrains.kotlin.backend.konan.cfg

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.util.*

fun IrElement.buildCfg(): Pair<BasicBlock, BasicBlock> {
    val builder = CfgBuilder()
    acceptVoid(builder)
    return builder.getResult()
}

class CfgBuilder : IrElementVisitorVoid {
    private val entry = BasicBlock()
    private var current = entry

    fun getResult() = entry to current

    override fun visitElement(element: IrElement) {
        throw UnsupportedOperationException()
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        current.statements += declaration
    }

    override fun visitFunction(declaration: IrFunction) {
        functionExitSink = BasicBlock()
        declaration.body?.acceptVoid(this)
        // KNPE => local function exists
        if (current.outgoingEdges.isEmpty()) {
            current edgeTo functionExitSink!!
        }
        functionExitSink = null
    }

    override fun visitBlockBody(body: IrBlockBody) {
        body.statements.forEach {
            it.acceptVoid(this)
        }
    }

    override fun visitBlock(expression: IrBlock) {
        expression.statements.forEach {
            it.acceptVoid(this)
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.initializer?.acceptVoid(this)
        current.statements += declaration
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        expression.value.acceptVoid(this)
        current.statements += expression
    }

    override fun visitReturn(expression: IrReturn) {
        expression.value.acceptVoid(this)
        current.statements += expression
        functionExitSink?.apply { current edgeTo this }
    }

    override fun visitExpression(expression: IrExpression) {
        when (expression) {
            is IrWhen -> {
                val conditionEntriesAndExits = expression.branches.map { it.condition.buildCfg() }
                val resultEntriesAndExits = expression.branches.map { it.result.buildCfg() }
                assert(conditionEntriesAndExits.isNotEmpty())
                assert(conditionEntriesAndExits.size == resultEntriesAndExits.size)
                val firstCondition = conditionEntriesAndExits.first()
                val firstBlockAfterWhen = BasicBlock()
                current edgeTo firstCondition.first
                current = firstCondition.second
                (conditionEntriesAndExits.drop(1) + (firstBlockAfterWhen to firstBlockAfterWhen)).zip(resultEntriesAndExits).forEach { (conditionEntryEndExit, previousConditionResultEntryAndExit) ->
                    val resultEntry = previousConditionResultEntryAndExit.first
                    val resultExit = previousConditionResultEntryAndExit.second
                    current edgeTo resultEntry
                    current edgeTo conditionEntryEndExit.first
                    if (resultExit.outgoingEdges.isEmpty()) {
                        resultExit edgeTo conditionEntryEndExit.first
                    }
                    current = conditionEntryEndExit.second
                }
            }
            else -> current.statements += expression
        }
    }

    companion object {
        // Assumed there are no local functions at the moment of building CFG.
        private var functionExitSink: BasicBlock? = null
    }
}

private infix fun BasicBlock.edgeTo(to: BasicBlock) {
    addOutgoingTo(to)
    to.addIncomingFrom(this)
}

class BasicBlock(
        val statements: MutableList<IrStatement> = mutableListOf(),
        val incomingEdges: MutableList<Edge> = mutableListOf(),
        val outgoingEdges: MutableList<Edge> = mutableListOf()
) {
    fun addOutgoingTo(nextBlock: BasicBlock) {
        outgoingEdges += Edge(this, nextBlock)
    }

    fun addIncomingFrom(previousBlock: BasicBlock) {
        incomingEdges += Edge(previousBlock, this)
    }

    companion object {
        fun of(vararg statements: IrStatement): BasicBlock {
            val result = BasicBlock()
            statements.forEach { result.statements += it }
            return result
        }
    }
}

class Edge(val from: BasicBlock, val to: BasicBlock)

private fun <T> BasicBlock.traverseBfs(result: T, updateResult: (T, BasicBlock) -> Unit): T {
    val visited = mutableSetOf<BasicBlock>()
    val bfsOrder = ArrayDeque<BasicBlock>()
    bfsOrder.addLast(this)
    visited += this
    while (bfsOrder.isNotEmpty()) {
        val nextBlock = bfsOrder.pollFirst()!!
        updateResult(result, nextBlock)
        nextBlock.outgoingEdges.forEach {
            if (it.to !in visited) {
                bfsOrder.addLast(it.to)
                visited += it.to
            }
        }
    }
    return result
}

private fun BasicBlock.enumerate(): Map<BasicBlock, Int> {
    var num = 1
    return traverseBfs(mutableMapOf(), { result, basicBlock ->
        result[basicBlock] = num++
    })
}

fun BasicBlock.dumpCfg(): String {
    val enumeration = enumerate()
    fun BasicBlock.dump(): String =
            buildString {
                val num = enumeration[this@dump]!!
                appendln("==== ${this@dump.incomingEdges.joinToString { enumeration[it.from].toString() }} --> START BLOCK #$num ====")
                statements.forEach { append(it.dump()) }
                appendln("==== END BLOCK #$num --> ${this@dump.outgoingEdges.joinToString { enumeration[it.to].toString() }} ====")
            }
    val result = StringBuilder()
    return traverseBfs(result, { res, basicBlock ->
        res.append(basicBlock.dump())
    }).toString()
}
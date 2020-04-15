package org.jetbrains.kotlin.backend.konan.cfg

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrWhen
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
    val entry = BasicBlock()
    var current = entry

    fun getResult() = entry to current

    override fun visitElement(element: IrElement) {
        throw UnsupportedOperationException()
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        current.statements += declaration
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.body?.acceptVoid(this)
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

    override fun visitExpression(expression: IrExpression) {
        when (expression) {
            is IrWhen -> {
                val conditionBlocks = expression.branches.map { BasicBlock.of(it.condition) }
                val resultEntriesAndExits = expression.branches.map { it.result.buildCfg() }
                assert(conditionBlocks.isNotEmpty())
                assert(conditionBlocks.size == resultEntriesAndExits.size)
                val firstCondition = conditionBlocks.first()
                val firstBlockAfterWhen = BasicBlock()
                current edgeTo firstCondition
                current = firstCondition
                (conditionBlocks.drop(1) + firstBlockAfterWhen).zip(resultEntriesAndExits).forEach { (conditionBlock, previousConditionResultEntryAndExit) ->
                    val resultEntry = previousConditionResultEntryAndExit.first
                    val resultExit = previousConditionResultEntryAndExit.second
                    current edgeTo resultEntry
                    current edgeTo conditionBlock
                    resultExit edgeTo conditionBlock
                    current = conditionBlock
                }
            }
            else -> current.statements += expression
        }
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

fun BasicBlock.dumpCfg(): String {
    var num = 1
    fun BasicBlock.dump(): String =
            buildString {
                val n = num++
                appendln("==== START BLOCK #$n ====")
                statements.forEach { append(it.dump()) }
                appendln("==== END BLOCK #$n ====")
            }
    val visited = mutableSetOf<BasicBlock>()
    val bfsOrder = ArrayDeque<BasicBlock>()
    bfsOrder.addLast(this)
    visited += this
    return buildString {
        while (bfsOrder.isNotEmpty()) {
            val nextBlock = bfsOrder.pollFirst()!!
            append(nextBlock.dump())
            nextBlock.outgoingEdges.forEach {
                if (it.to !in visited) {
                    bfsOrder.addLast(it.to)
                    visited += it.to
                }
            }
        }
    }
}
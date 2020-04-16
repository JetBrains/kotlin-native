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

    override fun visitComposite(expression: IrComposite) {
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
        functionExitSink?.apply { current returnEdgeTo this }
    }

    override fun visitBreak(jump: IrBreak) {
        current breakEdgeTo labeledLoops[jump.label ?: ""]!!.second
    }

    override fun visitContinue(jump: IrContinue) {
        current continueEdgeTo labeledLoops[jump.label ?: ""]!!.first
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
                    if (resultExit.kind == EdgeKind.NORMAL) {
                        resultExit edgeTo conditionEntryEndExit.first
                    }
                    current = conditionEntryEndExit.second
                }
            }
            is IrDoWhileLoop -> {
                val fakeLoopBodyStart = BasicBlock()
                current edgeTo fakeLoopBodyStart
                current = fakeLoopBodyStart
                val firstBlockAfterLoop = BasicBlock()
                val label = expression.label ?: ""
                val outerLoopStartAndEnd = labeledLoops[label]
                labeledLoops[label] = fakeLoopBodyStart to firstBlockAfterLoop

                val bodyEntryAndExit = expression.body?.buildCfg() ?: BasicBlock().let { it to it }
                val conditionEntryAndExit = expression.condition.buildCfg()
                current edgeTo bodyEntryAndExit.first
                bodyEntryAndExit.second edgeTo conditionEntryAndExit.first
                conditionEntryAndExit.second edgeTo bodyEntryAndExit.first
                conditionEntryAndExit.second edgeTo firstBlockAfterLoop
                current = firstBlockAfterLoop

                outerLoopStartAndEnd?.let { labeledLoops[label] = it }
            }

            is IrWhileLoop -> {
                val conditionEntryAndExit = expression.condition.buildCfg()
                val firstBlockAfterLoop = BasicBlock()
                val label = expression.label ?: ""
                val outerLoopStartAndEnd = labeledLoops[label]
                labeledLoops[label] = conditionEntryAndExit.first to firstBlockAfterLoop

                val bodyEntryAndExit = expression.body?.buildCfg() ?: BasicBlock().let { it to it }
                current edgeTo conditionEntryAndExit.first
                conditionEntryAndExit.second edgeTo bodyEntryAndExit.first
                conditionEntryAndExit.second edgeTo firstBlockAfterLoop
                bodyEntryAndExit.second edgeTo conditionEntryAndExit.first
                current = firstBlockAfterLoop

                outerLoopStartAndEnd?.let { labeledLoops[label] = it }
            }
            else -> current.statements += expression
        }


    }

    override fun visitCall(expression: IrCall) {
        for (i in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(i)!!.acceptVoid(this)
        }
        current.statements += expression
    }

    companion object {
        // Assumed there are no local functions at the moment of building CFG.
        private var functionExitSink: BasicBlock? = null

        private val labeledLoops = mutableMapOf<String, Pair<BasicBlock, BasicBlock>>()
    }
}

private infix fun BasicBlock.edgeTo(to: BasicBlock) {
    edgeTo(this, to, EdgeKind.NORMAL)
}

private infix fun BasicBlock.returnEdgeTo(to: BasicBlock) {
    edgeTo(this, to, EdgeKind.NORMAL_RETURN)
}

private infix fun BasicBlock.breakEdgeTo(to: BasicBlock) {
    edgeTo(this, to, EdgeKind.BREAK)
}

private infix fun BasicBlock.continueEdgeTo(to: BasicBlock) {
    edgeTo(this, to, EdgeKind.CONTINUE)
}

private fun edgeTo(from: BasicBlock, to: BasicBlock, kind: EdgeKind) {
    from.addOutgoingTo(to, kind)
    to.addIncomingFrom(from, kind)
}

class BasicBlock(
        val statements: MutableList<IrStatement> = mutableListOf(),
        val incomingEdges: MutableList<Edge> = mutableListOf(),
        val outgoingEdges: MutableList<Edge> = mutableListOf()
) {

    val kind: EdgeKind
        get() = when {
            outgoingEdges.size == 1 -> outgoingEdges.single().kind
            else -> EdgeKind.NORMAL
        }

    fun addOutgoingTo(nextBlock: BasicBlock, kind: EdgeKind = EdgeKind.NORMAL) {
        outgoingEdges += Edge(this, nextBlock, kind)
    }

    fun addIncomingFrom(previousBlock: BasicBlock, kind: EdgeKind = EdgeKind.NORMAL) {
        incomingEdges += Edge(previousBlock, this, kind)
    }

    companion object {
        fun of(vararg statements: IrStatement): BasicBlock {
            val result = BasicBlock()
            statements.forEach { result.statements += it }
            return result
        }
    }
}

enum class EdgeKind {
    NORMAL,
    NORMAL_RETURN,
    BREAK,
    CONTINUE
}

class Edge(val from: BasicBlock, val to: BasicBlock, val kind: EdgeKind = EdgeKind.NORMAL)

fun <T> BasicBlock.traverseBfs(result: T, updateResult: (T, BasicBlock) -> Unit): T {
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

fun BasicBlock.enumerate(): Map<BasicBlock, Int> {
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
                appendln("==== ${this@dump.incomingEdges.joinToString { enumeration[it.from].toString() + "[${it.kind.name}]" }} --> START BLOCK #$num ====")
                statements.forEach { append(it.dump()) }
                appendln("==== END BLOCK #$num --> ${this@dump.outgoingEdges.joinToString { enumeration[it.to].toString() + "[${it.kind.name}]" }} ====")
            }
    val result = StringBuilder()
    return traverseBfs(result, { res, basicBlock ->
        res.append(basicBlock.dump())
    }).toString()
}

// Mutates and returns itself.
fun BasicBlock.decomposed(): BasicBlock {
    // 1, 2 --> [a, b, c] --> 3, 4 ====> 1, 2 --> [a] -> [b] -> [c] -> 3, 4
    fun BasicBlock.atomDecompose(): BasicBlock {
        if (statements.size <= 1) {
            return this
        }
        val oldOutgoingEdges = mutableListOf<Edge>()
        oldOutgoingEdges += outgoingEdges
        outgoingEdges.clear()
        var lastBlock = this
        for (i in 1 until statements.size) {
            val newBlock = BasicBlock.of(statements[i])
            lastBlock edgeTo newBlock
            lastBlock = newBlock
        }
        statements.retainAll(statements.take(1))
        val newOutgoingEdges = oldOutgoingEdges.map { Edge(lastBlock, it.to, it.kind) }
        lastBlock.outgoingEdges += newOutgoingEdges
        oldOutgoingEdges.forEachIndexed { index, oldOutgoingEdge ->
            oldOutgoingEdge.to.apply {
                incomingEdges.remove(oldOutgoingEdge)
                incomingEdges += newOutgoingEdges[index]
            }
        }
        return this
    }
    traverseBfs(this, { _, bb -> bb.atomDecompose() })
    return this
}
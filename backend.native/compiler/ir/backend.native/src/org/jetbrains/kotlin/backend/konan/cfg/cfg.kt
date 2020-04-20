package org.jetbrains.kotlin.backend.konan.cfg

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.util.*

fun IrElement.buildCfg(filter: CfgElementsFilter): Pair<BasicBlock, BasicBlock> {
    val builder = CfgBuilder(filter)
    acceptVoid(builder)
    return builder.getResult()
}

class CfgBuilder(private val filter: CfgElementsFilter) : IrElementVisitorVoid {
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

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        expression.argument.acceptVoid(this)
        if (!filter.filter(expression)) {
            current.statements += expression
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.initializer?.acceptVoid(this)
        if (!filter.filter(declaration)) {
            current.statements += declaration
        }
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        expression.value.acceptVoid(this)
        if (!filter.filter(expression)) {
            current.statements += expression
        }
    }

    override fun visitSetField(expression: IrSetField) {
        expression.value.acceptVoid(this)
        if (!filter.filter(expression)) {
            current.statements += expression
        }
    }

    override fun visitReturn(expression: IrReturn) {
        expression.value.acceptVoid(this)
        if (!filter.filter(expression)) {
            current.statements += expression
        }
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
                val conditionEntriesAndExits = expression.branches.map { it.condition.buildCfg(filter) }
                val resultEntriesAndExits = expression.branches.map { it.result.buildCfg(filter) }
                assert(conditionEntriesAndExits.isNotEmpty())
                val endOfWhen = BasicBlock()
                var lastConditionIsElse = false
                conditionEntriesAndExits.zip(resultEntriesAndExits).forEachIndexed { index, (conditionEntryEndExit, previousConditionResultEntryAndExit) ->
                    val (conditionEntry, conditionExit) = conditionEntryEndExit
                    val (resultEntry, resultExit) = previousConditionResultEntryAndExit
                    // Check if current condition is a replacement for `else`
                    if (index == conditionEntriesAndExits.lastIndex && conditionEntry === conditionExit && conditionEntry.isTrueConst()) {
                        current edgeTo resultEntry
                        if (resultExit.kind == EdgeKind.NORMAL) {
                            resultExit edgeTo endOfWhen
                        }
                        lastConditionIsElse = true
                        return@forEachIndexed
                    }
                    // --> conditionEntry --> ... --> conditionExit --> resultEntry --> ... --> resultExit --> <end of when>
                    //                                              \
                    //                                               nextConditionEntry --> ... --> nextConditionExit --> ...
                    current edgeTo conditionEntry
                    current = conditionExit
                    conditionExit edgeTo resultEntry
                    if (resultExit.kind == EdgeKind.NORMAL) {
                        resultExit edgeTo endOfWhen
                    }
                }
                if (!lastConditionIsElse) {
                    current edgeTo endOfWhen
                }
                current = endOfWhen
            }
            is IrDoWhileLoop -> {
                val fakeLoopBodyStart = BasicBlock()
                current edgeTo fakeLoopBodyStart
                current = fakeLoopBodyStart
                val firstBlockAfterLoop = BasicBlock()
                val label = expression.label ?: ""
                val outerLoopStartAndEnd = labeledLoops[label]
                labeledLoops[label] = fakeLoopBodyStart to firstBlockAfterLoop

                val bodyEntryAndExit = expression.body?.buildCfg(filter) ?: BasicBlock().let { it to it }
                val conditionEntryAndExit = expression.condition.buildCfg(filter)
                current edgeTo bodyEntryAndExit.first
                bodyEntryAndExit.second edgeTo conditionEntryAndExit.first
                conditionEntryAndExit.second edgeTo bodyEntryAndExit.first
                conditionEntryAndExit.second edgeTo firstBlockAfterLoop
                current = firstBlockAfterLoop

                outerLoopStartAndEnd?.let { labeledLoops[label] = it }
            }

            is IrWhileLoop -> {
                val conditionEntryAndExit = expression.condition.buildCfg(filter)
                val firstBlockAfterLoop = BasicBlock()
                val label = expression.label ?: ""
                val outerLoopStartAndEnd = labeledLoops[label]
                labeledLoops[label] = conditionEntryAndExit.first to firstBlockAfterLoop

                val bodyEntryAndExit = expression.body?.buildCfg(filter) ?: BasicBlock().let { it to it }
                current edgeTo conditionEntryAndExit.first
                conditionEntryAndExit.second edgeTo bodyEntryAndExit.first
                conditionEntryAndExit.second edgeTo firstBlockAfterLoop
                bodyEntryAndExit.second edgeTo conditionEntryAndExit.first
                current = firstBlockAfterLoop

                outerLoopStartAndEnd?.let { labeledLoops[label] = it }
            }
            else -> if (!filter.filter(expression)) {
                current.statements += expression
            }
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        expression.dispatchReceiver?.acceptVoid(this)
        expression.extensionReceiver?.acceptVoid(this)
        for (i in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(i)!!.acceptVoid(this)
        }
        if (!filter.filter(expression)) {
            current.statements += expression
        }
    }

    companion object {
        // Assumed there are no local functions at the moment of building CFG.
        private var functionExitSink: BasicBlock? = null

        private val labeledLoops = mutableMapOf<String, Pair<BasicBlock, BasicBlock>>()

        private fun BasicBlock.isTrueConst(): Boolean {
            return statements.size == 1 && statements.single().let { it is IrConst<*> && it.isTrueConst() }
        }
    }
}

interface CfgElementsFilter {
    fun filter(element: IrElement): Boolean
}

class CfgServiceFunctionsFilter(private val serviceFunctions: List<IrFunctionSymbol>) : CfgElementsFilter {
    override fun filter(element: IrElement): Boolean = element is IrCall && element.symbol in serviceFunctions
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
        Edge(this, nextBlock, kind).also {
            outgoingEdges += it
            nextBlock.incomingEdges += it
        }
    }

    fun addIncomingFrom(previousBlock: BasicBlock, kind: EdgeKind = EdgeKind.NORMAL) {
        Edge(previousBlock, this, kind).also {
            incomingEdges += it
            previousBlock.outgoingEdges += it
        }
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
    var num = 0
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

inline fun <S, T : S> Collection<T>.reduceOrNull(
        operation: (S, T) -> S
): S? {
    if (isEmpty()) return null
    return reduce(operation)
}
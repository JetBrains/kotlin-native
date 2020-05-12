package org.jetbrains.kotlin.backend.konan.cfg.analysis

import org.jetbrains.kotlin.backend.konan.cfg.BasicBlock
import org.jetbrains.kotlin.backend.konan.cfg.traverseBfs
import java.util.*

interface IntraproceduralCfgAnalyzer<T : AnalysisResult<T>> {
    fun initialFor(basicBlock: BasicBlock): T
    fun evaluate(basicBlock: BasicBlock, currentAnalysisResult: Map<BasicBlock, T>): T
    fun analyze(entry: BasicBlock): Map<BasicBlock, T>
}

abstract class WorkListIntraproceduralCfgAnalyzer<T : AnalysisResult<T>>(private val workList: AbstractWorkList) : IntraproceduralCfgAnalyzer<T> {

    override fun analyze(entry: BasicBlock): Map<BasicBlock, T> {
        val overallResult = mutableMapOf<BasicBlock, T>()
        val cfgAsCollection = convertCfgToCollection(entry)
        workList += cfgAsCollection
        cfgAsCollection.forEach { overallResult[it] = initialFor(it) }
        while (workList.isNotEmpty()) {
            val nextBasicBlock = workList.pollNext()
            val newResult = evaluate(nextBasicBlock, overallResult)
            if (newResult.differsFrom(overallResult[nextBasicBlock]!!)) {
                workList += nextBasicBlock.outgoingEdges.map { it.to }
                overallResult[nextBasicBlock] = newResult
            }
        }
        return overallResult
    }

    private fun convertCfgToCollection(entry: BasicBlock): Collection<BasicBlock> {
        val result = mutableListOf<BasicBlock>()
        entry.traverseBfs(result) { res, nextBlock ->
            res += nextBlock
        }
        return result
    }
}

sealed class AbstractWorkList {
    abstract operator fun plusAssign(element: BasicBlock)
    operator fun plusAssign(elements: Collection<BasicBlock>) {
        elements.forEach(::plusAssign)
    }

    abstract fun isNotEmpty(): Boolean
    abstract fun pollNext(): BasicBlock
}

class LifoWorkList(val backedList: LinkedList<BasicBlock>) : AbstractWorkList() {
    override fun plusAssign(element: BasicBlock) {
        backedList += element
    }
    override fun isNotEmpty() = backedList.isNotEmpty()
    override fun pollNext() = backedList.pollFirst()!!
}

class ReversePostorderWorkList(val cfgEntry: BasicBlock) : AbstractWorkList() {
    private val order = ReversePostorder(cfgEntry)
    private val current = LinkedList<BasicBlock>()
    private val pending = sortedSetOf(order)

    override fun plusAssign(element: BasicBlock) {
        pending += element
    }

    override fun isNotEmpty(): Boolean {
        return current.isNotEmpty() || pending.isNotEmpty()
    }

    override fun pollNext(): BasicBlock {
        if (current.isEmpty()) {
            current += pending
            pending.clear()
        }
        return current.pollFirst()!!
    }

    private class ReversePostorder(entry: BasicBlock) : Comparator<BasicBlock> {
        private val postTimes = mutableMapOf<BasicBlock, Int>()

        init {
            entry.dfsAndCalculatePostTime()
        }

        override fun compare(bb1: BasicBlock, bb2: BasicBlock): Int {
            val postTime1 = postTimes[bb1]!!
            val postTime2 = postTimes[bb2]!!
            return postTime2.compareTo(postTime1)
        }

        private fun BasicBlock.dfsAndCalculatePostTime() {
            val visited = mutableSetOf<BasicBlock>()
            var time = 0
            fun dfs(basicBlock: BasicBlock) {
                visited += basicBlock
                for (outgoingEdge in basicBlock.outgoingEdges) {
                    if (outgoingEdge.to !in visited) {
                        dfs(outgoingEdge.to)
                    }
                }
                time++
                postTimes[basicBlock] = time
            }

            dfs(this)
        }
    }
}
interface AnalysisResult<T : AnalysisResult<T>> {
    fun bottom(): T
    fun differsFrom(other: T): Boolean
}
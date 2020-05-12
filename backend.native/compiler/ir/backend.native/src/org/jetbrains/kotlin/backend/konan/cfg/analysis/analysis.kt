package org.jetbrains.kotlin.backend.konan.cfg.analysis

import org.jetbrains.kotlin.backend.konan.cfg.BasicBlock
import org.jetbrains.kotlin.backend.konan.cfg.traverseBfs
import java.util.*

interface IntraproceduralCfgAnalyzer<T : AnalysisResult<T>> {
    fun initialFor(basicBlock: BasicBlock): T
    fun evaluate(basicBlock: BasicBlock, currentAnalysisResult: Map<BasicBlock, T>): T
    fun analyze(entry: BasicBlock): Map<BasicBlock, T>
}

interface WorklistIntraproceduralCfgAnalyzer<T : AnalysisResult<T>> : IntraproceduralCfgAnalyzer<T> {
    override fun analyze(entry: BasicBlock): Map<BasicBlock, T> {
        val overallResult = mutableMapOf<BasicBlock, T>()
        val workList = LinkedList<BasicBlock>()
        val cfgAsCollection = convertCfgToCollection(entry)
        workList += cfgAsCollection
        cfgAsCollection.forEach { overallResult[it] = initialFor(it) }
        while (workList.isNotEmpty()) {
            val nextBasicBlock = workList.pollFirst()!!
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

interface AnalysisResult<T : AnalysisResult<T>> {
    fun bottom(): T
    fun differsFrom(other: T): Boolean
}
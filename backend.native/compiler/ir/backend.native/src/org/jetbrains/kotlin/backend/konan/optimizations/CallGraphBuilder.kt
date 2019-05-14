/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.DirectedGraph
import org.jetbrains.kotlin.backend.konan.DirectedGraphNode
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.node

internal class CallGraphNode(val graph: CallGraph, val symbol: DataFlowIR.FunctionSymbol)
    : DirectedGraphNode<DataFlowIR.FunctionSymbol> {

    override val key get() = symbol

    override val directEdges: List<DataFlowIR.FunctionSymbol> by lazy {
        graph.directEdges[symbol]!!.callSites
                .map { it.actualCallee }
                .filter { graph.reversedEdges.containsKey(it) }
    }

    override val reversedEdges: List<DataFlowIR.FunctionSymbol> by lazy {
        graph.reversedEdges[symbol]!!
    }

    class CallSite(val call: DataFlowIR.Node.Call, val isVirtual: Boolean, val actualCallee: DataFlowIR.FunctionSymbol)

    val callSites = mutableListOf<CallSite>()
}

internal class CallGraph(val directEdges: Map<DataFlowIR.FunctionSymbol, CallGraphNode>,
                         val reversedEdges: Map<DataFlowIR.FunctionSymbol, MutableList<DataFlowIR.FunctionSymbol>>)
    : DirectedGraph<DataFlowIR.FunctionSymbol, CallGraphNode> {

    override val nodes get() = directEdges.values

    override fun get(key: DataFlowIR.FunctionSymbol) = directEdges[key]!!

    fun addEdge(caller: DataFlowIR.FunctionSymbol, callSite: CallGraphNode.CallSite) {
        directEdges[caller]!!.callSites += callSite
        reversedEdges[callSite.actualCallee]?.add(caller)
    }

}

internal class CallGraphBuilder(val context: Context,
                                val moduleDFG: ModuleDFG,
                                val externalModulesDFG: ExternalModulesDFG,
                                val devirtualizationAnalysisResult: Devirtualization.AnalysisResult?,
                                val gotoExternal: Boolean) {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val devirtualizedCallSites = devirtualizationAnalysisResult?.devirtualizedCallSites

    private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
        if (this is DataFlowIR.FunctionSymbol.External)
            return externalModulesDFG.publicFunctions[this.hash] ?: this
        return this
    }

    private val visitedFunctions = mutableSetOf<DataFlowIR.FunctionSymbol>()
    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol, CallGraphNode>()
    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol, MutableList<DataFlowIR.FunctionSymbol>>()
    private val callGraph = CallGraph(directEdges, reversedEdges)

    fun build(): CallGraph {
        val rootSet = Devirtualization.computeRootSet(context, moduleDFG, externalModulesDFG)
        //rootSet.forEach { println(it) }
        @Suppress("LoopToCallChain")
        for (symbol in rootSet) {
            if (!visitedFunctions.contains(symbol))
                dfs(symbol)
        }
        return callGraph
    }

    private fun addNode(symbol: DataFlowIR.FunctionSymbol) {
        if (directEdges.containsKey(symbol))
            return
        val node = CallGraphNode(callGraph, symbol)
        directEdges.put(symbol, node)
        val list = mutableListOf<DataFlowIR.FunctionSymbol>()
        reversedEdges.put(symbol, list)
    }

    private val symbols = context.ir.symbols
    private val arrayGet = symbols.arrayGet[symbols.array]!!.owner
    private val arraySet = symbols.arraySet[symbols.array]!!.owner

    private inline fun DataFlowIR.FunctionBody.forEachCallSite(block: (DataFlowIR.Node.Call) -> Unit) =
            nodes.forEach { node ->
                when (node) {
                    is DataFlowIR.Node.Call -> block(node)

                    is DataFlowIR.Node.Singleton ->
                        node.constructor?.let { block(DataFlowIR.Node.Call(it, emptyList(), node.type, null)) }

                    is DataFlowIR.Node.ArrayRead ->
                        block(DataFlowIR.Node.Call(
                                callee = moduleDFG.symbolTable.mapFunction(arrayGet),
                                arguments = listOf(node.array, node.index),
                                returnType = node.type,
                                irCallSite = null)
                        )

                    is DataFlowIR.Node.ArrayWrite ->
                        block(DataFlowIR.Node.Call(
                                callee = moduleDFG.symbolTable.mapFunction(arraySet),
                                arguments = listOf(node.array, node.index, node.value),
                                returnType = moduleDFG.symbolTable.mapType(context.irBuiltIns.unitType),
                                irCallSite = null)
                        )

                    is DataFlowIR.Node.FunctionReference ->
                        block(DataFlowIR.Node.Call(
                                callee = node.symbol,
                                arguments = emptyList(),
                                returnType = node.symbol.returnParameter.type,
                                irCallSite = null
                        ))
                }
            }

    private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
        visitedFunctions += symbol
        if (gotoExternal) {
            addNode(symbol)
//            if (symbol.name?.contains("accept") == true)
//                println("ZZZ")
            val function = moduleDFG.functions[symbol] ?: externalModulesDFG.functionDFGs[symbol] ?: return // TODO: throw if external.
            val body = function.body
            body.forEachCallSite { call ->
                val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites?.get(it) }
                if (devirtualizedCallSite == null) {
                    val callee = call.callee.resolved()
//                    if (call is DataFlowIR.Node.VirtualCall)
//                        println("BUGBUGBUG: caller = $symbol, callee = $callee")

//                    if (callee is DataFlowIR.FunctionSymbol.Declared
//                            && call !is DataFlowIR.Node.VirtualCall
//                            && !directEdges.containsKey(callee))
//                        dfs(callee)

                    if (call !is DataFlowIR.Node.VirtualCall) {
                        if (callee !is DataFlowIR.FunctionSymbol.Declared) {
                            //if (symbol.name?.contains("findKey") == true && !symbol.name.contains("hashCode"))
                             //   println("EXTERNAL: $symbol, $callee")
                            callGraph.addEdge(symbol, CallGraphNode.CallSite(call, call is DataFlowIR.Node.VirtualCall, callee))
                        }
                        else {
                            //if (symbol.name?.contains("findKey") == true && !symbol.name.contains("hashCode"))
                             //   println("NON-VIRTUAL: $symbol, $callee")
                            callGraph.addEdge(symbol, CallGraphNode.CallSite(call, call is DataFlowIR.Node.VirtualCall, callee))
                            if (!directEdges.containsKey(callee))
                                dfs(callee)
                        }
                    } else {
                        //if (symbol.name?.contains("findKey") == true && !symbol.name.contains("hashCode"))
                         //   println("VIRTUAL: $symbol, $callee")
                        val typeHierarcy = devirtualizationAnalysisResult?.typeHierarchy
                        val instantiatingClasses = devirtualizationAnalysisResult?.instantiatingClasses
                        if (typeHierarcy != null && instantiatingClasses != null && call.receiverType != DataFlowIR.Type.Virtual) {
                            //if (symbol.name?.contains("findKey") == true && !symbol.name.contains("hashCode"))
                              //  println("YEAH, BABY!: $symbol")
                            typeHierarcy.inheritorsOf(call.receiverType as DataFlowIR.Type.Declared /*TODO*/)
                                    .filterNot { it.isAbstract }
                                    //.filter { instantiatingClasses.contains(it) } // TODO: Нам жопа.
                                    .forEach { receiverType ->

                                        val actualCallee = when (call) {
                                            is DataFlowIR.Node.VtableCall -> {
                                                if (receiverType.vtable.size <= call.calleeVtableIndex) {
                                                    println("BUGBUGBUG: $callee")
                                                    println("    receiverType = ${call.receiverType}, inheritor = $receiverType")
                                                }
                                                receiverType.vtable[call.calleeVtableIndex]
                                            }

                                            is DataFlowIR.Node.ItableCall ->
                                                receiverType.itable[call.calleeHash]!!

                                            else -> error("Unreachable")
                                        }.resolved()
//                                            if (symbol.name?.contains("hash") == true && !symbol.name.contains("hashCode")) {
//                                                println("receiver = $receiverType, callee = $actualCallee")
//                                            }
                                        callGraph.addEdge(symbol, CallGraphNode.CallSite(call, false, actualCallee))
                                        if (actualCallee is DataFlowIR.FunctionSymbol.Declared
                                                && !directEdges.containsKey(actualCallee))
                                            dfs(actualCallee)
                                    }
                        }
                    }

                } else {
                    devirtualizedCallSite.possibleCallees.forEach {
                        val callee = it.callee.resolved()
                        callGraph.addEdge(symbol, CallGraphNode.CallSite(call, false, callee))
                        if (callee is DataFlowIR.FunctionSymbol.Declared
                                && !directEdges.containsKey(callee))
                            dfs(callee)
                    }
                }
            }
            body.nodes.filterIsInstance<DataFlowIR.Node.FunctionReference>()
                    .forEach {
                        val callee = it.symbol.resolved()
                        if (callee is DataFlowIR.FunctionSymbol.Declared
                                && !directEdges.containsKey(callee))
                            dfs(callee)
                    }
        } else {
            var function = moduleDFG.functions[symbol]
            var local = true
            if (function != null)
                addNode(symbol)
            else {
                function = externalModulesDFG.functionDFGs[symbol]!!
                local = false
            }
            val body = function.body
            body.forEachCallSite { call ->
                val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites?.get(it) }
                if (devirtualizedCallSite == null) {
                    val callee = call.callee.resolved()
                    if (moduleDFG.functions.containsKey(callee))
                        addNode(callee)
                    if (local)
                        callGraph.addEdge(symbol, CallGraphNode.CallSite(call, call is DataFlowIR.Node.VirtualCall, callee))
                    if (callee is DataFlowIR.FunctionSymbol.Declared
                            && call !is DataFlowIR.Node.VirtualCall
                            && !visitedFunctions.contains(callee))
                        dfs(callee)
                } else {
                    devirtualizedCallSite.possibleCallees.forEach {
                        val callee = it.callee.resolved()
                        if (moduleDFG.functions.containsKey(callee))
                            addNode(callee)
                        if (local)
                            callGraph.addEdge(symbol, CallGraphNode.CallSite(call, false, callee))
                        if (callee is DataFlowIR.FunctionSymbol.Declared
                                && !visitedFunctions.contains(callee))
                            dfs(callee)
                    }
                }
            }
            body.nodes.filterIsInstance<DataFlowIR.Node.FunctionReference>()
                    .forEach {
                        val callee = it.symbol.resolved()
                        if (moduleDFG.functions.containsKey(callee))
                            addNode(callee)
                        if (callee is DataFlowIR.FunctionSymbol.Declared
                                && !visitedFunctions.contains(callee))
                            dfs(callee)
                    }

        }
    }
}
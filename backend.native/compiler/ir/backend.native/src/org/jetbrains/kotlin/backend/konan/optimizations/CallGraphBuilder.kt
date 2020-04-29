/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.DirectedGraph
import org.jetbrains.kotlin.backend.konan.DirectedGraphNode
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.ir.util.parentAsClass

internal class CallGraphNode(val graph: CallGraph, val symbol: DataFlowIR.FunctionSymbol.Declared)
    : DirectedGraphNode<DataFlowIR.FunctionSymbol.Declared> {

    override val key get() = symbol

    override val directEdges: List<DataFlowIR.FunctionSymbol.Declared> by lazy {
        graph.directEdges[symbol]!!.callSites
                .map { it.actualCallee }
                .filterIsInstance<DataFlowIR.FunctionSymbol.Declared>()
                .filter { graph.directEdges.containsKey(it) }
    }

    override val reversedEdges: List<DataFlowIR.FunctionSymbol.Declared> by lazy {
        graph.reversedEdges[symbol]!!
    }

    class CallSite(val call: DataFlowIR.Node.Call, val isVirtual: Boolean, val actualCallee: DataFlowIR.FunctionSymbol)

    val callSites = mutableListOf<CallSite>()
}

internal class CallGraph(val directEdges: Map<DataFlowIR.FunctionSymbol.Declared, CallGraphNode>,
                         val reversedEdges: Map<DataFlowIR.FunctionSymbol.Declared, MutableList<DataFlowIR.FunctionSymbol.Declared>>,
                         val rootExternalFunctions: List<DataFlowIR.FunctionSymbol>)
    : DirectedGraph<DataFlowIR.FunctionSymbol.Declared, CallGraphNode> {

    override val nodes get() = directEdges.values

    override fun get(key: DataFlowIR.FunctionSymbol.Declared) = directEdges[key]!!

    fun addEdge(caller: DataFlowIR.FunctionSymbol.Declared, callSite: CallGraphNode.CallSite, addReversedEdge: Boolean) {
        directEdges[caller]!!.callSites += callSite
        if (addReversedEdge)
            reversedEdges[callSite.actualCallee as DataFlowIR.FunctionSymbol.Declared]!!.add(caller)
    }
}

internal class CallGraphBuilder(val context: Context,
                                val moduleDFG: ModuleDFG,
                                val externalModulesDFG: ExternalModulesDFG,
                                val devirtualizationAnalysisResult: Devirtualization.AnalysisResult) {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val devirtualizedCallSites = devirtualizationAnalysisResult.devirtualizedCallSites

    private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
        if (this is DataFlowIR.FunctionSymbol.External)
            return externalModulesDFG.publicFunctions[this.hash] ?: this
        return this
    }

    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, CallGraphNode>()
    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, MutableList<DataFlowIR.FunctionSymbol.Declared>>()
    private val externalRootFunctions = mutableListOf<DataFlowIR.FunctionSymbol>()
    private val callGraph = CallGraph(directEdges, reversedEdges, externalRootFunctions)
    private val stack = mutableListOf<Pair<DataFlowIR.FunctionSymbol.Declared, DataFlowIR.Function>>()

    fun build(): CallGraph {
        val rootSet = Devirtualization.computeRootSet(context, moduleDFG, externalModulesDFG)
        for (symbol in rootSet) {
            val function = moduleDFG.functions[symbol]
            if (function == null)
                externalRootFunctions.add(symbol)
            else {
                symbol as DataFlowIR.FunctionSymbol.Declared
                addNode(symbol)
                stack.push(Pair(symbol, function))
            }
        }
        while (stack.isNotEmpty()) {
            val (symbol, function) = stack.pop()
            handleFunction(symbol, function)
        }

        DEBUG_OUTPUT(1) {
            println("DirectEdges: ${directEdges.size}")
            println("ReversedEdges: ${reversedEdges.size}")
            println("Sum: ${directEdges.values.sumBy { it.callSites.size }}")
        }

        return callGraph
    }

    private fun addNode(symbol: DataFlowIR.FunctionSymbol.Declared) {
        val node = CallGraphNode(callGraph, symbol)
        directEdges[symbol] = node
        val list = mutableListOf<DataFlowIR.FunctionSymbol.Declared>()
        reversedEdges[symbol] = list
    }

    private val symbols = context.ir.symbols

    private inline fun DataFlowIR.FunctionBody.forEachCallSite(block: (DataFlowIR.Node.Call) -> Unit): Unit =
            nodes.forEach { node ->
                when (node) {
                    // TODO: OBJC-CONSTRUCTOR-CALL
                    is DataFlowIR.Node.NewObject -> {
                        block(node)
                        if (node.irCallSite?.symbol?.owner?.parentAsClass?.isObjCClass() == true) {
                            block(DataFlowIR.Node.Call(
                                    callee = moduleDFG.symbolTable.mapFunction(symbols.interopAllocObjCObject.owner),
                                    arguments = listOf(DataFlowIR.Edge(DataFlowIR.Node.Null, null)),
                                    returnType = moduleDFG.symbolTable.mapType(symbols.interopAllocObjCObject.owner.returnType),
                                    irCallSite = null)
                            )
                            block(DataFlowIR.Node.Call(
                                    callee = moduleDFG.symbolTable.mapFunction(symbols.interopInterpretObjCPointer.owner),
                                    arguments = listOf(DataFlowIR.Edge(DataFlowIR.Node.Null, null)),
                                    returnType = moduleDFG.symbolTable.mapType(symbols.interopInterpretObjCPointer.owner.returnType),
                                    irCallSite = null)
                            )
                            block(DataFlowIR.Node.Call(
                                    callee = moduleDFG.symbolTable.mapFunction(symbols.interopObjCRelease.owner),
                                    arguments = listOf(DataFlowIR.Edge(DataFlowIR.Node.Null, null)),
                                    returnType = moduleDFG.symbolTable.mapType(symbols.interopObjCRelease.owner.returnType),
                                    irCallSite = null)
                            )
                        }
                    }

                    is DataFlowIR.Node.Call -> block(node)

                    is DataFlowIR.Node.Singleton ->
                        node.constructor?.let { block(DataFlowIR.Node.Call(it, emptyList(), node.type, null)) }

                    is DataFlowIR.Node.ArrayRead ->
                        block(DataFlowIR.Node.Call(
                                callee = node.callee,
                                arguments = listOf(node.array, node.index),
                                returnType = node.type,
                                irCallSite = null)
                        )

                    is DataFlowIR.Node.ArrayWrite ->
                        block(DataFlowIR.Node.Call(
                                callee = node.callee,
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

    private fun staticCall(caller: DataFlowIR.FunctionSymbol.Declared, call: DataFlowIR.Node.Call, callee: DataFlowIR.FunctionSymbol) {
        val resolvedCallee = callee.resolved()
        val callSite = CallGraphNode.CallSite(call, false, resolvedCallee)
        val function = moduleDFG.functions[resolvedCallee]
        if (function == null)
            callGraph.addEdge(caller, callSite, /* addReversedEdge = */ false)
        else {
            resolvedCallee as DataFlowIR.FunctionSymbol.Declared
            val goToCallee = !directEdges.containsKey(resolvedCallee)
            if (goToCallee)
                addNode(resolvedCallee)
            callGraph.addEdge(caller, callSite, /* addReversedEdge = */ true)
            if (goToCallee)
                stack.push(Pair(resolvedCallee, function))
        }
    }

    private fun handleFunction(
            symbol: DataFlowIR.FunctionSymbol.Declared,
            function: DataFlowIR.Function
    ) = function.body.forEachCallSite { call ->
            val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites[it] }
            when {
                call !is DataFlowIR.Node.VirtualCall -> staticCall(symbol, call, call.callee)

                devirtualizedCallSite != null -> {
                    devirtualizedCallSite.possibleCallees.forEach {
                        staticCall(symbol, call, it.callee)
                    }
                }

                call.receiverType == DataFlowIR.Type.Virtual -> {
                    // Skip callsite. This can only be for invocations Any's methods on instances of ObjC classes.
                }

                else -> {
                    // Callsite has not been devirtualized - conservatively assume the worst:
                    // any inheritor of the receiver type is possible here.
                    val typeHierarchy = devirtualizationAnalysisResult.typeHierarchy
                    typeHierarchy.inheritorsOf(call.receiverType as DataFlowIR.Type.Declared).forEachBit {
                        val receiverType = typeHierarchy.allTypes[it]
                        if (receiverType.isAbstract) return@forEachBit
                        // TODO: Unconservative way - when we can use it?
                        //.filter { devirtualizationAnalysisResult.instantiatingClasses.contains(it) }
                        val actualCallee = when (call) {
                            is DataFlowIR.Node.VtableCall ->
                                receiverType.vtable[call.calleeVtableIndex]

                            is DataFlowIR.Node.ItableCall ->
                                receiverType.itable[call.calleeHash]!!

                            else -> error("Unreachable")
                        }
                        staticCall(symbol, call, actualCallee)
                    }
                }
            }
    }
}
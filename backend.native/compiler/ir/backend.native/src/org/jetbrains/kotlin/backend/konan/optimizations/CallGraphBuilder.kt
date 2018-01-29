/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.DirectedGraph
import org.jetbrains.kotlin.backend.konan.DirectedGraphNode
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.llvm.findMainEntryPoint
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

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

//internal interface DFGSymbolsResolver {
//    fun resolve(type: DataFlowIR.Type): DataFlowIR.Type.Declared
//    fun resolve(functionSymbol: DataFlowIR.FunctionSymbol): DataFlowIR.FunctionSymbol
//}
//
//internal class TypeHierarchy(private val resolver: DFGSymbolsResolver, types: List<DataFlowIR.Type.Declared>) {
//    private val typesSubTypes = mutableMapOf<DataFlowIR.Type.Declared, MutableList<DataFlowIR.Type.Declared>>()
//
//    init {
//        val visited = mutableSetOf<DataFlowIR.Type.Declared>()
//
//        fun processType(type: DataFlowIR.Type.Declared) {
//            if (type == DataFlowIR.Type.Virtual) return
//            if (!visited.add(type)) return
//            type.superTypes
//                    .map { resolver.resolve(it) }
//                    .forEach { superType ->
//                        val subTypes = typesSubTypes.getOrPut(superType, { mutableListOf() })
//                        subTypes += type
//                        processType(superType)
//                    }
//        }
//
//        types.forEach { processType(it) }
//    }
//
//    private fun findAllInheritors(type: DataFlowIR.Type.Declared, result: MutableSet<DataFlowIR.Type.Declared>) {
//        if (!result.add(type)) return
//        typesSubTypes[type]?.forEach { findAllInheritors(it, result) }
//    }
//
//    fun inheritorsOf(type: DataFlowIR.Type.Declared): List<DataFlowIR.Type.Declared> {
//        val result = mutableSetOf<DataFlowIR.Type.Declared>()
//        findAllInheritors(type, result)
//        return result.toList()
//    }
//}
//
//internal class RapidTypeAnalysis(private val functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
//                                 private val resolver: DFGSymbolsResolver,
//                                 private val typeHierarchy: TypeHierarchy) {
//    private val DEBUG = 0
//
//    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
//        if (DEBUG > severity) block()
//    }
//
//    private val visited = mutableSetOf<DataFlowIR.FunctionSymbol>()
//    private val typesVirtualCallSites = mutableMapOf<DataFlowIR.Type.Declared, MutableList<DataFlowIR.Node.VirtualCall>>()
//    private val instantiatingClasses = mutableSetOf<DataFlowIR.Type.Declared>()
//
//    fun run(roots: List<DataFlowIR.FunctionSymbol>): Set<DataFlowIR.Type.Declared> {
//        roots.forEach { dfs(it) }
//        return instantiatingClasses
//    }
//
//    private fun addInstantiatingClass(type: DataFlowIR.Type) {
//        val resolvedType = resolver.resolve(type)
//        if (!instantiatingClasses.add(resolvedType)) return
//        if (DEBUG > 0)
//            println("Adding instantiating class: $resolvedType")
//        checkSupertypes(resolvedType, resolvedType, mutableSetOf())
//    }
//
//    private fun processVirtualCall(virtualCall: DataFlowIR.Node.VirtualCall,
//                                   receiverType: DataFlowIR.Type.Declared) {
//        val callee = when (virtualCall) {
//            is DataFlowIR.Node.VtableCall ->
//                receiverType.vtable[virtualCall.calleeVtableIndex]
//
//            is DataFlowIR.Node.ItableCall -> {
//                if (receiverType.itable[virtualCall.calleeHash] == null) {
//                    println("BUGBUGBUG: $receiverType, HASH=${virtualCall.calleeHash}")
//                    receiverType.itable.forEach { hash, impl ->
//                        println("HASH: $hash, IMPL: $impl")
//                    }
//                }
//                receiverType.itable[virtualCall.calleeHash]!!
//            }
//
//            else -> error("Unreachable")
//        }
//        dfs(callee)
//    }
//
//    private fun checkSupertypes(type: DataFlowIR.Type.Declared,
//                                inheritor: DataFlowIR.Type.Declared,
//                                seenTypes: MutableSet<DataFlowIR.Type.Declared>) {
//        seenTypes += type
//        typesVirtualCallSites[type]?.toList()?.forEach { processVirtualCall(it, inheritor) }
//        typesVirtualCallSites[type]?.let { virtualCallSites ->
//            var index = 0
//            while (index < virtualCallSites.size) {
//                processVirtualCall(virtualCallSites[index], inheritor)
//                ++index
//            }
//        }
//        type.superTypes
//                .map { resolver.resolve(it) }
//                .filterNot { seenTypes.contains(it) }
//                .forEach { checkSupertypes(it, inheritor, seenTypes) }
//    }
//
//    private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
//        val resolvedFunctionSymbol = resolver.resolve(symbol)
//        if (resolvedFunctionSymbol is DataFlowIR.FunctionSymbol.External) return
//        if (!visited.add(resolvedFunctionSymbol)) return
//        if (DEBUG > 0)
//            println("Visiting $resolvedFunctionSymbol")
//        val function = functions[resolvedFunctionSymbol]!!
//        nodeLoop@for (node in function.body.nodes) {
//            when (node) {
//                is DataFlowIR.Node.NewObject -> {
//                    addInstantiatingClass(node.returnType)
//                    dfs(node.callee)
//                }
//
//                is DataFlowIR.Node.Singleton -> {
//                    addInstantiatingClass(node.type)
//                    node.constructor?.let { dfs(it) }
//                }
//
//                is DataFlowIR.Node.Const -> addInstantiatingClass(node.type)
//
//                is DataFlowIR.Node.StaticCall -> dfs(node.callee)
//
//                is DataFlowIR.Node.VirtualCall -> {
//                    if (node.receiverType == DataFlowIR.Type.Virtual)
//                        continue@nodeLoop
//                    val receiverType = resolver.resolve(node.receiverType)
//                    typeHierarchy.inheritorsOf(receiverType)
//                            .filter { instantiatingClasses.contains(it) }
//                            .forEach { processVirtualCall(node, it) }
//                    if (DEBUG > 0) {
//                        println("Adding virtual callsite:")
//                        println("    Receiver: $receiverType")
//                        println("    Callee: ${node.callee}")
//                        println("    Inheritors:")
//                        typeHierarchy.inheritorsOf(receiverType).forEach { println("        $it") }
//                    }
//                    typesVirtualCallSites.getOrPut(receiverType, { mutableListOf() }).add(node)
//                }
//            }
//        }
//    }
//}

internal class CallGraphBuilder(val context: Context,
                                val moduleDFG: ModuleDFG,
                                val externalModulesDFG: ExternalModulesDFG,
                                val devirtualizedCallSites: Map<DataFlowIR.Node.VirtualCall, Devirtualization.DevirtualizedCallSite>?) {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

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

    private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
        visitedFunctions += symbol
        var function = moduleDFG.functions[symbol]
        var local = true
        if (function != null)
            addNode(symbol)
        else {
            function = externalModulesDFG.functionDFGs[symbol]!!
            local = false
        }
        val body = function.body
        body.nodes.filterIsInstance<DataFlowIR.Node.Call>()
                .forEach { call ->
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
    }
}

//internal class CallGraphBuilder(val context: Context,
//                                val moduleDFG: ModuleDFG,
//                                val externalModulesDFG: ExternalModulesDFG,
//                                val devirtualizedCallSites: Map<DataFlowIR.Node.VirtualCall, Devirtualization.DevirtualizedCallSite>?) {
//
//    private val DEBUG = 0
//
//    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
//        if (DEBUG > severity) block()
//    }
//
//    private val hasMain = context.config.configuration.get(KonanConfigKeys.PRODUCE) == CompilerOutputKind.PROGRAM
//
//    private val symbolTable = moduleDFG.symbolTable
//
//    private fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
//        if (this is DataFlowIR.Type.Declared) return this
//        val hash = (this as DataFlowIR.Type.External).hash
//        return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $hash")
//    }
//
//    private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
//        if (this is DataFlowIR.FunctionSymbol.External)
//            return externalModulesDFG.publicFunctions[this.hash] ?: this
//        return this
//    }
//
//    private fun DataFlowIR.Type.Declared.isSubtypeOf(other: DataFlowIR.Type.Declared): Boolean {
//        return this == other || this.superTypes.any { it.resolved().isSubtypeOf(other) }
//    }
//
//    private val directEdges = mutableMapOf<DataFlowIR.FunctionSymbol, CallGraphNode>()
//    private val reversedEdges = mutableMapOf<DataFlowIR.FunctionSymbol, MutableList<DataFlowIR.FunctionSymbol>>()
//    private val callGraph = CallGraph(directEdges, reversedEdges)
//
//    fun build(): CallGraph {
//        val rootSet = if (hasMain) {
//            listOf(symbolTable.mapFunction(findMainEntryPoint(context)!!).resolved()) +
//                    moduleDFG.functions
//                            .map { it.key }
//                            .filter { it.isGlobalInitializer }
//
//        } else {
//            moduleDFG.functions.keys.filterIsInstance<DataFlowIR.FunctionSymbol.Public>()
//        }
//        @Suppress("LoopToCallChain")
//        for (symbol in rootSet) {
//            if (!directEdges.containsKey(symbol))
//                dfs(symbol)
//        }
//        return callGraph
//    }
//
//    private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
//        val node = CallGraphNode(callGraph, symbol)
//        directEdges.put(symbol, node)
//        val list = mutableListOf<DataFlowIR.FunctionSymbol>()
//        reversedEdges.put(symbol, list)
//        val function = moduleDFG.functions[symbol] ?: externalModulesDFG.functionDFGs[symbol]
//        val body = function!!.body
//        body.nodes.filterIsInstance<DataFlowIR.Node.Call>()
//                .forEach { call ->
//                    val devirtualizedCallSite = (call as? DataFlowIR.Node.VirtualCall)?.let { devirtualizedCallSites?.get(it) }
//                    if (devirtualizedCallSite == null) {
//                        val callee = call.callee.resolved()
//                        callGraph.addEdge(symbol, CallGraphNode.CallSite(call, call is DataFlowIR.Node.VirtualCall, callee))
//                        if (callee is DataFlowIR.FunctionSymbol.Declared
//                                && call !is DataFlowIR.Node.VirtualCall
//                                && !directEdges.containsKey(callee))
//                            dfs(callee)
//                    } else {
//                        devirtualizedCallSite.possibleCallees.forEach {
//                            val callee = it.callee.resolved()
//                            callGraph.addEdge(symbol, CallGraphNode.CallSite(call, false, callee))
//                            if (callee is DataFlowIR.FunctionSymbol.Declared
//                                    && !directEdges.containsKey(callee))
//                                dfs(callee)
//                        }
//                    }
//                }
//    }
//}
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

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.IrPrivateFunctionCallImpl
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.getValueArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.util.*

// TODO: Exceptions.

// Devirtualization analysis is performed using Variable Type Analysis algorithm.
// See http://web.cs.ucla.edu/~palsberg/tba/papers/sundaresan-et-al-oopsla00.pdf for details.
internal object Devirtualization {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val TAKE_NAMES = false // Take fqNames for all functions and types (for debug purposes).

    private inline fun takeName(block: () -> String) = if (TAKE_NAMES) block() else null

    fun computeRootSet(context: Context, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG)
            : List<DataFlowIR.FunctionSymbol> {

        fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        val hasMain = context.config.configuration.get(KonanConfigKeys.PRODUCE) == CompilerOutputKind.PROGRAM
        return if (hasMain) {
            listOf(moduleDFG.symbolTable.mapFunction(findMainEntryPoint(context)!!).resolved()) +
                    moduleDFG.functions.keys
                            .filter { it.isGlobalInitializer }
        } else {
            (moduleDFG.functions.keys.filterIsInstance<DataFlowIR.FunctionSymbol.Public>() +
                    moduleDFG.symbolTable.classMap.values
                            .filterIsInstance<DataFlowIR.Type.Declared>()
                            .flatMap { it.vtable + it.itable.values }
                            .filterIsInstance<DataFlowIR.FunctionSymbol.Declared>()
                            .filter { moduleDFG.functions.containsKey(it) }).distinct()
        }
    }

    fun BitSet.format(allTypes: List<DataFlowIR.Type.Declared>): String {
        return allTypes.withIndex().filter { this[it.index] }.joinToString { it.value.toString() }
    }

    private val VIRTUAL_TYPE_ID = 0 // Id of [DataFlowIR.Type.Virtual].

    private class DevirtualizationAnalysis(val context: Context,
                                           val externalModulesDFG: ExternalModulesDFG,
                                           val moduleDFG: ModuleDFG) {

        private val entryPoint = findMainEntryPoint(context)

        private val symbolTable = moduleDFG.symbolTable

        sealed class Node(val id: Int) : DirectedGraphNode<Node> {
            override val directEdges = mutableListOf<Node>()
            override val reversedEdges = mutableListOf<Node>()
            override val key get() = this

            val directCastEdges = mutableListOf<CastEdge>()
            val reversedCastEdges = mutableListOf<CastEdge>()

            val types = BitSet()

            var priority = -1

            fun addEdge(node: Node) {
                directEdges += node
                node.reversedEdges += this
            }

            fun addCastEdge(edge: CastEdge) {
                directCastEdges += edge
                edge.node.reversedCastEdges += CastEdge(this, edge.suitableTypes)
            }

            abstract fun toString(allTypes: List<DataFlowIR.Type.Declared>): String

            class Source(id: Int, typeId: Int, nameBuilder: () -> String): Node(id) {
                val name = takeName(nameBuilder)

                init {
                    types.set(typeId)
                }

                override fun toString(allTypes: List<DataFlowIR.Type.Declared>): String {
                    return "Source(name='$name', types='${types.format(allTypes)}')"
                }
            }

            class Ordinary(id: Int, nameBuilder: () -> String) : Node(id) {
                val name = takeName(nameBuilder)

                override fun toString(allTypes: List<DataFlowIR.Type.Declared>): String {
                    return "Ordinary(name='$name', types='${types.format(allTypes)}')"
                }
            }

            class CastEdge(val node: Node, val suitableTypes: BitSet)
        }

        class Function(val symbol: DataFlowIR.FunctionSymbol, val parameters: Array<Node>, val returns: Node)

        class ConstraintGraph : DirectedGraph<Node, Node> {

            private var nodesCount = 0

            override val nodes = mutableListOf<Node>()
            override fun get(key: Node) = key

            val voidNode = addNode { Node.Ordinary(it, { "Void" }) }
            val virtualNode = addNode { Node.Source(it, VIRTUAL_TYPE_ID, { "Virtual" }) }
            val arrayItemField = DataFlowIR.Field(null, 1, "Array\$Item")
            val functions = mutableMapOf<DataFlowIR.FunctionSymbol, Function>()
            val concreteClasses = mutableMapOf<DataFlowIR.Type.Declared, Node>()
            val externalFunctions = mutableMapOf<DataFlowIR.FunctionSymbol, Node>()
            val fields = mutableMapOf<DataFlowIR.Field, Node>() // Do not distinguish receivers.
            val virtualCallSiteReceivers = mutableMapOf<DataFlowIR.Node.VirtualCall, Triple<Node, List<DevirtualizedCallee>, DataFlowIR.FunctionSymbol>>()

            private fun nextId(): Int = nodesCount++

            fun addNode(nodeBuilder: (Int) -> Node) = nodeBuilder(nextId()).also { nodes.add(it) }
        }

        private val constraintGraph = ConstraintGraph()

        private fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
            if (this is DataFlowIR.Type.Declared) return this
            val hash = (this as DataFlowIR.Type.External).hash
            return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $hash")
        }

        private fun DataFlowIR.FunctionSymbol.resolved(): DataFlowIR.FunctionSymbol {
            if (this is DataFlowIR.FunctionSymbol.External)
                return externalModulesDFG.publicFunctions[this.hash] ?: this
            return this
        }

        private inner class TypeHierarchy(types: List<DataFlowIR.Type.Declared>) {
            private val typesSubTypes = mutableMapOf<DataFlowIR.Type.Declared, MutableList<DataFlowIR.Type.Declared>>()

            init {
                val visited = mutableSetOf<DataFlowIR.Type.Declared>()

                fun processType(type: DataFlowIR.Type.Declared) {
                    if (type == DataFlowIR.Type.Virtual) return
                    if (!visited.add(type)) return
                    type.superTypes
                            .map { it.resolved() }
                            .forEach { superType ->
                                val subTypes = typesSubTypes.getOrPut(superType, { mutableListOf() })
                                subTypes += type
                                processType(superType)
                            }
                }

                types.forEach { processType(it) }
            }

            private fun findAllInheritors(type: DataFlowIR.Type.Declared, result: MutableSet<DataFlowIR.Type.Declared>) {
                if (!result.add(type)) return
                typesSubTypes[type]?.forEach { findAllInheritors(it, result) }
            }

            fun inheritorsOf(type: DataFlowIR.Type.Declared): List<DataFlowIR.Type.Declared> {
                val result = mutableSetOf<DataFlowIR.Type.Declared>()
                findAllInheritors(type, result)
                return result.toList()
            }
        }

        private inner class InstantiationsSearcher(val moduleDFG: ModuleDFG,
                                                   val externalModulesDFG: ExternalModulesDFG,
                                                   val typeHierarchy: TypeHierarchy) {
            private val visited = mutableSetOf<DataFlowIR.FunctionSymbol>()
            private val typesVirtualCallSites = mutableMapOf<DataFlowIR.Type.Declared, MutableList<DataFlowIR.Node.VirtualCall>>()
            private val instantiatingClasses = mutableSetOf<DataFlowIR.Type.Declared>()

            fun search(): Set<DataFlowIR.Type.Declared> {
                // Rapid Type Analysis: find all instantiations and conservatively estimate call graph.
                if (entryPoint != null) {
                    // Optimistic algorithm: traverse call graph from the roots - the entry point and all global initializers.

                    dfs(symbolTable.mapFunction(entryPoint))

                    (moduleDFG.functions.values + externalModulesDFG.functionDFGs.values)
                            .map { it.symbol }
                            .filter { it.isGlobalInitializer }
                            .forEach { dfs(it) }
                } else {
                    // For a library assume the worst: find every instantiation and singleton and consider all of them possible.
                    moduleDFG.functions.values
                            .filter { it.symbol is DataFlowIR.FunctionSymbol.Public }
                            .forEach {
                                it.parameterTypes
                                        .map { it.resolved() }
                                        .filter { it.isFinal }
                                        .forEach { instantiatingClasses += it }
                            }
                    (moduleDFG.functions.values + externalModulesDFG.functionDFGs.values)
                            .asSequence()
                            .flatMap { it.body.nodes.asSequence() }
                            .forEach {
                                when (it) {
                                    is DataFlowIR.Node.NewObject -> instantiatingClasses += it.returnType.resolved()
                                    is DataFlowIR.Node.Singleton -> instantiatingClasses += it.type.resolved()
                                    is DataFlowIR.Node.Const -> instantiatingClasses += it.type.resolved()
                                }
                            }
                }
                return instantiatingClasses
            }

            private fun addInstantiatingClass(type: DataFlowIR.Type) {
                val resolvedType = type.resolved()
                if (!instantiatingClasses.add(resolvedType)) return

                DEBUG_OUTPUT(1) { println("Adding instantiating class: $resolvedType") }

                checkSupertypes(resolvedType, resolvedType, mutableSetOf())
            }

            private fun processVirtualCall(virtualCall: DataFlowIR.Node.VirtualCall,
                                           receiverType: DataFlowIR.Type.Declared) {
                DEBUG_OUTPUT(1) {
                    println("Processing virtual call: ${virtualCall.callee}")
                    println("Receiver type: $receiverType")
                }

                val callee = when (virtualCall) {
                    is DataFlowIR.Node.VtableCall ->
                        receiverType.vtable[virtualCall.calleeVtableIndex]

                    is DataFlowIR.Node.ItableCall ->
                        receiverType.itable[virtualCall.calleeHash]!!

                    else -> error("Unreachable")
                }
                dfs(callee)
            }

            private fun checkSupertypes(type: DataFlowIR.Type.Declared,
                                        inheritor: DataFlowIR.Type.Declared,
                                        seenTypes: MutableSet<DataFlowIR.Type.Declared>) {
                seenTypes += type

                DEBUG_OUTPUT(1) {
                    println("Checking supertype $type of $inheritor")
                    typesVirtualCallSites[type].let {
                        if (it == null)
                            println("None virtual call sites encountered yet")
                        else {
                            println("Virtual call sites:")
                            it.forEach {
                                println("    ${it.callee}")
                            }
                        }
                    }
                }

                typesVirtualCallSites[type]?.let { virtualCallSites ->
                    var index = 0
                    while (index < virtualCallSites.size) {
                        processVirtualCall(virtualCallSites[index], inheritor)
                        ++index
                    }
                }
                type.superTypes
                        .map { it.resolved() }
                        .filterNot { seenTypes.contains(it) }
                        .forEach { checkSupertypes(it, inheritor, seenTypes) }
            }

            private fun dfs(symbol: DataFlowIR.FunctionSymbol) {
                val resolvedFunctionSymbol = symbol.resolved()
                if (resolvedFunctionSymbol is DataFlowIR.FunctionSymbol.External) return
                if (!visited.add(resolvedFunctionSymbol)) return

                DEBUG_OUTPUT(1) { println("Visiting $resolvedFunctionSymbol") }

                val function = (moduleDFG.functions[resolvedFunctionSymbol] ?: externalModulesDFG.functionDFGs[resolvedFunctionSymbol])!!

                DEBUG_OUTPUT(1) { function.debugOutput() }

                nodeLoop@for (node in function.body.nodes) {
                    when (node) {
                        is DataFlowIR.Node.NewObject -> {
                            addInstantiatingClass(node.returnType)
                            dfs(node.callee)
                        }

                        is DataFlowIR.Node.Singleton -> {
                            addInstantiatingClass(node.type)
                            node.constructor?.let { dfs(it) }
                        }

                        is DataFlowIR.Node.Const -> addInstantiatingClass(node.type)

                        is DataFlowIR.Node.StaticCall -> dfs(node.callee)

                        is DataFlowIR.Node.VirtualCall -> {
                            if (node.receiverType == DataFlowIR.Type.Virtual)
                                continue@nodeLoop
                            val receiverType = node.receiverType.resolved()

                            DEBUG_OUTPUT(1) {
                                println("Adding virtual callsite:")
                                println("    Receiver: $receiverType")
                                println("    Callee: ${node.callee}")
                                println("    Inheritors:")
                                typeHierarchy.inheritorsOf(receiverType).forEach { println("        $it") }
                                println("    Encountered so far:")
                                typeHierarchy.inheritorsOf(receiverType)
                                        .filter { instantiatingClasses.contains(it) }
                                        .forEach { println("        $it") }
                            }

                            typesVirtualCallSites.getOrPut(receiverType, { mutableListOf() }).add(node)
                            typeHierarchy.inheritorsOf(receiverType)
                                    .filter { instantiatingClasses.contains(it) }
                                    .forEach { processVirtualCall(node, it) }
                        }
                    }
                }
            }
        }

        fun BitSet.copy() = BitSet(this.size()).apply { this.or(this@copy) }

        fun analyze(): Map<DataFlowIR.Node.VirtualCall, DevirtualizedCallSite> {
            val functions = moduleDFG.functions + externalModulesDFG.functionDFGs
            val typeHierarchy = TypeHierarchy(symbolTable.classMap.values.filterIsInstance<DataFlowIR.Type.Declared>() +
                                              externalModulesDFG.allTypes)
            val instantiatingClasses =
                    InstantiationsSearcher(moduleDFG, externalModulesDFG, typeHierarchy).search()
                            .withIndex()
                            .associate { it.value to (it.index + 1 /* 0 is reserved for [DataFlowIR.Type.Virtual] */) }
            val allTypes = listOf(DataFlowIR.Type.Virtual) + instantiatingClasses.asSequence().sortedBy { it.value }.map { it.key }

            val rootSet = computeRootSet(context, moduleDFG, externalModulesDFG)

            val nodesMap = mutableMapOf<DataFlowIR.Node, Node>()
            val variables = mutableMapOf<DataFlowIR.Node.Variable, Node>()
            val constraintGraphBuilder = ConstraintGraphBuilder(nodesMap, variables, typeHierarchy, instantiatingClasses, allTypes)
            rootSet.filterNot { it is DataFlowIR.FunctionSymbol.Declared && it.isGlobalInitializer }
                    .forEach { constraintGraphBuilder.buildFunctionConstraintGraph(it)!! }
            // TODO: Are globals inititalizers always called whether they are actually reachable from roots or not?
            functions.values
                    .filter { it.symbol.isGlobalInitializer }
                    .forEach { constraintGraphBuilder.buildFunctionConstraintGraph(it.symbol)!! }

            DEBUG_OUTPUT(0) {
                println("FULL CONSTRAINT GRAPH")
                constraintGraph.nodes.forEach {
                    println("    NODE #${it.id}: ${it.directCastEdges}")
                    it.directEdges.forEach {
                        println("        EDGE: #${it.id}z")
                    }
                    allTypes.forEachIndexed { index, type ->
                        if (it.types[index])
                            println("        TYPE: $type")
                    }
                }
            }

            constraintGraph.nodes.forEach {
                if (it is Node.Source)
                    assert(it.reversedEdges.isEmpty(), { "A source node #${it.id} has incoming edges" })
            }

            DEBUG_OUTPUT(0) {
                println("CONSTRAINT GRAPH: ${constraintGraph.nodes.size} nodes, " +
                    "${constraintGraph.nodes.sumBy { it.directEdges.size + it.directCastEdges.size } } edges")
            }

            val condensation = DirectedGraphCondensationBuilder(constraintGraph).build()
            val topologicalOrder = condensation.topologicalOrder.reversed()

            DEBUG_OUTPUT(0) {
                println("CONDENSATION")
                topologicalOrder.forEachIndexed { index, multiNode ->
                    println("    MULTI-NODE #$index")
                    multiNode.nodes.forEach {
                        println("        #${it.id}: ${it.toString(allTypes)}")
                    }
                }
            }

            topologicalOrder.forEachIndexed { index, multiNode -> multiNode.nodes.forEach { it.priority = index } }

            // Handle all 'right-directed' edges.
            // TODO: this is pessimistic handling of [DataFlowIR.Type.Virtual], think how to do it better.
            for (multiNode in topologicalOrder) {
                if (multiNode.nodes.size == 1 && multiNode.nodes.first() is Node.Source)
                    continue // A source has no incoming edges.
                val types = BitSet()
                for (node in multiNode.nodes) {
                    node.reversedEdges.forEach { types.or(it.types) }
                    node.reversedCastEdges
                            .filter { it.node.priority < node.priority } // Doesn't contradict topological order.
                            .forEach {
                                val sourceTypes = it.node.types.copy()
                                sourceTypes.and(it.suitableTypes)
                                types.or(sourceTypes)
                            }
                }
                for (node in multiNode.nodes)
                    node.types.or(types)
            }
            val badEdges = mutableListOf<Pair<Node, Node.CastEdge>>()
            for (node in constraintGraph.nodes) {
                node.directCastEdges
                        .filter { it.node.priority < node.priority } // Contradicts topological order.
                        .forEach { badEdges += node to it }
            }
            badEdges.sortBy { it.second.node.priority } // Heuristic.

            do {
                fun propagateTypes(node: Node, types: BitSet) {
                    node.types.or(types)
                    for (edge in node.directEdges) {
                        val missingTypes = types.copy().apply { andNot(edge.types) }
                        if (!missingTypes.isEmpty)
                            propagateTypes(edge, missingTypes)
                    }
                    for (castEdge in node.directCastEdges) {
                        val missingTypes = types.copy().apply { andNot(castEdge.node.types) }
                        missingTypes.and(castEdge.suitableTypes)
                        if (!missingTypes.isEmpty)
                            propagateTypes(castEdge.node, missingTypes)
                    }
                }

                var end = true
                for ((sourceNode, edge) in badEdges) {
                    val distNode = edge.node
                    val missingTypes = sourceNode.types.copy().apply { andNot(distNode.types) }
                    missingTypes.and(edge.suitableTypes)
                    if (!missingTypes.isEmpty) {
                        end = false
                        propagateTypes(distNode, missingTypes)
                    }
                }
            } while (!end)

            DEBUG_OUTPUT(0) {
                topologicalOrder.forEachIndexed { index, multiNode ->
                    println("Types of multi-node #$index")
                    for (node in multiNode.nodes) {
                        println("    Node #${node.id}")
                        allTypes.withIndex()
                                .filter { node.types[it.index] }
                                .forEach { println("        ${it.value}") }
                    }
                }
            }

            val result = mutableMapOf<DataFlowIR.Node.VirtualCall, Pair<DevirtualizedCallSite, DataFlowIR.FunctionSymbol>>()
            val nothing = symbolTable.mapClass(context.builtIns.nothing)
            functions.values
                    .asSequence()
                    .filter { constraintGraph.functions.containsKey(it.symbol) }
                    .flatMap { it.body.nodes.asSequence() }
                    .filterIsInstance<DataFlowIR.Node.VirtualCall>()
                    .forEach { virtualCall ->
                        assert (nodesMap[virtualCall] != null, { "Node for virtual call $virtualCall has not been built" })
                        val receiver = constraintGraph.virtualCallSiteReceivers[virtualCall]
                        if (receiver == null) {
                            result.put(virtualCall, DevirtualizedCallSite(emptyList()) to virtualCall.callee)
                            return@forEach
                        }
                        if (receiver.first.types[VIRTUAL_TYPE_ID]) {

                            DEBUG_OUTPUT(0) {
                                println("Unable to devirtualize callsite ${virtualCall.callSite?.let { ir2stringWhole(it) } ?: virtualCall.toString() }")
                                println("    receiver is Virtual")
                            }

                            return@forEach
                        }
                        val possibleReceivers = allTypes.withIndex()
                                .filter { receiver.first.types[it.index] }
                                .map { it.value }
                                .filter { it != nothing }
                        val map = receiver.second.associateBy({ it.receiverType }, { it })
                        result.put(virtualCall, DevirtualizedCallSite(possibleReceivers.map { receiverType ->
                            assert (map[receiverType] != null) {
                                "Non-expected receiver type $receiverType at call site: " +
                                    (virtualCall.callSite?.let { ir2stringWhole(it) } ?: virtualCall.toString())
                            }
                            val devirtualizedCallee = map[receiverType]!!
                            val callee = devirtualizedCallee.callee
                            if (callee is DataFlowIR.FunctionSymbol.Declared && callee.symbolTableIndex < 0)
                                error("Function ${devirtualizedCallee.receiverType}.$callee cannot be called virtually," +
                                        " but actually is at call site: ${virtualCall.callSite?.let { ir2stringWhole(it) } ?: virtualCall.toString() }")
                            devirtualizedCallee
                        }) to receiver.third)
                    }

            DEBUG_OUTPUT(0) {
                result.forEach { virtualCall, devirtualizedCallSite ->
                    if (devirtualizedCallSite.first.possibleCallees.isNotEmpty()) {
                        println("DEVIRTUALIZED")
                        println("FUNCTION: ${devirtualizedCallSite.second}")
                        println("CALL SITE: ${virtualCall.callSite?.let { ir2stringWhole(it) } ?: virtualCall.toString()}")
                        println("POSSIBLE RECEIVERS:")
                        devirtualizedCallSite.first.possibleCallees.forEach { println("    TYPE: ${it.receiverType}") }
                        devirtualizedCallSite.first.possibleCallees.forEach { println("    FUN: ${it.callee}") }
                        println()
                    }
                }
            }

            return result.asSequence().associateBy({ it.key }, { it.value.first })
        }

        private inner class ConstraintGraphBuilder(val functionNodesMap: MutableMap<DataFlowIR.Node, Node>,
                                                   val variables: MutableMap<DataFlowIR.Node.Variable, Node>,
                                                   val typeHierarchy: TypeHierarchy,
                                                   val instantiatingClasses: Map<DataFlowIR.Type.Declared, Int>,
                                                   val allTypes: List<DataFlowIR.Type.Declared>) {

            private fun concreteType(type: DataFlowIR.Type.Declared): Int {
                assert(!(type.isAbstract && type.isFinal)) { "Incorrect type: $type" }
                return if (type.isAbstract) VIRTUAL_TYPE_ID else instantiatingClasses[type]!!
            }

            private fun ordinaryNode(nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Ordinary(it, nameBuilder) }

            private fun sourceNode(typeId: Int, nameBuilder: () -> String) =
                    constraintGraph.addNode { Node.Source(it, typeId, nameBuilder) }

            fun buildFunctionConstraintGraph(symbol: DataFlowIR.FunctionSymbol): Function? {
                if (symbol is DataFlowIR.FunctionSymbol.External) return null
                constraintGraph.functions[symbol]?.let { return it }

                val function = (moduleDFG.functions[symbol] ?: externalModulesDFG.functionDFGs[symbol])
                        ?: error("Unknown function: $symbol")

                val body = function.body
                val parameters = Array(symbol.numberOfParameters) { ordinaryNode { "Param#$it\$$symbol" } }

                if (entryPoint == null && symbol is DataFlowIR.FunctionSymbol.Public && moduleDFG.functions.containsKey(symbol)) {
                    // Exported function from the current module.
                    function.parameterTypes.forEachIndexed { index, type ->
                        val resolvedType = type.resolved()
                        val node = if (!resolvedType.isFinal)
                                       constraintGraph.virtualNode
                                   else {
                                       constraintGraph.concreteClasses.getOrPut(resolvedType) {
                                           sourceNode(concreteType(resolvedType)) { "Class\$$resolvedType" }
                                       }
                                   }
                        node.addEdge(parameters[index])
                    }
                }

                val returnsNode = ordinaryNode { "Returns\$$symbol" }
                val functionConstraintGraph = Function(symbol, parameters, returnsNode)
                constraintGraph.functions[symbol] = functionConstraintGraph
                body.nodes.forEach { dfgNodeToConstraintNode(functionConstraintGraph, it) }
                functionNodesMap[body.returns]!!.addEdge(returnsNode)


                DEBUG_OUTPUT(0) {
                    println("CONSTRAINT GRAPH FOR ${symbol}")
                    val ids = function.body.nodes.withIndex().associateBy({ it.value }, { it.index })
                    for (node in function.body.nodes) {
                        println("FT NODE #${ids[node]}")
                        DataFlowIR.Function.printNode(node, ids)
                        val constraintNode = functionNodesMap[node] ?: variables[node] ?: break
                        println("       CG NODE #${constraintNode.id}: ${constraintNode.toString(allTypes)}")
                        println()
                    }
                    println("Returns: #${ids[function.body.returns]}")
                    println()
                }

                return functionConstraintGraph
            }

            private fun inheritorsOf(type: DataFlowIR.Type.Declared): BitSet {
                val suitableTypes = BitSet()
                suitableTypes.set(VIRTUAL_TYPE_ID)
                for (inheritor in typeHierarchy.inheritorsOf(type)) {
                    instantiatingClasses[inheritor]?.let { suitableTypes.set(it) }
                }
                return suitableTypes
            }

            private fun edgeToConstraintNode(function: Function,
                                             edge: DataFlowIR.Edge): Node {
                val result = dfgNodeToConstraintNode(function, edge.node)
                val castToType = edge.castToType?.resolved() ?: return result
                val castNode = ordinaryNode { "Cast\$${function.symbol}" }
                val castEdge = Node.CastEdge(castNode, inheritorsOf(castToType))
                result.addCastEdge(castEdge)
                return castNode
            }

            /**
             * Takes a function DFG's node and creates a constraint graph node corresponding to it.
             * Also creates all necessary edges.
             */
            private fun dfgNodeToConstraintNode(function: Function, node: DataFlowIR.Node): Node {

                fun edgeToConstraintNode(edge: DataFlowIR.Edge): Node =
                        edgeToConstraintNode(function, edge)

                fun argumentToConstraintNode(argument: Any): Node =
                        when (argument) {
                            is Node -> argument
                            is DataFlowIR.Edge -> edgeToConstraintNode(argument)
                            else -> error("Unexpected argument: $argument")
                        }

                fun doCall(callee: Function, arguments: List<Any>): Node {
                    assert(callee.parameters.size == arguments.size) {
                        "Function ${callee.symbol} takes ${callee.parameters.size} but caller ${function.symbol} provided ${arguments.size}"
                    }
                    callee.parameters.forEachIndexed { index, parameter ->
                        val argument = argumentToConstraintNode(arguments[index])
                        argument.addEdge(parameter)
                    }
                    return callee.returns
                }

                fun doCall(callee: DataFlowIR.FunctionSymbol,
                           arguments: List<Any>,
                           returnType: DataFlowIR.Type.Declared,
                           receiverType: DataFlowIR.Type.Declared?): Node {
                    val resolvedCallee = callee.resolved()
                    val calleeConstraintGraph = buildFunctionConstraintGraph(resolvedCallee)
                    return if (calleeConstraintGraph == null) {
                        constraintGraph.externalFunctions.getOrPut(resolvedCallee) {
                            val fictitiousReturnNode = ordinaryNode { "External$resolvedCallee" }
                            val possibleReturnTypes = typeHierarchy.inheritorsOf(returnType).filter { instantiatingClasses.containsKey(it) }
                            for (type in possibleReturnTypes) {
                                constraintGraph.concreteClasses.getOrPut(type) {
                                    sourceNode(concreteType(type)) { "Class\$$type" }
                                }.addEdge(fictitiousReturnNode)
                            }
                            fictitiousReturnNode
                        }
                    } else {
                        if (receiverType == null)
                            doCall(calleeConstraintGraph, arguments)
                        else {
                            val receiverNode = argumentToConstraintNode(arguments[0])
                            val castedReceiver = ordinaryNode { "CastedReceiver\$${function.symbol}" }
                            val castedEdge = Node.CastEdge(castedReceiver, inheritorsOf(receiverType))
                            receiverNode.addCastEdge(castedEdge)
                            doCall(calleeConstraintGraph, listOf(castedReceiver) + arguments.drop(1))
                        }
                    }
                }

                if (node is DataFlowIR.Node.Variable && !node.temp) {
                    var variableNode = variables[node]
                    if (variableNode == null) {
                        variableNode = ordinaryNode { "Variable\$${function.symbol}" }
                        variables[node] = variableNode
                        for (value in node.values) {
                            edgeToConstraintNode(value).addEdge(variableNode)
                        }
                    }
                    return variableNode
                }

                return functionNodesMap.getOrPut(node) {
                    when (node) {
                        is DataFlowIR.Node.Const ->
                            sourceNode(concreteType(node.type.resolved())) { "Const\$${function.symbol}" }

                        is DataFlowIR.Node.Parameter ->
                            function.parameters[node.index]

                        is DataFlowIR.Node.StaticCall ->
                            doCall(node.callee, node.arguments, node.returnType.resolved(), node.receiverType?.resolved())

                        is DataFlowIR.Node.NewObject -> {
                            val returnType = node.returnType.resolved()
                            val instanceNode = constraintGraph.concreteClasses.getOrPut(returnType) {
                                sourceNode(concreteType(returnType)) { "Class\$$returnType" }
                            }
                            doCall(node.callee, listOf(instanceNode) + node.arguments, returnType, null)
                            instanceNode
                        }

                        is DataFlowIR.Node.VirtualCall -> {
                            val callee = node.callee
                            if (node.receiverType == DataFlowIR.Type.Virtual)
                                constraintGraph.voidNode
                            else {
                                val receiverType = node.receiverType.resolved()

                                DEBUG_OUTPUT(0) {
                                    println("Virtual call")
                                    println("Caller: ${function.symbol}")
                                    println("Callee: $callee")
                                    println("Receiver type: $receiverType")
                                }

                                val possibleReceiverTypes = typeHierarchy.inheritorsOf(receiverType).filter { instantiatingClasses.containsKey(it) }
                                val callees = possibleReceiverTypes.map {
                                    when (node) {
                                        is DataFlowIR.Node.VtableCall ->
                                            it.vtable[node.calleeVtableIndex]

                                        is DataFlowIR.Node.ItableCall ->
                                            it.itable[node.calleeHash]!!

                                        else -> error("Unreachable")
                                    }
                                }

                                DEBUG_OUTPUT(0) {
                                    println("Possible callees:")
                                    callees.forEach { println("$it") }
                                    println()
                                }

                                val returnType = node.returnType.resolved()
                                val receiverNode = edgeToConstraintNode(node.arguments[0])
                                val castedReceiver = ordinaryNode { "CastedReceiver\$${function.symbol}" }
                                val castedEdge = Node.CastEdge(castedReceiver, inheritorsOf(receiverType))
                                receiverNode.addCastEdge(castedEdge)
                                val arguments = listOf(castedReceiver) + node.arguments.drop(1)
                                val result = when {
                                    callees.isEmpty() -> {

                                        DEBUG_OUTPUT(0) {
                                            println("WARNING: no possible callees for call ${node.callee}")
                                        }

                                        constraintGraph.voidNode
                                    }

                                    callees.size == 1 -> doCall(callees[0], arguments, returnType, possibleReceiverTypes.single())

                                    else -> {
                                        val returns = ordinaryNode { "VirtualCallReturns\$${function.symbol}" }
                                        callees.forEachIndexed { index, actualCallee ->
                                            doCall(actualCallee, arguments, returnType, possibleReceiverTypes[index]).addEdge(returns)
                                        }
                                        returns
                                    }
                                }
                                val devirtualizedCallees = possibleReceiverTypes.mapIndexed { index, possibleReceiverType ->
                                    DevirtualizedCallee(possibleReceiverType, callees[index])
                                }
                                constraintGraph.virtualCallSiteReceivers[node] = Triple(castedReceiver, devirtualizedCallees, function.symbol)
                                result
                            }
                        }

                        is DataFlowIR.Node.Singleton -> {
                            val type = node.type.resolved()
                            node.constructor?.let { buildFunctionConstraintGraph(it) }
                            constraintGraph.concreteClasses.getOrPut(type) {
                                sourceNode(concreteType(type)) { "Class\$$type" }
                            }
                        }

                        is DataFlowIR.Node.FieldRead ->
                            constraintGraph.fields.getOrPut(node.field) {
                                ordinaryNode { "Field\$${node.field}" }
                            }

                        is DataFlowIR.Node.FieldWrite -> {
                            val fieldNode = constraintGraph.fields.getOrPut(node.field) {
                                ordinaryNode { "Field\$${node.field}" }
                            }
                            edgeToConstraintNode(node.value).addEdge(fieldNode)
                            constraintGraph.voidNode
                        }

                        is DataFlowIR.Node.ArrayRead ->
                            constraintGraph.fields.getOrPut(constraintGraph.arrayItemField) {
                                ordinaryNode { "Field\$${constraintGraph.arrayItemField}" }
                            }

                        is DataFlowIR.Node.ArrayWrite -> {
                            val fieldNode = constraintGraph.fields.getOrPut(constraintGraph.arrayItemField) {
                                ordinaryNode { "Field\$${constraintGraph.arrayItemField}" }
                            }
                            edgeToConstraintNode(node.value).addEdge(fieldNode)
                            constraintGraph.voidNode
                        }

                        is DataFlowIR.Node.Variable ->
                            node.values.map { edgeToConstraintNode(it) }.let { values ->
                                ordinaryNode { "TempVar\$${function.symbol}" }.also { node ->
                                    values.forEach { it.addEdge(node) }
                                }
                            }

                        else -> error("Unreachable")
                    }
                }
            }
        }

    }

    class DevirtualizedCallee(val receiverType: DataFlowIR.Type, val callee: DataFlowIR.FunctionSymbol)

    class DevirtualizedCallSite(val possibleCallees: List<DevirtualizedCallee>)

    fun run(irModule: IrModuleFragment, context: Context, moduleDFG: ModuleDFG, externalModulesDFG: ExternalModulesDFG)
            : Map<DataFlowIR.Node.VirtualCall, DevirtualizedCallSite> {
        val devirtualizationAnalysisResult = DevirtualizationAnalysis(context, externalModulesDFG, moduleDFG).analyze()
        val devirtualizedCallSites =
                devirtualizationAnalysisResult
                        .asSequence()
                        .filter { it.key.callSite != null }
                        .associate { it.key.callSite!! to it.value }
        Devirtualization.devirtualize(irModule, context, devirtualizedCallSites)
        return devirtualizationAnalysisResult
    }

    private fun devirtualize(irModule: IrModuleFragment, context: Context,
                             devirtualizedCallSites: Map<IrCall, DevirtualizedCallSite>) {
        irModule.transformChildrenVoid(object: IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val devirtualizedCallSite = devirtualizedCallSites[expression]
                val actualCallee = devirtualizedCallSite?.possibleCallees?.singleOrNull()?.callee as? DataFlowIR.FunctionSymbol.Declared
                        ?: return expression
                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
                irBuilder.run {
                    val dispatchReceiver = expression.dispatchReceiver!!
                    return IrPrivateFunctionCallImpl(
                            startOffset,
                            endOffset,
                            expression.type,
                            expression.symbol,
                            expression.descriptor,
                            (expression as? IrCallImpl)?.typeArguments,
                            actualCallee.module.descriptor,
                            actualCallee.module.numberOfFunctions,
                            actualCallee.symbolTableIndex
                    ).apply {
                        this.dispatchReceiver    = dispatchReceiver
                        this.extensionReceiver   = expression.extensionReceiver
                        expression.descriptor.valueParameters.forEach {
                            this.putValueArgument(it.index, expression.getValueArgument(it))
                        }
                    }
                }
            }
        })
    }
}
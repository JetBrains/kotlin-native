package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.util.dump

/*
 * Simple implementation of escape analysis based on:
 * Choi et al. "Stack Allocation and Synchronization Optimizations for Java Using Escape Analysis",
 * ACM Transactions on Programming Languages and Systems (TOPLAS), Volume 25 Issue 6.
 *
 * TODO: Intraprocedural analysis part is not implemented
 * TODO: Without inlining/IPA it is not very useful;
 *       Absence of CFG makes it even more coarse grained
 * TODO: Kotlin coding convention/idioms not fully followed
 * TODO: Did not pay any attention to performance.
 */

internal object SimpleEscapeAnalysis {
    private val DEBUG = 10

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private enum class EscapeState {
        GLOBAL_ESCAPE,       // escape through global reference
        ARG_ESCAPE,         // escapes through function arguments, return or throw
        NO_ESCAPE           // object does not escape
    }

    private enum class NodeKind {
        PHANTOM,            // phantom object node (e.g., to represent object pointed to by formal parameter)
        OBJECT,             // new object returned from constructor
        LOCAL_REF,          // local reference variable
        ARG_REF,            // actual parameter or return
        FIELD_REF,          // non-static field/object reference
        GLOBAL_REF          // global reference variable
    }

    private enum class EdgeKind {
        FIELD,              // Object.field
        POINTS_TO,          // p = Object
        DEFERRED            // p = q
    }

    private open class CGNode(
        val kind: NodeKind,
        val irnode: DataFlowIR.Node,
        var escape: EscapeState = EscapeState.NO_ESCAPE
    ) {
        val succs = mutableListOf<Edge>()

        fun addSucc(succ: CGNode) {
            val e = Edge(this, succ)
            /* check that equals() work as expected :)
            if (e !in succs) {
                assert(succs.find {it.dest == succ} == null)
            }
            */
            if (e !in succs) succs.add(e)
        }
    }

    private open class ObjectNode(irnode: DataFlowIR.Node, escape: EscapeState) :
        CGNode(NodeKind.OBJECT, irnode, escape) {
        /*
        val type: DataFlowIR.Type
            get() = when (irnode) {
                is DataFlowIR.Node.Call -> irnode.returnType
                is DataFlowIR.Node.Const -> irnode.type
                is DataFlowIR.Node.Singleton -> irnode.type
                is DataFlowIR.Node.AllocInstance -> irnode.type
                else -> throw java.lang.IllegalArgumentException()
            }
         */
    }

    private class PhantomNode(irnode: DataFlowIR.Node, escape: EscapeState) : ObjectNode(irnode, escape)

    private open class RefNode(irnode: DataFlowIR.Node, kind: NodeKind, escape: EscapeState) :
        CGNode(kind, irnode, escape)

    private class LocalRefNode(irnode: DataFlowIR.Node) : RefNode(irnode, NodeKind.LOCAL_REF, EscapeState.NO_ESCAPE)

    private class GlobalRefNode(irnode: DataFlowIR.Node) :
        RefNode(irnode, NodeKind.GLOBAL_REF, EscapeState.GLOBAL_ESCAPE)

    private class FieldRefNode(irnode: DataFlowIR.Node, val field: DataFlowIR.Field) :
        RefNode(irnode, NodeKind.FIELD_REF, EscapeState.NO_ESCAPE)

    private class ArrayFieldRefNode() : RefNode(DataFlowIR.Node.Null, NodeKind.FIELD_REF, EscapeState.NO_ESCAPE)

    private class ActualNode(irnode: DataFlowIR.Node, val index: Int) :
        RefNode(irnode, NodeKind.ARG_REF, EscapeState.ARG_ESCAPE)

    private class Edge(val source: CGNode, val dest: CGNode) {
        fun kind(): EdgeKind {
            if (source.kind == NodeKind.OBJECT)
                return EdgeKind.FIELD
            else if (dest.kind == NodeKind.OBJECT)
                return EdgeKind.POINTS_TO
            else
                return EdgeKind.DEFERRED
        }

        /*
        override fun equals(other: Any?): Boolean {
            if (this == other) return true
            if (other !is Edge) return false
            return source == other.source && dest == other.dest
        }
        */

    }

    // Connection graph is a directed graph G = (NO U NR, EP U ED EF)
    // where
    // NO represent set of objects
    // NR is a set of reference nodes; in turn it consist of these sets:
    //   NL - set of local references
    //   NA - set of actuals and return values (we include throws here)
    //   NF - set of non-static fields and
    //   NG - set of global references
    // EP - set of points-to edges (source is in NR, sink is in NO)
    // ED - set of deferred edges (both source and sink are from NR)
    // EF - set of field edges (source is in NO, sink represents field of source object)
    //
    // Main four operations used to build CG:
    // 1. p = Object() : generates points-to edge p -> O
    // 2. p = q        : generates deferred edge p -> q
    // 3. p.f = q      : generates a set of edges pointsTo(p).f -> q
    // 4. p = q.f      : generates a set of edges p = pointsTo(q).f
    //
    // The graph construction is completed using simple flow equations over (GLOBAL_ESCAPE, ARG_ESCAPE, NO_ESCAPE)
    // lattice. NOTE: we do not perform neither flow sensitive analysis (due to absense of CFG) nor intraprocedural
    // analysis, so our implementation is somewhat simpler than described in paper.
    private class ConnectionGraph private constructor(val function: DataFlowIR.Function) {
        // Phantom node represents objects pointed to by formal parameters
        // We keep single node for all of them
        val phantom = PhantomNode(DataFlowIR.Node.Null, EscapeState.GLOBAL_ESCAPE)

        val allnodes: List<CGNode>
            get() = nodes.toList()

        private val nodeMap = HashMap<DataFlowIR.Node, CGNode>()

        // Not all CG nodes directly map to DataFlowIR nodes, so we additionally store all CG nodes in
        // this set. (Mostly used in graph visialisation)
        private val nodes = mutableSetOf<CGNode>(phantom)

        // Used for debug purposes
        /*private*/ val ids = function.body.nodes.withIndex().associateBy({ it.value }, { it.index })

        companion object Builder {
            fun build(f: DataFlowIR.Function): ConnectionGraph {
                val CG = ConnectionGraph(f)
                f.body.nodes.forEach { CG.getNode(it) }
                CG.getNode(f.body.returns).escape = EscapeState.ARG_ESCAPE
                CG.getNode(f.body.throws).escape = EscapeState.ARG_ESCAPE
                /*
                if (f.symbol is DataFlowIR.FunctionSymbol.Public && f.symbol.hash == 3978444438534600335 ) {
                    println("Before propagate:")
                    CG.printDot()
                }
                */
                CG.propagate()
                return CG
            }
        }

        fun connect(src: CGNode, sink: CGNode) = src.addSucc(sink)

        // Get field ref node. Create one if does not exists
        fun ObjectNode.getOrAddField(field: DataFlowIR.Field): CGNode {
            return succs.find { (it.dest as FieldRefNode).field == field }?.dest
                ?: FieldRefNode(this.irnode, field).also { addSucc(it) }
        }

        // Get artificial array ref field.
        // (We use single pseudo-field nodes to represent all array members)
        fun ObjectNode.getOrAddArrayField(): CGNode {
            return succs.find { it.dest is ArrayFieldRefNode }?.dest
                ?: ArrayFieldRefNode().also { addSucc(it) }
        }

        //fun getNode(edge : DataFlowIR.Edge) = getNode(edge.node)

        // Used in debug assertion to check that we process any DataFlowIR node only once
        private val visitedIRNodes = mutableSetOf<DataFlowIR.Node>()

        fun getNode(node: DataFlowIR.Node): CGNode {

            DEBUG_OUTPUT(0) { println("CG.getNode(${ids[node]}): ${nodeMap.get(node)}") }

            return nodeMap.getOrPut(node) {
                DEBUG_OUTPUT(8) { println("...MAKING NEW NODE") }
                assert(!visitedIRNodes.contains(node))
                visitedIRNodes += node
                when (node) {
                    is DataFlowIR.Node.Parameter -> {
                        DEBUG_OUTPUT(8) { println("...parameter ${node.index}") }
                        LocalRefNode(node).also { connect(it, phantom) }
                    }
                    is DataFlowIR.Node.Variable -> {
                        DEBUG_OUTPUT(8) { println("...variable") }
                        val cgnode = LocalRefNode(node)
                        nodeMap[node] = cgnode // Put into map immediately to avoid endless recursion
                        node.values.forEach {
                            // edge ->
                            DEBUG_OUTPUT(8) { println("   ${ids[node]}'s value ${ids[it.node]}") }
                            val value = it.node
                            assert(value != node) { "Variable points to itself" }
                            // What about FIELD WRITE?
                            assert(value !is DataFlowIR.Node.FieldWrite) { "FIELD WRITE used as value" }
                            //assert(value !is DataFlowIR.Node.ArrayWrite) { "ARRAY WRITE used as value" } Oops
                            if (value is DataFlowIR.Node.FieldRead && value.receiver != null) {
                                val receiver = getNode(value.receiver.node)
                                pointsTo(receiver).forEach { obj ->
                                    val field = obj.getOrAddField(value.field)
                                    nodes.add(field)
                                    connect(cgnode, field)
                                }
                            } else {
                                connect(cgnode, getNode(value))
                            }
                        }
                        cgnode
                    }
                    is DataFlowIR.Node.FieldRead -> {
                        // Should not be used. Return dummy node
                        if (node.receiver == null) {
                            GlobalRefNode(node)
                        } else {
                            phantom //PhantomNode(node, EscapeState.NO_ESCAPE)
                        }
                    }
                    is DataFlowIR.Node.FieldWrite -> {
                        val receiver = node.receiver
                        if (receiver == null) {
                            val global = GlobalRefNode(node)
                            nodeMap[node] = global
                            connect(global, getNode((node.value.node)))
                            global
                        } else {
                            val pointees = pointsTo(getNode(receiver.node))
                            val value = getNode(node.value.node)
                            pointees.forEach { obj ->
                                val field = obj.getOrAddField(node.field)
                                nodes.add(field)
                                connect(field, value)
                            }
                            // dummy node
                            PhantomNode(node, EscapeState.NO_ESCAPE)
                        }
                    }
                    is DataFlowIR.Node.Const -> {
                        // XXX: Not sure if Const may be an object?
                        DEBUG_OUTPUT(8) { println("...const") }
                        ObjectNode(node, EscapeState.NO_ESCAPE)
                    }
                    is DataFlowIR.Node.AllocInstance -> {
                        ObjectNode(node, EscapeState.NO_ESCAPE)
                    }
                    is DataFlowIR.Node.Singleton -> { // FIXME: GLOBAL_REF?
                        ObjectNode(node, EscapeState.GLOBAL_ESCAPE)
                    }
                    is DataFlowIR.Node.Null -> {
                        ObjectNode(node, EscapeState.NO_ESCAPE) // FIXME: Huh?
                    }
                    is DataFlowIR.Node.Call -> {
                        val cgnode = ObjectNode(node, EscapeState.NO_ESCAPE)
                        nodeMap[node] = cgnode
                        node.arguments.forEachIndexed { index, arg ->
                            val actual = ActualNode(node, index)
                            nodes.add(actual)
                            connect(actual, getNode(arg.node))
                        }
                        cgnode
                    }
                    is DataFlowIR.Node.ArrayRead -> {
                        // Conservatively treat array as a single object/ref
                        getNode(node.array.node)
                    }
                    is DataFlowIR.Node.ArrayWrite -> {
                        DEBUG_OUTPUT(8) {
                            println("...array write array ${ids[node.array.node]} value ${ids[node.value.node]}")
                        }
                        val arr = getNode(node.array.node)
                        // Conservatively treat array as a single object/ref
                        pointsTo(arr).forEach {
                            val af = it.getOrAddArrayField()
                            nodes.add(af)
                            connect(af, getNode(node.value.node))
                        }
                        arr
                    }
                    else -> {
                        throw java.lang.IllegalArgumentException()
                    }
                }
            }.also { nodes.add(it) }
             .also { DEBUG_OUTPUT(8) { println("return NODE ${ids[node]}   escape: ${it.escape.name}") } }
        }

        // Find all objects this ref may point to
        fun pointsTo(ref: CGNode): Set<ObjectNode> {
            if (ref is ObjectNode) return setOf(ref)
            visited.clear()
            val result = mutableSetOf<ObjectNode>()
            visitSuccs(ref) {
                if (it is ObjectNode) {
                    result += it
                    // do not descent into field refs
                    visited.addAll(it.succs.map { edge -> edge.dest })
                }
            }
            return result
        }


        private val visited = mutableSetOf<CGNode>()
        private fun visitSuccs(node: CGNode, visitor: (CGNode) -> Unit) {
            visited += node
            DEBUG_OUTPUT(15) { println("visitSuccs: ${nodeName(node)}") }
            node.succs.forEach {
                DEBUG_OUTPUT(15) { if (visited.contains(it.dest)) println("${nodeName(it.dest)} already visited") }
                if (!visited.contains(it.dest)) {
                    visitor(it.dest)
                    visitSuccs(it.dest, visitor)
                }
            }
        }

        /* Propagate escape status */
        fun propagate() {
            visited.clear()
            nodes.filter { it.escape == EscapeState.GLOBAL_ESCAPE }.forEach {
                visitSuccs(it) { cgnode ->
                    cgnode.escape = EscapeState.GLOBAL_ESCAPE; println("${nodeName(cgnode)} is GLOBAL_ESCAPE")
                }
            }

            nodes.filter { it.escape == EscapeState.ARG_ESCAPE }.forEach {
                visitSuccs(it) { cgnode ->
                    cgnode.escape = EscapeState.ARG_ESCAPE; println("${nodeName(cgnode)} is ARG_ESCAPE")
                }
            }
        }

        fun verify() {
            nodes.forEach { node ->
                node.succs.forEach { edge ->
                    val dest = edge.dest
                    if (edge.kind() == EdgeKind.POINTS_TO) {
                        assert(node.kind != NodeKind.OBJECT)
                        assert(node.escape >= dest.escape) { "ESCAPE(${ids[node.irnode]}):${node.escape} < ESCAPE(${ids[dest.irnode]}):${dest.escape}" }
                    } else if (edge.kind() == EdgeKind.FIELD) {
                        assert(node.escape == dest.escape) { "ESCAPE(${ids[node.irnode]}):${node.escape} != ESCAPE(${ids[dest.irnode]}):${dest.escape}" }
                    }
                }
            }
        }

        fun printDot() {
            println("digraph ConnectionGraph {")
            allnodes.forEach { cgnode ->
                val irnode = cgnode.irnode
                val src = nodeName(cgnode)
                print(src)
                printDotNodeAttrs(cgnode)
                println(";")
                cgnode.succs.forEach { edge ->
                    val dst = nodeName(edge.dest)
                    print("$src -> $dst")
                    printDotEdgeAttrs(edge)
                    println(";")
                }
            }
            println("}")
        }

        private fun nodeName(node: CGNode) = when (node) {
            is PhantomNode -> "Phantom"
            is ObjectNode -> "Object"
            is LocalRefNode -> "LocRef"
            is GlobalRefNode -> "GlobRef"
            is FieldRefNode -> "FieldRef"
            is ActualNode -> "ArgRef${node.index}Of"
            is ArrayFieldRefNode -> "ArrField"
            else -> "CGNode"
        } + "${ids[node.irnode] ?: ""}"

        private fun printDotNodeAttrs(node: CGNode) {
            print("[")
            when (node) {
                is ObjectNode -> print("shape=rectangle ")
                is FieldRefNode -> print("shape=diamond ")
            }
            when (node.escape) {
                EscapeState.NO_ESCAPE -> print("color=green ")
                EscapeState.ARG_ESCAPE -> print("color=blue ")
            }
            print("]")
        }

        private fun printDotEdgeAttrs(edge: Edge) {
            if (edge.kind() == EdgeKind.DEFERRED)
                print("[style=dashed]")
        }
    }

    fun analyze(context: Context, moduleDFG: ModuleDFG, lifetimes: MutableMap<IrElement, Lifetime>) {
        val functions = moduleDFG.functions

        functions.forEach { (name, function) ->
            DEBUG_OUTPUT(5) {
                println("===============================================")
                println("Visiting $name")
                println("IR:")
                println(function.symbol.irFunction?.dump() ?: "NO SOURCE")
                println("-----------------------------------------------")
                println("DATA FLOW IR:")
                function.debugOutput()
            }

            val CG = ConnectionGraph.build(function)

            DEBUG_OUTPUT(5) {
                CG.printDot()
//                val indices = CG.nodes.values.toList().withIndex().associateBy({it.value}, {it.index})
//                function.body.nodes.forEach { irnode ->
//                    val cgnode = CG.nodes[irnode]
//                    println("IRNODE ${CG.ids[irnode]} -> CGNODE ${indices[cgnode]}")
//                }
                CG.verify()
            }
            CG.allnodes.filter {
                it.escape == EscapeState.NO_ESCAPE
            }.forEach { cgnode ->
                val ir = when (cgnode.irnode) {
                    is DataFlowIR.Node.Call -> cgnode.irnode.irCallSite
                    else -> null
                }
                ir?.let {
                    lifetimes.put(it, Lifetime.LOCAL)
                    DEBUG_OUTPUT(0) { println("${ir} does not escape") }
                }
            }
        }
    }

    fun computeLifetimes(context: Context, moduleDFG: ModuleDFG, lifetimes: MutableMap<IrElement, Lifetime>) {
        if (!lifetimes.isEmpty()) {
            DEBUG_OUTPUT(0) { println("Lifetimes is not empty!") }
            return
        }
        analyze(context, moduleDFG, lifetimes)
    }
}

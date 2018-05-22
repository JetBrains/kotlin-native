//@SymbolName("Kotlin_konan_internal_undefined")
//internal external fun <T> undefined(): T
//
//class Zef<T> {
//    var element: T = undefined()
//}

//fun main(args: Array<String>) {
//    for (i in 0 until args[0].length)
//        println(args[0][i])
//}
//
//import kotlin.system.measureTimeMillis
//
//fun number(x: Int, y: Int, z: Int, depth: Int): Int {
//    val mask = 1 shl depth
//    if (x and mask != 0) {
//        if (y and mask != 0) {
//            if (z and mask != 0)
//                return 7
//            return 6
//        }
//        if (z and mask != 0)
//            return 5
//        return 4
//    }
//    if (y and mask != 0) {
//        if (z and mask != 0)
//            return 3
//        return 2
//    }
//    if (z and mask != 0)
//        return 1
//    return 0
//}
//
//open class OctoTree<T>(val depth: Int) {
//    private var root: Node<T>? = null
//    private var actual = false
//
//
//    fun get(x: Int, y: Int, z: Int): T? {
//        var dep = depth
//        var iter = root
//        while (true) {
//            if (iter == null) {
//                return null
//            } else if (iter is Node.Leaf) {
//                return iter.value
//            }
//
//            iter = (iter as Node.Branch<T>).nodes[number(x, y, z, --dep)]
//        }
//    }
//
//    fun set(x: Int, y: Int, z: Int, value: T) {
//        if (root == null) {
//            root = Node.Branch()
//        }
//        if (root!!.set(x, y, z, value, depth - 1)) {
//            root = Node.Leaf(value)
//        }
//        actual = false
//    }
//
//    override fun toString(): String = root.toString()
//
//    sealed class Node<T> {
//
//        abstract fun set(x: Int, y: Int, z: Int, value: T, depth: Int): Boolean
//
//        class Leaf<T>(var value: T) : Node<T>() {
//
//            override fun set(x: Int, y: Int, z: Int, value: T, depth: Int): Boolean {
//                throw UnsupportedOperationException("set on Leaf element")
//            }
//
//            override fun toString(): String = "L{$value}"
//        }
//
//        class Branch<T>() : Node<T>() {
//
//
//            constructor(value: T, exclude: Int) : this() {
//
//                var i = 0
//                while (i < 8) {
//                    if (i != exclude) {
//                        nodes[i] = Leaf(value)
//                    }
//                    i++
//                }
//            }
//
//            private fun canClusterize(value: T): Boolean {
//                var i = 0
//                while (i < 8) {
//                    val w = nodes[i]
//                    if (w == null || w !is Leaf || value != w.value) {
//                        return false
//                    }
//                    i++
//                }
//                return true
//            }
//
//            override fun set(x: Int, y: Int, z: Int, value: T, depth: Int): Boolean {
//                val branchIndex = number(x, y, z, depth)
//                val node = nodes[branchIndex]
//                when (node) {
//                    null -> {
//                        if (depth == 0) {
//                            nodes[branchIndex] = Leaf(value)
//                            return canClusterize(value)
//                        } else {
//                            nodes[branchIndex] = Branch()
//                        }
//                    }
//                    is Leaf<T> -> {
//                        if (node.value == value) {
//                            return false
//                        } else if (depth == 0) {
//                            node.value = value
//                            return canClusterize(value)
//                        }
//                        nodes[branchIndex] = Branch(node.value, number(x, y, z, depth - 1))
//                    }
//                }
//
//
//                if (nodes[branchIndex]!!.set(x, y, z, value, depth - 1)) {
//                    nodes[branchIndex] = Leaf(value)
//                    return canClusterize(value)
//                }
//                return false
//            }
//
//            val nodes = arrayOfNulls<Node<T>>(8)
//            override fun toString(): String = nodes.joinToString(prefix = "[", postfix = "]")
//        }
//    }
//}
//
///**
// * Created by semoro on 07.07.17.
// */
//fun main(args: Array<String>) {
//    val time = measureTimeMillis {
//        for (i in 0..10) {
//            //println("Starting cycle")
//            val tree = OctoTree<Boolean>(6)
//            val to = (2 shl tree.depth)
//
//            var x = 0
//            var y = 0
//            var z = 0
//
//            while (x < to) {
//                y = 0
//                while (y < to) {
//                    z = 0
//                    while (z < to) {
//                        val c = (z + to * y + to * to * x) % 2 == 0
//
//                        tree.set(x, y, z, c)
//                        z++
//                    }
//                    y++
//                }
//                x++
//                //println("Fill ${x / to.toDouble() * 100.0}%")
//            }
//
//            //println("Fill done")
//            x = 0
//            y = 0
//            z = 0
//            while (x < to) {
//                y = 0
//                while (y < to) {
//                    z = 0
//                    while (z < to) {
//                        val c = (z + to * y + to * to * x) % 2 == 0
//
//                        val res = tree.get(x, y, z)
//
//                        assert(res == c)
//                        z++
//                    }
//                    y++
//                }
//                x++
//                //println("Get ${x / to.toDouble() * 100.0}%")
//            }
//            //println("get done")
//            //println("Cycle end")
//        }
//    }
//    println("Full time: ${time / 1000L}")
//}

//import kotlinx.cinterop.ByteVar
//import kotlinx.cinterop.allocArray
//import kotlinx.cinterop.memScoped
//import kotlinx.cinterop.toKString
//import platform.posix.*
//import kotlin.system.measureNanoTime
//
//class Node(val id: Int) {
//    val neighbors: MutableMap<Node, Int> = mutableMapOf()
//}
//
//class Graph {
//    val nodes: MutableList<Node> = mutableListOf()
//    val size
//        get() = nodes.size
//
//    fun first(): Node = nodes.first()
//
//    operator fun get(nodeId: Int): Node = nodes.getOrElse(nodeId) { nodes.add(it, Node(it)) ; nodes[it] }
//}
//
//fun parseFile(fileName: String): Graph {
//    val file = fopen(fileName, "r")
//    if (file == null) {
//        perror("cannot open input file $fileName")
//        exit(1)
//    }
//
//    val graph = Graph()
//
//    try {
//        memScoped {
//            fseek(file, 0, SEEK_END)
//            val bufferLength = ftell(file) + 1
//            fseek(file, 0, SEEK_SET)
//            val buffer = allocArray<ByteVar>(bufferLength)
//            if (fread(buffer, 1, bufferLength, file) <= 0) {
//                println("Error while reading")
//                exit(1)
//            }
//
//            val fileContent = buffer.toKString().split("\n")
//            fileContent.filter(String::isNotBlank).forEachIndexed { lineNumber, line ->
//                addNeighbors(line, graph, lineNumber)
//            }
//
//            println("Parsing ended")
//        }
//    } finally {
//        fclose(file)
//    }
//
//    return graph
//}
//
//private data class ClosestNode(val node: Node, val distance: Int)
//
//class Greedy(private val graph: Graph) {
//    private val visitedNodes: MutableSet<Node> = mutableSetOf()
//
//    private fun getClosest(node: Node): ClosestNode? {
//        val nodes = node.neighbors
//                .filter { it.value != 0 && !visitedNodes.contains(it.key) }
//        val clostest = nodes
//                .minBy { it.value }!!
//        return ClosestNode(clostest.key, clostest.value)
//    }
//
//    fun solve() {
//        var previousNode = graph.first()
//        visitedNodes.add(graph.first())
//        var cost = 0
//
//        while (visitedNodes.size != graph.size) {
//            val closest = getClosest(previousNode)!!
//            visitedNodes.add(closest.node)
//            cost += closest.distance
//            previousNode = closest.node
//        }
//
//        cost += visitedNodes.last().neighbors[graph.first()]!!
//        println("Final cost: $cost")
//    }
//}
//
//fun addNeighbors(line: String, graph: Graph, lineNumber: Int): Node {
//    val node = graph[lineNumber]
//    line.split(',').forEachIndexed { index, value ->
//        node.neighbors.put(graph[index], value.trim().toInt())//.trim().toInt())
//    }
//    return node
//}
//
//fun main(args: Array<String>) {
//    var graph: Graph? = null
//    val parsingTime = measureNanoTime {
//        //graph = parseFile("/Users/jetbrains/work/kotlin-native/tsp_exp.txt")
//        graph = parseFile("/Users/jetbrains/Downloads/tsp/small_exp.txt")
//    }
//    println("Parsing time: ${parsingTime / 1_000_000}ms")
//    val elapsed = measureNanoTime {
//        Greedy(graph!!).solve()
//    }
//    println("Solving time: ${elapsed / 1_000_000}ms")
//}

//import konan.worker.*

//data class WorkerArgument(val intParam: Int, val stringParam: String)
//data class WorkerResult(val intResult: Int, val stringResult: String)
//
//fun main(args: Array<String>) {
//    val COUNT = 5
//    val workers = Array(COUNT, { _ -> startWorker()})
//
//    for (attempt in 1 .. 3) {
//        val futures = Array(workers.size, { workerIndex -> workers[workerIndex].schedule(TransferMode.CHECKED, {
//            WorkerArgument(workerIndex, "attempt $attempt") }) { input ->
//            var sum = 0
//            for (i in 0..input.intParam * 1000) {
//                sum += i
//            }
//            WorkerResult(sum, input.stringParam + " result")
//        }
//        })
//        val futureSet = futures.toSet()
//        var consumed = 0
//        while (consumed < futureSet.size) {
//            val ready = futureSet.waitForMultipleFutures(10000)
//            ready.forEach {
//                it.consume { result ->
//                    if (result.stringResult != "attempt $attempt result") throw Error("Unexpected $result")
//                    consumed++ }
//            }
//        }
//    }
//    workers.forEach {
//        it.requestTermination().consume { _ -> }
//    }
//    println("OK")
//}

//data class WorkerArg(val x: Int)
//
//fun main(args: Array<String>) {
//    val worker = startWorker()
//
//        Array(1, { workerIndex -> worker.schedule(TransferMode.CHECKED, {
//            WorkerArg(workerIndex) }) { input -> input } })
//}

//fun main(args : Array<String>) {
//  for (s in args) {
//      println(s)
//  }
//}

//////import kotlin.system.measureNanoTime
////
////fun main(args: Array<String>) {
////    (0..10_000)
////            .asSequence()
////            .filter { it % 3 == 2 }
////            .sumBy { it }
////}
////
//////import kotlin.system.measureNanoTime
//////
//////fun main(args: Array<String>) {
//////    //val arr = IntArray(10_000) { it }
//////    val elapsed = measureNanoTime {
//////        var s = 0L
//////        //for (i in 1..3000) {
//////            s += (0..10_000)
//////                    .asSequence()
//////                    .filter { it % 3 == 2 }
//////                    //.map { (it * it).toLong() }
//////                    .sumBy { it }
//////        //}
//////        println(s)
//////    }
//////    //println(elapsed / 1_000_000)
//////}
////
//
//
//
////data class Node(val data: Int, var next: Node?, var prev: Node?, val outer: Node?)
////
////fun makeCycle(len: Int, outer: Node?): Node {
////    val start = Node(0, null, null, outer)
////    start.next = start
////    return start
////}
////
////fun createCycles(junk: Node) {
////    val cycle1 = makeCycle(1, junk)
////}
////
////fun main(args: Array<String>) {
////    // Create outer link from cyclic garbage.
////    val outer = Node(42, null, null, null)
////    createCycles(outer)
////    konan.internal.GC.collect()
////    // Ensure outer is not collected.
////    println(outer.data)
////}
//
////fun main(args : Array<String>) {
////  for (s in args) {
////      println(s)
////  }
////}
//
//import kotlin.system.measureNanoTime
//
//val BENCHMARK_SIZE = 100
//
////-----------------------------------------------------------------------------//
//
//class Launcher(val numWarmIterations: Int, val numMeasureIterations: Int) {
//    val results = mutableMapOf<String, Long>()
//
//    fun launch(benchmark: () -> Any?, coeff: Double = 1.0): Long {                          // If benchmark runs too long - use coeff to speed it up.
//        var i = (numWarmIterations * coeff).toInt()
//        var j = (numMeasureIterations * coeff).toInt()
//
//        while (i-- > 0) benchmark()
//        val time = measureNanoTime {
//        println("START")
//            while (j-- > 0) {
////                if (j % 1000 == 0)
////                    println("ZZZ")
//                benchmark()
//            }
//        println("END")
//        }
//
//        return (time / numMeasureIterations / coeff).toLong()
//        //return 0L
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runBenchmarks() {
//        runLoopBenchmark()
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runLoopBenchmark() {
//        val benchmark = LoopBenchmark()
//        benchmark.setup()
//
////        results["Loop.arrayLoop"]            = launch(benchmark::arrayLoop)
////        results["Loop.arrayIndexLoop"]       = launch(benchmark::arrayIndexLoop)
////        results["Loop.rangeLoop"]            = launch(benchmark::rangeLoop)
////        results["Loop.arrayListLoop"]        = launch(benchmark::arrayListLoop)
////        results["Loop.arrayWhileLoop"]       = launch(benchmark::arrayWhileLoop)
//        //results["Loop.arrayForeachLoop"]     = launch(benchmark::arrayForeachLoop)
//        println(launch(benchmark::arrayWhileLoop))
////        results["Loop.arrayListForeachLoop"] = launch(benchmark::arrayListForeachLoop)
//    }
//
//}
//
////-----------------------------------------------------------------------------//
//
//class Blackhole {
//    companion object {
//        var consumer = 0
//        fun consume(value: Any) {
//            consumer += value.hashCode()
//        }
//    }
//}
//
//fun classValues(size: Int): Iterable<Value> {
//    return intValues(size).map { Value(it) }
//}
//
//fun stringValues(size: Int): Iterable<String> {
//    return intValues(size).map { it.toString() }
//}
//
//fun intValues(size: Int): Iterable<Int> {
//    return 1..size
//}
//
//open class Value(var value: Int) {
//    val text = value.toString().reversed()
//}
//
////fun filterLoad(v: Value): Boolean {
////    return v.value.toString() in v.text
////}
////
////fun mapLoad(v: Value): String = v.text.reversed()
////
////fun filterLoad(v: Int): Boolean {
////    return v.toString() in "0123456789"
////}
////
////fun mapLoad(v: Int): String = v.toString()
////
////fun filterSome(v: Int): Boolean = v % 7 == 0 || v % 11 == 0
////
////fun filterPrime(v: Int): Boolean {
////    if (v <= 1)
////        return false
////    if (v <= 3)
////        return true
////    if (v % 2 == 0)
////        return false
////    var i = 3
////    while (i*i <= v) {
////        if (v % i == 0)
////            return false
////        i += 2
////    }
////    return true
////}
////
////inline fun Array<Value>.cnt(predicate: (Value) -> Boolean): Int {
////    var count = 0
////    for (element in this) {
////        if (predicate(element))
////            count++
////    }
////    return count
////}
////
////inline fun IntArray.cnt(predicate: (Int) -> Boolean): Int {
////    var count = 0
////    for (element in this) {
////        if (predicate(element))
////            count++
////    }
////    return count
////}
////
////inline fun Iterable<Int>.cnt(predicate: (Int) -> Boolean): Int {
////    var count = 0
////    for (element in this) {
////        if (predicate(element))
////            count++
////    }
////    return count
////}
////
////inline fun Sequence<Int>.cnt(predicate: (Int) -> Boolean): Int {
////    var count = 0
////    for (element in this) {
////        if (predicate(element))
////            count++
////    }
////    return count
////}
//
//open class LoopBenchmark {
//    lateinit var arrayList: List<Value>
//    lateinit var array: Array<Value>
//
//    fun setup() {
////        val list = ArrayList<Value>(BENCHMARK_SIZE)
////        for (n in classValues(BENCHMARK_SIZE))
////            list.add(n)
////        arrayList = list
////        array = list.toTypedArray()
//        array = Array<Value>(BENCHMARK_SIZE) { Value(it) }
//    }
//
////    //Benchmark
////    fun arrayLoop() {
////        for (x in array) {
////            Blackhole.consume(x)
////        }
////    }
////
////    //Benchmark
////    fun arrayIndexLoop() {
////        for (i in array.indices) {
////            Blackhole.consume(array[i])
////        }
////    }
////
////    //Benchmark
////    fun rangeLoop() {
////        for (i in 0..BENCHMARK_SIZE) {
////            Blackhole.consume(i)
////        }
////    }
////
////    //Benchmark
////    fun arrayListLoop() {
////        for (x in arrayList) {
////            Blackhole.consume(x)
////        }
////    }
//
//    //Benchmark
//    fun arrayWhileLoop() {
//        var i = 0
//        val s = array.size
//        while (i < s) {
//            Blackhole.consume(array[i])
//            i++
//        }
//    }
//
////    //Benchmark
////    fun arrayForeachLoop() {
////        array.forEach { Blackhole.consume(it) }
////    }
//
////    //Benchmark
////    fun arrayListForeachLoop() {
////        arrayList.forEach { Blackhole.consume(it) }
////    }
//}
//
//fun main(args: Array<String>) {
////        val benchmark = LoopBenchmark()
////        benchmark.setup()
////    benchmark.arrayForeachLoop()
//// //    val array = Array(2) { Value(it) }
//// //    array.forEach { Blackhole.consume(it) }
//    var numWarmIterations    =  1000       // Should be 100000 for jdk based run
//    var numMeasureIterations =  5000000
//
//    if (args.size == 2) {
//        numWarmIterations    = args[0].toInt()
//        numMeasureIterations = args[1].toInt()
//    }
//
//    println("Ring starting")
//    println("  warmup  iterations count: $numWarmIterations")
//    println("  measure iterations count: $numMeasureIterations")
//    Launcher(numWarmIterations, numMeasureIterations).runBenchmarks()
//}































import kotlin.system.measureNanoTime
import konan.internal.GC

fun cleanup() { GC.collect() }

class Random() {
    companion object {
        var seedInt = 0
        fun nextInt(boundary: Int = 100): Int {
            seedInt = (3 * seedInt + 11) % boundary
            return seedInt
        }

        var seedDouble: Double = 0.1
        fun nextDouble(boundary: Double = 100.0): Double {
            seedDouble = (7.0 * seedDouble + 7.0) % boundary
            return seedDouble
        }
    }
}

class Blackhole {
    companion object {
        var consumer = 0
        fun consume(value: Any) {
            consumer += value.hashCode()
        }
    }
}

fun classValues(size: Int): Iterable<Value> {
    return intValues(size).map { Value(it) }
}

fun stringValues(size: Int): Iterable<String> {
    return intValues(size).map { it.toString() }
}

fun intValues(size: Int): Iterable<Int> {
    return 1..size
}

open class Value(var value: Int) {
    val text = value.toString().reversed()
}

fun filterLoad(v: Value): Boolean {
    return v.value.toString() in v.text
}

fun mapLoad(v: Value): String = v.text.reversed()

fun filterLoad(v: Int): Boolean {
    return v.toString() in "0123456789"
}

fun mapLoad(v: Int): String = v.toString()

fun filterSome(v: Int): Boolean = v % 7 == 0 || v % 11 == 0

fun filterPrime(v: Int): Boolean {
    if (v <= 1)
        return false
    if (v <= 3)
        return true
    if (v % 2 == 0)
        return false
    var i = 3
    while (i*i <= v) {
        if (v % i == 0)
            return false
        i += 2
    }
    return true
}

inline fun Array<Value>.cnt(predicate: (Value) -> Boolean): Int {
    var count = 0
    for (element in this) {
        if (predicate(element))
            count++
    }
    return count
}

inline fun IntArray.cnt(predicate: (Int) -> Boolean): Int {
    var count = 0
    for (element in this) {
        if (predicate(element))
            count++
    }
    return count
}

inline fun Iterable<Int>.cnt(predicate: (Int) -> Boolean): Int {
    var count = 0
    for (element in this) {
        if (predicate(element))
            count++
    }
    return count
}

inline fun Sequence<Int>.cnt(predicate: (Int) -> Boolean): Int {
    var count = 0
    for (element in this) {
        if (predicate(element))
            count++
    }
    return count
}



val BENCHMARK_SIZE = 100

fun load(value: Int, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

inline fun loadInline(value: Int, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

fun <T: Any> loadGeneric(value: T, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

inline fun <T: Any> loadGenericInline(value: T, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

open class InlineBenchmark {
    private var value = 2138476523

    //Benchmark
    fun calculate(): Int {
        return load(value, BENCHMARK_SIZE)
    }

    //Benchmark
    fun calculateInline(): Int {
        return loadInline(value, BENCHMARK_SIZE)
    }

    //Benchmark
    fun calculateGeneric(): Int {
        return loadGeneric(value, BENCHMARK_SIZE)
    }

    //Benchmark
    fun calculateGenericInline(): Int {
        return loadGenericInline(value, BENCHMARK_SIZE)
    }
}

var globalAddendum = 0

open class LambdaBenchmark {
    private inline fun <T> runLambda(x: () -> T): T = x()
    private fun <T> runLambdaNoInline(x: () -> T): T = x()

    fun setup() {
        globalAddendum = Random.nextInt(20)
    }

    //Benchmark
    fun noncapturingLambda(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda { globalAddendum }
        }
        return x
    }

    //Benchmark
    fun noncapturingLambdaNoInline(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline { globalAddendum }
        }
        return x
    }

    //Benchmark
    fun capturingLambda(): Int {
        val addendum = globalAddendum + 1
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda { addendum }
        }
        return x
    }

    //Benchmark
    fun capturingLambdaNoInline(): Int {
        val addendum = globalAddendum + 1
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline { addendum }
        }
        return x
    }

    //Benchmark
    fun mutatingLambda(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            runLambda { x += globalAddendum }
        }
        return x
    }

    //Benchmark
    fun mutatingLambdaNoInline(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            runLambdaNoInline { x += globalAddendum }
        }
        return x
    }

    //Benchmark
    fun methodReference(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambda(::referenced)
        }
        return x
    }

    //Benchmark
    fun methodReferenceNoInline(): Int {
        var x: Int = 0
        for (i in 0..BENCHMARK_SIZE) {
            x += runLambdaNoInline(::referenced)
        }
        return x
    }
}

private fun referenced(): Int {
    return globalAddendum
}

//-----------------------------------------------------------------------------//

val SPARSE_SWITCH_CASES = intArrayOf(11, 29, 47, 71, 103,
        149, 175, 227, 263, 307,
        361, 487, 563, 617, 677,
        751, 823, 883, 967, 1031)

const val V1 = 1
const val V2 = 2
const val V3 = 3
const val V4 = 4
const val V5 = 5
const val V6 = 6
const val V7 = 7
const val V8 = 8
const val V9 = 9
const val V10 = 10
const val V11 = 11
const val V12 = 12
const val V13 = 13
const val V14 = 14
const val V15 = 15
const val V16 = 16
const val V17 = 17
const val V18 = 18
const val V19 = 19
const val V20 = 20


object Numbers {
    const val V1 = 1
    const val V2 = 2
    const val V3 = 3
    const val V4 = 4
    const val V5 = 5
    const val V6 = 6
    const val V7 = 7
    const val V8 = 8
    const val V9 = 9
    const val V10 = 10
    const val V11 = 11
    const val V12 = 12
    const val V13 = 13
    const val V14 = 14
    const val V15 = 15
    const val V16 = 16
    const val V17 = 17
    const val V18 = 18
    const val V19 = 19
    const val V20 = 20
}

var VV1 = 1
var VV2 = 2
var VV3 = 3
var VV4 = 4
var VV5 = 5
var VV6 = 6
var VV7 = 7
var VV8 = 8
var VV9 = 9
var VV10 = 10
var VV11 = 11
var VV12 = 12
var VV13 = 13
var VV14 = 14
var VV15 = 15
var VV16 = 16
var VV17 = 17
var VV18 = 18
var VV19 = 19
var VV20 = 20

open class SwitchBenchmark {
    fun sparseIntSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            11 -> {
                t = 1
            }
            29 -> {
                t = 2
            }
            47 -> {
                t = 3
            }
            71 -> {
                t = 4
            }
            103 -> {
                t = 5
            }
            149 -> {
                t = 6
            }
            175 -> {
                t = 7
            }
            227 -> {
                t = 1
            }
            263 -> {
                t = 9
            }
            307 -> {
                t = 1
            }
            361 -> {
                t = 2
            }
            487 -> {
                t = 3
            }
            563 -> {
                t = 4
            }
            617 -> {
                t = 4
            }
            677 -> {
                t = 4
            }
            751 -> {
                t = 435
            }
            823 -> {
                t = 31
            }
            883 -> {
                t = 1
            }
            967 -> {
                t = 1
            }
            1031 -> {
                t = 1
            }
            20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun denseIntSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            1 -> {
                t = 1
            }
            -1 -> {
                t = 2
            }
            2 -> {
                t = 3
            }
            3 -> {
                t = 4
            }
            4 -> {
                t = 5
            }
            5 -> {
                t = 6
            }
            6 -> {
                t = 7
            }
            7 -> {
                t = 1
            }
            8 -> {
                t = 9
            }
            9 -> {
                t = 1
            }
            10 -> {
                t = 2
            }
            11 -> {
                t = 3
            }
            12 -> {
                t = 4
            }
            13 -> {
                t = 4
            }
            14 -> {
                t = 4
            }
            15 -> {
                t = 435
            }
            16 -> {
                t = 31
            }
            17 -> {
                t = 1
            }
            18 -> {
                t = 1
            }
            19 -> {
                t = 1
            }
            20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun constSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            V1 -> {
                t = 1
            }
            V2 -> {
                t = 3
            }
            V3 -> {
                t = 4
            }
            V4 -> {
                t = 5
            }
            V5 -> {
                t = 6
            }
            V6 -> {
                t = 7
            }
            V7 -> {
                t = 1
            }
            V8 -> {
                t = 9
            }
            V9 -> {
                t = 1
            }
            V10 -> {
                t = 2
            }
            V11 -> {
                t = 3
            }
            V12 -> {
                t = 4
            }
            V13 -> {
                t = 4
            }
            V14 -> {
                t = 4
            }
            V15 -> {
                t = 435
            }
            V16 -> {
                t = 31
            }
            V17 -> {
                t = 1
            }
            V18 -> {
                t = 1
            }
            V19 -> {
                t = 1
            }
            V20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun objConstSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            Numbers.V1 -> {
                t = 1
            }
            Numbers.V2 -> {
                t = 3
            }
            Numbers.V3 -> {
                t = 4
            }
            Numbers.V4 -> {
                t = 5
            }
            Numbers.V5 -> {
                t = 6
            }
            Numbers.V6 -> {
                t = 7
            }
            Numbers.V7 -> {
                t = 1
            }
            Numbers.V8 -> {
                t = 9
            }
            Numbers.V9 -> {
                t = 1
            }
            Numbers.V10 -> {
                t = 2
            }
            Numbers.V11 -> {
                t = 3
            }
            Numbers.V12 -> {
                t = 4
            }
            Numbers.V13 -> {
                t = 4
            }
            Numbers.V14 -> {
                t = 4
            }
            Numbers.V15 -> {
                t = 435
            }
            Numbers.V16 -> {
                t = 31
            }
            Numbers.V17 -> {
                t = 1
            }
            Numbers.V18 -> {
                t = 1
            }
            Numbers.V19 -> {
                t = 1
            }
            Numbers.V20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun varSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            VV1 -> {
                t = 1
            }
            VV2 -> {
                t = 3
            }
            VV3 -> {
                t = 4
            }
            VV4 -> {
                t = 5
            }
            VV5 -> {
                t = 6
            }
            VV6 -> {
                t = 7
            }
            VV7 -> {
                t = 1
            }
            VV8 -> {
                t = 9
            }
            VV9 -> {
                t = 1
            }
            VV10 -> {
                t = 2
            }
            VV11 -> {
                t = 3
            }
            VV12 -> {
                t = 4
            }
            VV13 -> {
                t = 4
            }
            VV14 -> {
                t = 4
            }
            VV15 -> {
                t = 435
            }
            VV16 -> {
                t = 31
            }
            VV17 -> {
                t = 1
            }
            VV18 -> {
                t = 1
            }
            VV19 -> {
                t = 1
            }
            VV20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    private fun stringSwitch(s: String) : Int {
        when(s) {
            "ABCDEFG1" -> return 1
            "ABCDEFG2" -> return 2
            "ABCDEFG2" -> return 3
            "ABCDEFG3" -> return 4
            "ABCDEFG4" -> return 5
            "ABCDEFG5" -> return 6
            "ABCDEFG6" -> return 7
            "ABCDEFG7" -> return 8
            "ABCDEFG8" -> return 9
            "ABCDEFG9" -> return 10
            "ABCDEFG10" -> return 11
            "ABCDEFG11" -> return 12
            "ABCDEFG12" -> return 1
            "ABCDEFG13" -> return 2
            "ABCDEFG14" -> return 3
            "ABCDEFG15" -> return 4
            "ABCDEFG16" -> return 5
            "ABCDEFG17" -> return 6
            "ABCDEFG18" -> return 7
            "ABCDEFG19" -> return 8
            "ABCDEFG20" -> return 9
            else -> return -1
        }
    }

    lateinit var denseIntData: IntArray
    lateinit var sparseIntData: IntArray

    fun setupInts() {
        denseIntData = IntArray(BENCHMARK_SIZE) { Random.nextInt(25) - 1 }
        sparseIntData= IntArray(BENCHMARK_SIZE) { SPARSE_SWITCH_CASES[Random.nextInt(20)] }
    }

    //Benchmark
    fun testSparseIntSwitch() {
        for (i in sparseIntData) {
            Blackhole.consume(sparseIntSwitch(i))
        }
    }

    //Benchmark
    fun testDenseIntSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(denseIntSwitch(i))
        }
    }

    //Benchmark
    fun testConstSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(constSwitch(i))
        }
    }

    //Benchmark
    fun testObjConstSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(objConstSwitch(i))
        }
    }

    //Benchmark
    fun testVarSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(varSwitch(i))
        }
    }

    var data : Array<String> = arrayOf()

    fun setupStrings() {
        data = Array(BENCHMARK_SIZE) {
            "ABCDEFG" + Random.nextInt(22)
        }
    }

    //Benchmark
    fun testStringsSwitch() {
        val n = data.size
        for (s in data) {
            Blackhole.consume(stringSwitch(s))
        }
    }

    enum class MyEnum {
        ITEM1, ITEM2, ITEM3, ITEM4, ITEM5, ITEM6, ITEM7, ITEM8, ITEM9, ITEM10, ITEM11, ITEM12, ITEM13, ITEM14, ITEM15, ITEM16, ITEM17, ITEM18, ITEM19, ITEM20, ITEM21, ITEM22, ITEM23, ITEM24, ITEM25, ITEM26, ITEM27, ITEM28, ITEM29, ITEM30, ITEM31, ITEM32, ITEM33, ITEM34, ITEM35, ITEM36, ITEM37, ITEM38, ITEM39, ITEM40, ITEM41, ITEM42, ITEM43, ITEM44, ITEM45, ITEM46, ITEM47, ITEM48, ITEM49, ITEM50, ITEM51, ITEM52, ITEM53, ITEM54, ITEM55, ITEM56, ITEM57, ITEM58, ITEM59, ITEM60, ITEM61, ITEM62, ITEM63, ITEM64, ITEM65, ITEM66, ITEM67, ITEM68, ITEM69, ITEM70, ITEM71, ITEM72, ITEM73, ITEM74, ITEM75, ITEM76, ITEM77, ITEM78, ITEM79, ITEM80, ITEM81, ITEM82, ITEM83, ITEM84, ITEM85, ITEM86, ITEM87, ITEM88, ITEM89, ITEM90, ITEM91, ITEM92, ITEM93, ITEM94, ITEM95, ITEM96, ITEM97, ITEM98, ITEM99, ITEM100
    }

    private fun enumSwitch(x: MyEnum) : Int {
        when (x) {
            MyEnum.ITEM5 -> return 1
            MyEnum.ITEM10 -> return 2
            MyEnum.ITEM15 -> return 3
            MyEnum.ITEM20 -> return 4
            MyEnum.ITEM25 -> return 5
            MyEnum.ITEM30 -> return 6
            MyEnum.ITEM35 -> return 7
            MyEnum.ITEM40 -> return 8
            MyEnum.ITEM45 -> return 9
            MyEnum.ITEM50 -> return 10
            MyEnum.ITEM55 -> return 11
            MyEnum.ITEM60 -> return 12
            MyEnum.ITEM65 -> return 13
            MyEnum.ITEM70 -> return 14
            MyEnum.ITEM75 -> return 15
            MyEnum.ITEM80 -> return 16
            MyEnum.ITEM85 -> return 17
            MyEnum.ITEM90 -> return 18
            MyEnum.ITEM95 -> return 19
            MyEnum.ITEM100 -> return 20
            else -> return -1
        }
    }

    private fun denseEnumSwitch(x: MyEnum) : Int {
        when (x) {
            MyEnum.ITEM1 -> return 1
            MyEnum.ITEM2 -> return 2
            MyEnum.ITEM3 -> return 3
            MyEnum.ITEM4 -> return 4
            MyEnum.ITEM5 -> return 5
            MyEnum.ITEM6 -> return 6
            MyEnum.ITEM7 -> return 7
            MyEnum.ITEM8 -> return 8
            MyEnum.ITEM9 -> return 9
            MyEnum.ITEM10 -> return 10
            MyEnum.ITEM11 -> return 11
            MyEnum.ITEM12 -> return 12
            MyEnum.ITEM13 -> return 13
            MyEnum.ITEM14 -> return 14
            MyEnum.ITEM15 -> return 15
            MyEnum.ITEM16 -> return 16
            MyEnum.ITEM17 -> return 17
            MyEnum.ITEM18 -> return 18
            MyEnum.ITEM19 -> return 19
            MyEnum.ITEM20 -> return 20
            else -> return -1
        }
    }

    lateinit var enumData : Array<MyEnum>
    lateinit var denseEnumData : Array<MyEnum>

    fun setupEnums() {
        enumData = Array(BENCHMARK_SIZE) {
            MyEnum.values()[it % MyEnum.values().size]
        }
        denseEnumData = Array(BENCHMARK_SIZE) {
            MyEnum.values()[it % 20]
        }
    }

    //Benchmark
    fun testEnumsSwitch() {
        val n = enumData.size -1
        val data = enumData
        for (i in 0..n) {
            Blackhole.consume(enumSwitch(data[i]))
        }
    }

    //Benchmark
    fun testDenseEnumsSwitch() {
        val n = denseEnumData.size -1
        val data = denseEnumData
        for (i in 0..n) {
            Blackhole.consume(denseEnumSwitch(data[i]))
        }
    }

    sealed class MySealedClass {
        class MySealedClass1: MySealedClass()
        class MySealedClass2: MySealedClass()
        class MySealedClass3: MySealedClass()
        class MySealedClass4: MySealedClass()
        class MySealedClass5: MySealedClass()
        class MySealedClass6: MySealedClass()
        class MySealedClass7: MySealedClass()
        class MySealedClass8: MySealedClass()
        class MySealedClass9: MySealedClass()
        class MySealedClass10: MySealedClass()
    }

    lateinit var sealedClassData: Array<MySealedClass>

    fun setupSealedClassses() {
        sealedClassData = Array(BENCHMARK_SIZE) {
            when(Random.nextInt(10)) {
                0 -> MySealedClass.MySealedClass1()
                1 -> MySealedClass.MySealedClass2()
                2 -> MySealedClass.MySealedClass3()
                3 -> MySealedClass.MySealedClass4()
                4 -> MySealedClass.MySealedClass5()
                5 -> MySealedClass.MySealedClass6()
                6 -> MySealedClass.MySealedClass7()
                7 -> MySealedClass.MySealedClass8()
                8 -> MySealedClass.MySealedClass9()
                9 -> MySealedClass.MySealedClass10()
                else -> throw IllegalStateException()
            }
        }
    }

    private fun sealedWhenSwitch(x: MySealedClass) : Int =
            when (x) {
                is MySealedClass.MySealedClass1 -> 1
                is MySealedClass.MySealedClass2 -> 2
                is MySealedClass.MySealedClass3 -> 3
                is MySealedClass.MySealedClass4 -> 4
                is MySealedClass.MySealedClass5 -> 5
                is MySealedClass.MySealedClass6 -> 6
                is MySealedClass.MySealedClass7 -> 7
                is MySealedClass.MySealedClass8 -> 8
                is MySealedClass.MySealedClass9 -> 9
                is MySealedClass.MySealedClass10 -> 10
            }


    //Benchmark
    fun testSealedWhenSwitch() {
        val n = sealedClassData.size -1
        for (i in 0..n) {
            Blackhole.consume(sealedWhenSwitch(sealedClassData[i]))
        }
    }
}

//-----------------------------------------------------------------------------//

open class ClassListBenchmark {
    private var _data: ArrayList<Value>? = null
    val data: ArrayList<Value>
        get() = _data!!

    fun setup() {
        val list = ArrayList<Value>(BENCHMARK_SIZE)
        for (n in classValues(BENCHMARK_SIZE))
            list.add(n)
        _data = list
    }

    //Benchmark
    fun copy(): List<Value> {
        return data.toList()
    }

    //Benchmark
    fun copyManual(): List<Value> {
        val list = ArrayList<Value>(data.size)
        for (item in data) {
            list.add(item)
        }
        return list
    }

    //Benchmark
    fun filterAndCount(): Int {
        return data.filter { filterLoad(it) }.count()
    }

    //Benchmark
    fun filterAndCountWithLambda(): Int {
        return data.filter { it.value % 2 == 0 }.count()
    }

    //Benchmark
    fun filterWithLambda(): List<Value> {
        return data.filter { it.value % 2 == 0 }
    }

    //Benchmark
    fun mapWithLambda(): List<String> {
        return data.map { it.toString() }
    }

    //Benchmark
    fun countWithLambda(): Int {
        return data.count { it.value % 2 == 0 }
    }

    //Benchmark
    fun filterAndMapWithLambda(): List<String> {
        return data.filter { it.value % 2 == 0 }.map { it.toString() }
    }

    //Benchmark
    fun filterAndMapWithLambdaAsSequence(): List<String> {
        return data.asSequence().filter { it.value % 2 == 0 }.map { it.toString() }.toList()
    }

    //Benchmark
    fun filterAndMap(): List<String> {
        return data.filter { filterLoad(it) }.map { mapLoad(it) }
    }

    //Benchmark
    fun filterAndMapManual(): ArrayList<String> {
        val list = ArrayList<String>()
        for (it in data) {
            if (filterLoad(it)) {
                val value = mapLoad(it)
                list.add(value)
            }
        }
        return list
    }

    //Benchmark
    fun filter(): List<Value> {
        return data.filter { filterLoad(it) }
    }

    //Benchmark
    fun filterManual(): List<Value> {
        val list = ArrayList<Value>()
        for (it in data) {
            if (filterLoad(it))
                list.add(it)
        }
        return list
    }

    //Benchmark
    fun countFilteredManual(): Int {
        var count = 0
        for (it in data) {
            if (filterLoad(it))
                count++
        }
        return count
    }

    //Benchmark
    fun countFiltered(): Int {
        return data.count { filterLoad(it) }
    }

    //Benchmark
//    fun countFilteredLocal(): Int {
//        return data.cnt { filterLoad(it) }
//    }

    //Benchmark
    fun reduce(): Int {
        return data.fold(0) { acc, it -> if (filterLoad(it)) acc + 1 else acc }
    }
}

//-----------------------------------------------------------------------------//

class Launcher(val numWarmIterations: Int) {
    class Results(val mean: Double, val variance: Double)

    val results = mutableMapOf<String, Results>()

    fun launch(benchmark: () -> Any?): Results {                          // If benchmark runs too long - use coeff to speed it up.
        var i = numWarmIterations

        while (i-- > 0) benchmark()
        cleanup()

        var autoEvaluatedNumberOfMeasureIteration = 1
        while (true) {
            var j = autoEvaluatedNumberOfMeasureIteration
            val time = measureNanoTime {
                while (j-- > 0) {
                    benchmark()
                }
                cleanup()
            }
            if (time >= 100L * 1_000_000) // 100ms
                break
            autoEvaluatedNumberOfMeasureIteration *= 2
        }

        val attempts = 100
        val samples = DoubleArray(attempts)
        for (k in samples.indices) {
            i = autoEvaluatedNumberOfMeasureIteration
            val time = measureNanoTime {
                while (i-- > 0) {
                    benchmark()
                }
                cleanup()
            }
            samples[k] = time * 1.0 / autoEvaluatedNumberOfMeasureIteration
        }
        val mean = samples.sum() / attempts
        val variance = samples.indices.sumByDouble { (samples[it] - mean) * (samples[it] - mean) } / attempts

        return Results(mean, variance)
    }

    //-------------------------------------------------------------------------//

    fun runBenchmarks() {
//        runAbstractMethodBenchmark()
//        runClassArrayBenchmark()
//        runClassBaselineBenchmark()
//        runClassListBenchmark()
//        runClassStreamBenchmark()
//        runCompanionObjectBenchmark()
//        runDefaultArgumentBenchmark()
//        runElvisBenchmark()
//        runEulerBenchmark()
//        runFibonacciBenchmark()
        runInlineBenchmark()
//        runIntArrayBenchmark()
//        runIntBaselineBenchmark()
//        runIntListBenchmark()
//        runIntStreamBenchmark()
//        runLambdaBenchmark()
//        runLoopBenchmark()
//        runMatrixMapBenchmark()
//        runParameterNotNullAssertionBenchmark()
//        runPrimeListBenchmark()
//        runStringBenchmark()
//        runSwitchBenchmark()
//        runWithIndiciesBenchmark()
//        runOctoTest()
//
        printResultsNormalized()
    }

    //-------------------------------------------------------------------------//

    fun printResultsAsTime() {
        results.forEach {
            val niceName = "\"${it.key}\"".padEnd(51)
            val niceTime = "${it.value}".padStart(10)
            println("    $niceName to ${niceTime}L,")
        }
    }

    //-------------------------------------------------------------------------//

    fun printResultsNormalized() {
        var totalMean = 0.0
        var totalVariance = 0.0
        results.asSequence().sortedBy { it.key }.forEach {
            val niceName  = it.key.padEnd(50, ' ')
            println("$niceName : ${it.value.mean.toString(9)} : ${kotlin.math.sqrt(it.value.variance).toString(9)}")

            totalMean += it.value.mean
            totalVariance += it.value.variance
        }
        val averageMean = totalMean / results.size
        val averageStdDev = kotlin.math.sqrt(totalVariance) / results.size
        println("\nRingAverage: ${averageMean.toString(9)} : ${averageStdDev.toString(9)}")
    }

//    //-------------------------------------------------------------------------//
//
//    fun runAbstractMethodBenchmark() {
//        val benchmark = AbstractMethodBenchmark()
//
//        results["AbstractMethod.sortStrings"]               = launch(benchmark::sortStrings)
//        results["AbstractMethod.sortStringsWithComparator"] = launch(benchmark::sortStringsWithComparator)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runClassArrayBenchmark() {
//        val benchmark = ClassArrayBenchmark()
//        benchmark.setup()
//
//        results["ClassArray.copy"]                = launch(benchmark::copy)
//        results["ClassArray.copyManual"]          = launch(benchmark::copyManual)
//        results["ClassArray.filterAndCount"]      = launch(benchmark::filterAndCount)
//        results["ClassArray.filterAndMap"]        = launch(benchmark::filterAndMap)
//        results["ClassArray.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
//        results["ClassArray.filter"]              = launch(benchmark::filter)
//        results["ClassArray.filterManual"]        = launch(benchmark::filterManual)
//        results["ClassArray.countFilteredManual"] = launch(benchmark::countFilteredManual)
//        results["ClassArray.countFiltered"]       = launch(benchmark::countFiltered)
//        results["ClassArray.countFilteredLocal"]  = launch(benchmark::countFilteredLocal)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runClassBaselineBenchmark() {
//        val benchmark = ClassBaselineBenchmark()
//
//        results["ClassBaseline.consume"]              = launch(benchmark::consume)
//        results["ClassBaseline.consumeField"]         = launch(benchmark::consumeField)
//        results["ClassBaseline.allocateList"]         = launch(benchmark::allocateList)
//        results["ClassBaseline.allocateArray"]        = launch(benchmark::allocateArray)
//        results["ClassBaseline.allocateListAndFill"]  = launch(benchmark::allocateListAndFill)
//        results["ClassBaseline.allocateListAndWrite"] = launch(benchmark::allocateListAndWrite)
//        results["ClassBaseline.allocateArrayAndFill"] = launch(benchmark::allocateArrayAndFill)
//    }
//
    //-------------------------------------------------------------------------//

    fun runClassListBenchmark() {
        val benchmark = ClassListBenchmark()
        benchmark.setup()

        results["ClassList.copy"]                             = launch(benchmark::copy)
//        results["ClassList.copyManual"]                       = launch(benchmark::copyManual)
//        results["ClassList.filterAndCount"]                   = launch(benchmark::filterAndCount)
//        results["ClassList.filterAndCountWithLambda"]         = launch(benchmark::filterAndCountWithLambda)
//        results["ClassList.filterWithLambda"]                 = launch(benchmark::filterWithLambda)
//        results["ClassList.mapWithLambda"]                    = launch(benchmark::mapWithLambda)
//        results["ClassList.countWithLambda"]                  = launch(benchmark::countWithLambda)
//        results["ClassList.filterAndMapWithLambda"]           = launch(benchmark::filterAndMapWithLambda)
//        results["ClassList.filterAndMapWithLambdaAsSequence"] = launch(benchmark::filterAndMapWithLambdaAsSequence)
//        results["ClassList.filterAndMap"]                     = launch(benchmark::filterAndMap)
//        results["ClassList.filterAndMapManual"]               = launch(benchmark::filterAndMapManual)
//        results["ClassList.filter"]                           = launch(benchmark::filter)
//        results["ClassList.filterManual"]                     = launch(benchmark::filterManual)
//        results["ClassList.countFilteredManual"]              = launch(benchmark::countFilteredManual)
//        results["ClassList.countFiltered"]                    = launch(benchmark::countFiltered)
//        results["ClassList.reduce"]                           = launch(benchmark::reduce)
    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runClassStreamBenchmark() {
//        val benchmark = ClassStreamBenchmark()
//        benchmark.setup()
//
//        results["ClassStream.copy"]                = launch(benchmark::copy)
//        results["ClassStream.copyManual"]          = launch(benchmark::copyManual)
//        results["ClassStream.filterAndCount"]      = launch(benchmark::filterAndCount)
//        results["ClassStream.filterAndMap"]        = launch(benchmark::filterAndMap)
//        results["ClassStream.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
//        results["ClassStream.filter"]              = launch(benchmark::filter)
//        results["ClassStream.filterManual"]        = launch(benchmark::filterManual)
//        results["ClassStream.countFilteredManual"] = launch(benchmark::countFilteredManual)
//        results["ClassStream.countFiltered"]       = launch(benchmark::countFiltered)
//        results["ClassStream.reduce"]              = launch(benchmark::reduce)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runCompanionObjectBenchmark() {
//        val benchmark = CompanionObjectBenchmark()
//
//        results["CompanionObject.invokeRegularFunction"]   = launch(benchmark::invokeRegularFunction)
//        results["CompanionObject.invokeJvmStaticFunction"] = launch(benchmark::invokeJvmStaticFunction)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runDefaultArgumentBenchmark() {
//        val benchmark = DefaultArgumentBenchmark()
//        benchmark.setup()
//
//        results["DefaultArgument.testOneOfTwo"]     = launch(benchmark::testOneOfTwo)
//        results["DefaultArgument.testTwoOfTwo"]     = launch(benchmark::testTwoOfTwo)
//        results["DefaultArgument.testOneOfFour"]    = launch(benchmark::testOneOfFour)
//        results["DefaultArgument.testFourOfFour"]   = launch(benchmark::testFourOfFour)
//        results["DefaultArgument.testOneOfEight"]   = launch(benchmark::testOneOfEight)
//        results["DefaultArgument.testEightOfEight"] = launch(benchmark::testEightOfEight)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runElvisBenchmark() {
//        val benchmark = ElvisBenchmark()
//        benchmark.setup()
//
//        results["Elvis.testElvis"] = launch(benchmark::testElvis)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runEulerBenchmark() {
//        val benchmark = EulerBenchmark()
//
//        results["Euler.problem1bySequence"] = launch(benchmark::problem1bySequence)
//        results["Euler.problem1"]           = launch(benchmark::problem1)
//        results["Euler.problem2"]           = launch(benchmark::problem2)
//        results["Euler.problem4"]           = launch(benchmark::problem4)
//        results["Euler.problem8"]           = launch(benchmark::problem8)
//        results["Euler.problem9"]           = launch(benchmark::problem9)
//        results["Euler.problem14"]          = launch(benchmark::problem14)
//        results["Euler.problem14full"]      = launch(benchmark::problem14full)
//    }
//
//
//    //-------------------------------------------------------------------------//
//
//    fun runFibonacciBenchmark() {
//        val benchmark = FibonacciBenchmark()
//        results["Fibonacci.calcClassic"]         = launch(benchmark::calcClassic)
//        results["Fibonacci.calc"]                = launch(benchmark::calc)
//        results["Fibonacci.calcWithProgression"] = launch(benchmark::calcWithProgression)
//        results["Fibonacci.calcSquare"]          = launch(benchmark::calcSquare)
//    }

    //-------------------------------------------------------------------------//

    fun runInlineBenchmark() {
        val benchmark = InlineBenchmark()
//        results["Inline.calculate"]              = launch(benchmark::calculate)
//        results["Inline.calculateInline"]        = launch(benchmark::calculateInline)
//        results["Inline.calculateGeneric"]       = launch(benchmark::calculateGeneric)
        results["Inline.calculateGenericInline"] = launch(benchmark::calculateGenericInline)
    }

//    //-------------------------------------------------------------------------//
//
//    fun runIntArrayBenchmark() {
//        val benchmark = IntArrayBenchmark()
//        benchmark.setup()
//
//        results["IntArray.copy"]                     = launch(benchmark::copy)
//        results["IntArray.copyManual"]               = launch(benchmark::copyManual)
//        results["IntArray.filterAndCount"]           = launch(benchmark::filterAndCount)
//        results["IntArray.filterSomeAndCount"]       = launch(benchmark::filterSomeAndCount)
//        results["IntArray.filterAndMap"]             = launch(benchmark::filterAndMap)
//        results["IntArray.filterAndMapManual"]       = launch(benchmark::filterAndMapManual)
//        results["IntArray.filter"]                   = launch(benchmark::filter)
//        results["IntArray.filterSome"]               = launch(benchmark::filterSome)
//        results["IntArray.filterPrime"]              = launch(benchmark::filterPrime)
//        results["IntArray.filterManual"]             = launch(benchmark::filterManual)
//        results["IntArray.filterSomeManual"]         = launch(benchmark::filterSomeManual)
//        results["IntArray.countFilteredManual"]      = launch(benchmark::countFilteredManual)
//        results["IntArray.countFilteredSomeManual"]  = launch(benchmark::countFilteredSomeManual)
//        results["IntArray.countFilteredPrimeManual"] = launch(benchmark::countFilteredPrimeManual)
//        results["IntArray.countFiltered"]            = launch(benchmark::countFiltered)
//        results["IntArray.countFilteredSome"]        = launch(benchmark::countFilteredSome)
//        results["IntArray.countFilteredPrime"]       = launch(benchmark::countFilteredPrime)
//        results["IntArray.countFilteredLocal"]       = launch(benchmark::countFilteredLocal)
//        results["IntArray.countFilteredSomeLocal"]   = launch(benchmark::countFilteredSomeLocal)
//        results["IntArray.reduce"]                   = launch(benchmark::reduce)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runIntBaselineBenchmark() {
//        val benchmark = IntBaselineBenchmark()
//
//        results["IntBaseline.consume"]              = launch(benchmark::consume)
//        results["IntBaseline.allocateList"]         = launch(benchmark::allocateList)
//        results["IntBaseline.allocateArray"]        = launch(benchmark::allocateArray)
//        results["IntBaseline.allocateListAndFill"]  = launch(benchmark::allocateListAndFill)
//        results["IntBaseline.allocateArrayAndFill"] = launch(benchmark::allocateArrayAndFill)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runIntListBenchmark() {
//        val benchmark = IntListBenchmark()
//        benchmark.setup()
//
//        results["IntList.copy"]                = launch(benchmark::copy)
//        results["IntList.copyManual"]          = launch(benchmark::copyManual)
//        results["IntList.filterAndCount"]      = launch(benchmark::filterAndCount)
//        results["IntList.filterAndMap"]        = launch(benchmark::filterAndMap)
//        results["IntList.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
//        results["IntList.filter"]              = launch(benchmark::filter)
//        results["IntList.filterManual"]        = launch(benchmark::filterManual)
//        results["IntList.countFilteredManual"] = launch(benchmark::countFilteredManual)
//        results["IntList.countFiltered"]       = launch(benchmark::countFiltered)
//        results["IntList.countFilteredLocal"]  = launch(benchmark::countFilteredLocal)
//        results["IntList.reduce"]              = launch(benchmark::reduce)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runIntStreamBenchmark() {
//        val benchmark = IntStreamBenchmark()
//        benchmark.setup()
//
//        results["IntStream.copy"]                = launch(benchmark::copy)
//        results["IntStream.copyManual"]          = launch(benchmark::copyManual)
//        results["IntStream.filterAndCount"]      = launch(benchmark::filterAndCount)
//        results["IntStream.filterAndMap"]        = launch(benchmark::filterAndMap)
//        results["IntStream.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
//        results["IntStream.filter"]              = launch(benchmark::filter)
//        results["IntStream.filterManual"]        = launch(benchmark::filterManual)
//        results["IntStream.countFilteredManual"] = launch(benchmark::countFilteredManual)
//        results["IntStream.countFiltered"]       = launch(benchmark::countFiltered)
//        results["IntStream.countFilteredLocal"]  = launch(benchmark::countFilteredLocal)
//        results["IntStream.reduce"]              = launch(benchmark::reduce)
//    }
//
    //-------------------------------------------------------------------------//

    fun runLambdaBenchmark() {
        val benchmark = LambdaBenchmark()
        benchmark.setup()

//        results["Lambda.noncapturingLambda"]         = launch(benchmark::noncapturingLambda)
//        results["Lambda.noncapturingLambdaNoInline"] = launch(benchmark::noncapturingLambdaNoInline)
//        results["Lambda.capturingLambda"]            = launch(benchmark::capturingLambda)
//        results["Lambda.capturingLambdaNoInline"]    = launch(benchmark::capturingLambdaNoInline)
//        results["Lambda.mutatingLambda"]             = launch(benchmark::mutatingLambda)
        results["Lambda.mutatingLambdaNoInline"]     = launch(benchmark::mutatingLambdaNoInline)
//        results["Lambda.methodReference"]            = launch(benchmark::methodReference)
//        results["Lambda.methodReferenceNoInline"]    = launch(benchmark::methodReferenceNoInline)
    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runLoopBenchmark() {
//        val benchmark = LoopBenchmark()
//        benchmark.setup()
//
//        results["Loop.arrayLoop"]            = launch(benchmark::arrayLoop)
//        results["Loop.arrayIndexLoop"]       = launch(benchmark::arrayIndexLoop)
//        results["Loop.rangeLoop"]            = launch(benchmark::rangeLoop)
//        results["Loop.arrayListLoop"]        = launch(benchmark::arrayListLoop)
//        results["Loop.arrayWhileLoop"]       = launch(benchmark::arrayWhileLoop)
//        results["Loop.arrayForeachLoop"]     = launch(benchmark::arrayForeachLoop)
//        results["Loop.arrayListForeachLoop"] = launch(benchmark::arrayListForeachLoop)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runMatrixMapBenchmark() {
//        val benchmark = MatrixMapBenchmark()
//
//        results["MatrixMap.add"] = launch(benchmark::add)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runParameterNotNullAssertionBenchmark() {
//        val benchmark = ParameterNotNullAssertionBenchmark()
//
//        results["ParameterNotNull.invokeOneArgWithNullCheck"]       = launch(benchmark::invokeOneArgWithNullCheck)
//        results["ParameterNotNull.invokeOneArgWithoutNullCheck"]    = launch(benchmark::invokeOneArgWithoutNullCheck)
//        results["ParameterNotNull.invokeTwoArgsWithNullCheck"]      = launch(benchmark::invokeTwoArgsWithNullCheck)
//        results["ParameterNotNull.invokeTwoArgsWithoutNullCheck"]   = launch(benchmark::invokeTwoArgsWithoutNullCheck)
//        results["ParameterNotNull.invokeEightArgsWithNullCheck"]    = launch(benchmark::invokeEightArgsWithNullCheck)
//        results["ParameterNotNull.invokeEightArgsWithoutNullCheck"] = launch(benchmark::invokeEightArgsWithoutNullCheck)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runPrimeListBenchmark() {
//        val benchmark = PrimeListBenchmark()
//
//        results["PrimeList.calcDirect"]       = launch(benchmark::calcDirect)
//        results["PrimeList.calcEratosthenes"] = launch(benchmark::calcEratosthenes)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runStringBenchmark() {
//        val benchmark = StringBenchmark()
//        benchmark.setup()
//
//        results["String.stringConcat"]                = launch(benchmark::stringConcat)
//        results["String.stringConcatNullable"]        = launch(benchmark::stringConcatNullable)
//        results["String.stringBuilderConcat"]         = launch(benchmark::stringBuilderConcat)
//        results["String.stringBuilderConcatNullable"] = launch(benchmark::stringBuilderConcatNullable)
//        results["String.summarizeSplittedCsv"]        = launch(benchmark::summarizeSplittedCsv)
//    }
//
    //-------------------------------------------------------------------------//

    fun runSwitchBenchmark() {
        val benchmark = SwitchBenchmark()
        benchmark.setupInts()
        benchmark.setupStrings()
        benchmark.setupEnums()
        benchmark.setupSealedClassses()

//        results["Switch.testSparseIntSwitch"]  = launch(benchmark::testSparseIntSwitch)
//        results["Switch.testDenseIntSwitch"]   = launch(benchmark::testDenseIntSwitch)
//        results["Switch.testConstSwitch"]      = launch(benchmark::testConstSwitch)
//        results["Switch.testObjConstSwitch"]   = launch(benchmark::testObjConstSwitch)
//        results["Switch.testVarSwitch"]        = launch(benchmark::testVarSwitch)
//        results["Switch.testStringsSwitch"]    = launch(benchmark::testStringsSwitch)
        results["Switch.testEnumsSwitch"]      = launch(benchmark::testEnumsSwitch)
//        results["Switch.testDenseEnumsSwitch"] = launch(benchmark::testDenseEnumsSwitch)
//        results["Switch.testSealedWhenSwitch"] = launch(benchmark::testSealedWhenSwitch)
    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runWithIndiciesBenchmark() {
//        val benchmark = WithIndiciesBenchmark()
//        benchmark.setup()
//
//        results["WithIndicies.withIndicies"]       = launch(benchmark::withIndicies)
//        results["WithIndicies.withIndiciesManual"] = launch(benchmark::withIndiciesManual)
//    }
//
//    //-------------------------------------------------------------------------//
//
//    fun runOctoTest() {
//        results["OctoTest"] = launch(::octoTest)
//    }

    //-------------------------------------------------------------------------//

    fun Double.toString(n: Int): String {
        val str = this.toString()
        if (str.contains('e', ignoreCase = true)) return str

        val len      = str.length
        val pointIdx = str.indexOf('.')
        val dropCnt  = len - pointIdx - n - 1
        if (dropCnt < 1) return str
        return str.dropLast(dropCnt)
    }
}


fun main(args: Array<String>) {
    Launcher(10).runBenchmarks()
}

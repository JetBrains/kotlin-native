//@SymbolName("Kotlin_konan_internal_undefined")
//internal external fun <T> undefined(): T
//
//class Zef<T> {
//    var element: T = undefined()
//}

fun main(args: Array<String>) {
    for (i in 0 until args[0].length)
        println(args[0][i])
}
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
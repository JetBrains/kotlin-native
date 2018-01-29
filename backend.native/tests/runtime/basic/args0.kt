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

fun main(args : Array<String>) {
  for (s in args) {
      println(s)
  }
}

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
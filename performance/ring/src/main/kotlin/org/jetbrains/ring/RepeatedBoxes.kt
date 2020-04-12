package org.jetbrains.ring

import org.jetbrains.benchmarksLauncher.Blackhole

fun <@Specialized(forTypes = [Int::class]) T> putIfAbsent(map: MutableMap<T, String>, key: T, value: String) {
    if (key in map) return
    map[key] = value
}

val map = mutableMapOf<Int, String>()

// Benchmark
fun testPutIfAbsent() {
    for (i in 1..BENCHMARK_SIZE) {
        putIfAbsent(map, i, i.toString())
    }
}

// Benchmark
fun testConsume() {
    for (i in 1..BENCHMARK_SIZE) {
        val v = Random.nextInt()
        cons(v)
    }
}

fun cons(v: Int) {
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
    Blackhole.consume(v)
}
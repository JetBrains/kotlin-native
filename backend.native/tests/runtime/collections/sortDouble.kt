package runtime.collections.sortDouble

import kotlin.test.*

fun test(list: MutableList<Double>) {
    list.sort()
    println(list.toString())
}

@Test fun runTest() {
    test(mutableListOf<Double>(0.5, -0.0, 41.999, -4.2, 1984.0, 0.0, 451.6, -273.0))

    test(mutableListOf<Double>(0.5, -0.0, 41.999, -4.2, 1984.0,
            Double.NaN, 0.0, -451.0, Double.NEGATIVE_INFINITY, Double.MAX_VALUE, Double.POSITIVE_INFINITY))

    test(mutableListOf<Double>(Double.NaN, 0.1, 1e-3 * 0, -1.1 * 0, Double.NaN, 42.0, -Double.NaN))

    test(mutableListOf<Double>(0.1, 1e-3 * 0, -1.1 * 0))

    val l = (-1.0 * 0)
    val r = (1.23 * 0)
    println(l)
    println(r)

    if (l.compareTo(r) == 0) {
        throw RuntimeException("-0.0 == 0.0 by compareTo()")
    }
    if (l == r) {
        println("-0.0 == 0.0")
    }
}
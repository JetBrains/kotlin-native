package codegen.controlflow.for_loops_lowering.test15

import kotlin.test.*

@Test fun runTest() {
    val arr = arrayOf(1, 2, 7147, 4, 5)
    var list = listOf(1, 2, 55, 12, 4)
    var sum = 0
    for (a in 0 .. 7) {
        for (b in arr.indices) {
            for (c in list.indices) {
                for (d in 0 until 5) {
                    for (e in 5 downTo 0) {
                        for (f in a .. c) {
                            for (g in d .. e) {
                                for (h in g .. a) {
                                    for (i in 0 .. 7) {
                                        sum += arr[b] + list[c] + d + e + f
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    println(sum)
}

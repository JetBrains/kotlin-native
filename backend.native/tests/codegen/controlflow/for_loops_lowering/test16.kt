package codegen.controlflow.for_loops_lowering.test16

import kotlin.test.*

@Test fun runTest() {
    val arr = arrayOf(1, 2, 3, 4, 5, 6, 7)
    var list = listOf(1, 2, 3, 4, 5, 6, 7)
    var sum = 0
    for (a in 0 .. 5) {
        for (b in arr.indices) {
            for (c in list.indices) {
                for (d in 0 until 5) {
                    for (e in 5 downTo 0) {
                        for (f in a .. c) {
                            for (g in d .. e) {
                                for (h in g .. a) {
                                    for (i in 0 .. -4) {
                                        sum += 1
                                    }
                                    for (i in 0 .. 5) {
                                        sum += arr[b] + list[c] + d + e + f
                                    }
                                    for (i in -4 downTo 1) {
                                        sum += 1
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

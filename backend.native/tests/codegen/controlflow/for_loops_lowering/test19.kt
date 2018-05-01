package codegen.controlflow.for_loops_lowering.test19

import kotlin.test.*

@Test fun runTest() {
    var sum = 0
    for (a in 3 downTo 0) {
        for (b in 0 .. 5) {
            for (c in 0 until 7) {
                for (d in 0 .. 10) {
                    for (e in 0 until 5) {
                        for (f in 0 .. 5) {
                            for (g in 0 until 5) {
                                for (h in 10 downTo 0) {
                                    sum += a + b + c + d + e + f + g + h
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

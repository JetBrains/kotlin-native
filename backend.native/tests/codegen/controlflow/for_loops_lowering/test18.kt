package codegen.controlflow.for_loops_lowering.test18

import kotlin.test.*

@Test fun runTest() {
    var sum = 0
    for (a in 0 .. 5) {
        for (b in 0 .. 5) {
            for (c in 0 .. 5) {
                for (d in 0 .. 5) {
                    for (e in 0 .. 4) {
                        for (f in 0 .. 2) {
                            for (g in 0 .. 3) {
                                for (h in 0 .. 5) {
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

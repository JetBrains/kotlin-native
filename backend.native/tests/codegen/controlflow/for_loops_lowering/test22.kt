package codegen.controlflow.for_loops_lowering.test22

import kotlin.test.*

@Test fun runTest() {
    var sum = 0
    for (a in 1 .. 5 step 1) {
        for (b in 1 .. 5 step 2 step 1) {
            for (c in 1 .. 6 step 3 step 1) {
                for (d in 1 .. 10 step a step b) {
                    for (e in 1 .. 10 step c step d step a) {
                        for (f in 1 .. 5 step e step 2) {
                            for (g in 1 until 10 step 10 step 2) {
                                for (h in 10 downTo 0 step 26 step 2) {
                                    for (i in 0 .. 5 step (a + b + 1)) {
                                        sum += a + b + c + d + e + f + g + h + i
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

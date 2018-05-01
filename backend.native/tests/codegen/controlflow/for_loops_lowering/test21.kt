package codegen.controlflow.for_loops_lowering.test21

import kotlin.test.*

@Test fun runTest() {
    var sum = 0
    for (g in 0 until 10 step 30 step 2) {
        sum += g
    }
    println(sum)
}

package codegen.controlflow.for_loops_lowering.test12

import kotlin.test.*

@Test fun runTest() {
    val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var sum = 0
    for (i in 0 .. Int.MAX_VALUE step 5000000) {
        for (j in arr.indices) {
            sum += i + j
        }
    }
    println(sum)
}

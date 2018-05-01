package codegen.controlflow.for_loops_lowering.test14

import kotlin.test.*

@Test fun runTest() {
    val arr = arrayOf(1, 2, 3, 4, 5)
    var sum = 0L
    for (i in 0L .. Long.MAX_VALUE step 10000000000000000L) {
        for (j in arr.indices) {
            for (k in 'a' until 'z') {
                sum += i + j + k.toLong()
            }
        }
    }
    println(sum)
}

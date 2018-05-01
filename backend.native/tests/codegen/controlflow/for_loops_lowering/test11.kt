package codegen.controlflow.for_loops_lowering.test11

import kotlin.test.*

@Test fun runTest() {
    val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    for (i in arr.indices step 2) {
      println(arr[i])
    }
}

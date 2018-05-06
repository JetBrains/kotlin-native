package codegen.controlflow.for_loops_lowering.test32

import kotlin.test.*

@Test fun runTest() {
	val arr = arrayOf(7, 10, 2, 6, 7, 9, 0, 4)
	for (i in arr) {
    if (i in arr.indices) {
      println(i)
    }
  }
}

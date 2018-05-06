package codegen.controlflow.for_loops_lowering.test29

import kotlin.test.*

@Test fun runTest() {
	val arr = arrayOf(7, 7, 2, 6, 7, 3, 0)
	for (i in arr) {
    var arr1 = arrayOf(1, 2, 3)
    if (i in arr1) {
      println(i)
    }
  }   
}

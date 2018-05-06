package codegen.controlflow.for_loops_lowering.test30

import kotlin.test.*

@Test fun runTest() {
	val arr = arrayOf(7, 7, 2, 6, 7, 9, 0)
	for (i in arr) {
    var rng = 0 .. 3
    if (i in rng) {
      println(i)
    }
  }   
}

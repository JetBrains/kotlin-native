package codegen.controlflow.for_loops_lowering.test7

import kotlin.test.*

@Test fun runTest() {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'f' downTo 'd' step 2) {
        println(c)
      }
    }
}

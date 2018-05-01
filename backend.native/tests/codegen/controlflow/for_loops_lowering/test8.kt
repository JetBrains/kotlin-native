package codegen.controlflow.for_loops_lowering.test8

import kotlin.test.*

@Test fun runTest() {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'a' until 'e' step 2) {
        println(c)
      }
    }
}

package codegen.controlflow.for_loops_lowering.test5

import kotlin.test.*

@Test fun runTest() {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'e' until 'k') {
        println(c)
      }
    }
}

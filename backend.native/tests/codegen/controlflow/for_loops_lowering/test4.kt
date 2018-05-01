package codegen.controlflow.for_loops_lowering.test4

import kotlin.test.*

@Test fun runTest() {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'e' .. 'k') {
        println(c)
      }
    }
}

package codegen.controlflow.for_loops_lowering.test6

import kotlin.test.*

@Test fun runTest() {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'k' downTo 'e') {
        println(c)
      }
    }
}

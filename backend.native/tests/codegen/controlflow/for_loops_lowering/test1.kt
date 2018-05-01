package codegen.controlflow.for_loops_lowering.test1

import kotlin.test.*

@Test fun runTest() {
    fun foo() : Int = 2
    val rng = 5 .. 10
    var arr = arrayOf(1, 2, 3)
    for (i in rng) {
    	for (j in arr.indices) {
          for (k in 4 downTo 0 step foo()) {
              println("$i + $j = ${i + j}")
          }
      }
    }
}
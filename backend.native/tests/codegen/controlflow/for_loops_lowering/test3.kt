package codegen.controlflow.for_loops_lowering.test3

import kotlin.test.*

@Test fun runTest() {
    fun foo() : Int { println("foo"); return 2 }

    fun bar() : Int = 1

    val rng = (Int.MAX_VALUE - 5) .. Int.MAX_VALUE
    var arr = listOf(4, 5, 6)
    for (i in rng) {
    	for (j in arr.indices) {
          for (k in 4 downTo 0 step foo() step bar()) {
              println("$i + $j = ${i + j}")
          }
      }
    }
}
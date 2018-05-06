package codegen.controlflow.for_loops_lowering.test35

import kotlin.test.*

@Test fun runTest() {
  val arr = arrayOf(4, 5, 6, 14, 15, 16, 24, 25, 26, 34, 35, 36, 0, 50, 60, 70)
  fun foo() : Int = 10
  fun bar() : Int { println("bar"); return 14 }
  fun tre() : Int { println("tre"); return 2 }
  for (i in arr) {
    val arr1 = arrayOf(14, 24, 24)
    if (i in arr1) {
      println(i)
    }
    val rng1 = 0 .. foo()
    if (i in rng1) {
      println(i)
    }
    val rng2 = 0 until bar()
    if (i in rng2) {
      println(i)
    } 
    val rng3 = 15 downTo 0
    if (i in rng3 step foo()) {
      println(i)
    }
    val inds1 = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).indices
    if (i in inds1) {
      println(i)
    }
    if (i in inds1 step tre()) {
      println(i)
    }
  }
}

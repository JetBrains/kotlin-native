package codegen.controlflow.for_loops_lowering.test34

import kotlin.test.*

@Test fun runTest() {
  val arr = arrayOf(4, 5, 6, 14, 15, 16, 24, 25, 26, 34, 35, 36, 0, 50, 60, 70)
  fun foo() : Int = 10
  fun bar() : Int { println("bar"); return 14 }
  for (i in arr) {
    if (i in arrayOf(14, 24, 24)) {
      println(i)
    }
    if (i in 0 .. foo()) {
      println(i)
    }
    if (i in 0 until bar()) {
      println(i)
    } 
    if (i in 15 downTo 0) {
      println(i)
    }
    if (i in arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).indices) {
      println(i)
    }
  }
}

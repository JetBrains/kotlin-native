package codegen.branching.advanced_when5

import kotlin.test.*

fun advanced_when5(i: Int): Int {
  when (i) {
    0 -> { val v = 42; return v}
    1 -> { val v = 42; return v}
    2 -> { val v = 42; return v}
    3 -> { val v = 42; return v}
    4 -> { val v = 42; return v}
    else -> return 24
  }
}

@Test fun runTest() {
  if (advanced_when5(5) != 24) throw Error()
}
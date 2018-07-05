package codegen.inline.callableReferenceWithReifiedParameter

import kotlin.test.*

inline fun <reified T> foo(a: Any, b: T, block: () -> Int) = if (a is T) block() else block() * 200

@Test fun runTest() {
    val a: (Any, Int, ()-> Int) -> Int = ::foo

    val b = a.invoke("", 10, { 10 })

    println(b)
}
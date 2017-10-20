package codegen.inline.inline3

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int): Int {
    try {
        return i4 / i5
    } catch (e: Throwable) {
        return i4
    }
}

fun bar(i1: Int, i2: Int, i3: Int): Int {
    return i1 + foo(i2, i3)
}

@Test fun runTest() {
    println(bar(1, 8, 2).toString())
}

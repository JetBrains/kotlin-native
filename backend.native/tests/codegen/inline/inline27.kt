package codegen.inline.inline27

import kotlin.test.*

inline fun foo(block: () -> Unit) = block()

@Test fun test1() {
    foo {
        val x = ""
        assertEquals(x::class, String::class)
    }
}

@Test fun test2() {
    val cls1: Any? = Int
    val cls2: Any? = ""

    cls1?.let {
        cls2?.let {
            var itClass = it::class
            assertEquals(itClass, String::class)
        }
    }
}

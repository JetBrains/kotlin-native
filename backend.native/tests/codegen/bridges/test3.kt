package codegen.bridges.test3

import kotlin.test.*

// non-generic interface, generic impl, non-virtual call + interface call
open class A<T> {
    var size: T = 56 as T
}

interface C {
    var size: Int
}

class B : C, A<Int>()

fun box(): String {
    val b = B()
    if (b.size != 56) return "fail 1"

    b.size = 55
    if (b.size != 55) return "fail 2"

    val c: C = b
    if (c.size != 55) return "fail 3"

    c.size = 57
    if (c.size != 57) return "fail 4"

    return "OK"
}

@Test fun runTest() {
    println(box())
}

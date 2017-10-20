package codegen.bridges.test18

import kotlin.test.*

// overriden function returns Unit
open class A {
    open fun foo(): Any = 42
}

open class B: A() {
    override fun foo(): Unit { }
}

@Test fun runTest() {
    val a: A = B()
    println(a.foo())
}

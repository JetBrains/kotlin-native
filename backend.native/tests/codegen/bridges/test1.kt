package codegen.bridges.test1

import kotlin.test.*

// interface call, bridge overridden
interface Z1 {
    fun foo(x: Int) : Any
}

open class A : Z1 {
    override fun foo(x: Int) : Int = 5
}

open class B : A() {
    override fun foo(x: Int) : Int = 42
}

@Test fun runTest() {
    val z1: A = B()
    println((z1.foo(1) + 1000).toString())
}


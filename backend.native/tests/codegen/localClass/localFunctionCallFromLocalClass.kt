package codegen.localClass.localFunctionCallFromLocalClass

import kotlin.test.*

@Test fun runTest() {
    var x = 1
    fun local1() {
        x++
    }

    class A {
        fun bar() {
            local1()
        }
    }
    A().bar()
    println("OK")
}
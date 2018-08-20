package codegen.inline.classDeclarationInsideInline

import kotlin.test.*

fun f() {
    run {
        class Test<T>(val x: T) {
            override fun toString() = "test2"
        }

        class Test2(val a: Test<Int>)

        val v = Test2(Test(1))
        println(v.a)
        println(v.a.x)
    }
}

fun main(args: Array<String>) {
    f()
}

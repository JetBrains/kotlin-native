package codegen.kclass.kclass2

import kotlinx.cinterop.*
import kotlin.test.*

open class Foo
class Bar
class Boo : Foo()

private fun callback1() {
    println("callback 1")
}

private fun callback2() {
    println("callback 2")
}

@Test
fun testUserClass() {
    val f = Foo()
    f::class.customInfo = staticCFunction(::callback1)

    val b = Boo()
    b::class.customInfo = staticCFunction(::callback2)

    (@Suppress("UNCHECKED_CAST")(Foo()::class.customInfo as CPointer<CFunction<() -> Unit>>))()
    (@Suppress("UNCHECKED_CAST")(Boo()::class.customInfo as CPointer<CFunction<() -> Unit>>))()
}

@Test
fun testSystemClass() {
    "Hi there"::class.customInfo = staticCFunction(::callback1)
    Any()::class.customInfo = staticCFunction(::callback2)

    (@Suppress("UNCHECKED_CAST")("Bye"::class.customInfo as CPointer<CFunction<() -> Unit>>))()
    (@Suppress("UNCHECKED_CAST")(Any()::class.customInfo as CPointer<CFunction<() -> Unit>>))()
}
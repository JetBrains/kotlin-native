package samples

import kotlin.native.*
import org.jetbrains.ring.CountBoxings

@SpecializedClass(forTypes = [Int::class])
class ThatSimpleBox<T1>

@SpecializedClass(forTypes = [Int::class])
class SimpleBox<T2>(val value: T2)

@SpecializedClass(forTypes = [Int::class])
class NoThatSimpleBox<T3>(val value: T3) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other) || (other as? NoThatSimpleBox<*>)?.value == value
    }
}

@SpecializedClass(forTypes = [Int::class, Long::class, Double::class])
class ManyConstructorsBox<T>(val value: T) {
    constructor(v: T, w: T) : this(v, 42, true)
    constructor(v: T, w: T, z: Int) : this(w, v)
    constructor(v: T, w: Int, t: Boolean) : this(v)
}

@SpecializedClass(forTypes = [Int::class, Long::class, Double::class])
class ClashingConstructorsBox<T>(val value: T) {
    constructor(v: T, w: Int) : this(v)
    constructor(v: Int, w: T) : this(w)
}

@SpecializedClass(forTypes = [Int::class])
class BoxWithId<T>(val value: T) {
    fun id() = value
}

@SpecializedClass(forTypes = [Int::class])
class BoxWithEqls<T>(val value: T) {
    fun eqls(other: BoxWithEqls<T>) = value == other.value
}

@SpecializedClass(forTypes = [Int::class])
class BoxWithCustomGetters<T>(val value1: T, val value2: T) {
    private val x = value1
        get() = if (foo()) field else value2
    private val y = value2
        get() = if (foo()) value1 else field

    fun foo() = value1 == value2
    fun bar() = x == y
}

@SpecializedClass(forTypes = [Int::class])
class BoxWithCustomSetters<T>(val value1: T, val value2: T) {
    var x = value1
        set(value) {
            field = if (!foo()) value2 else value
        }

    var y = value2
        set(value) {
            field = if (!foo()) value else value1
        }

    fun foo() = value1 == value2

    fun bar(value: T) {
        x = value
        y = value
    }
}

@SpecializedClass(forTypes = [Int::class])
class BoxWithNestedClass<T>(val outerValue: T) {
    fun foo(f: Boolean) = if (f) outerValue else Nested(outerValue).foo()

    @SpecializedClass(forTypes = [Int::class, Char::class])
    class Nested<U>(val innerValue: U) {
        fun foo() = innerValue
    }

    fun foo2(f: Boolean) = if (f) outerValue else Nested(outerValue).foo()
}

@SpecializedClass(forTypes = [Int::class, Double::class])
class BoxWithInnerClass<T>(val outerValue: T) {
    fun foo(f: Boolean) = if (f) outerValue else Inner(outerValue).foo(f)

    inner class Inner(val innerValue: T) {
        fun foo(f: Boolean) = if (f) outerValue else innerValue
    }
}

@SpecializedClass(forTypes = [Int::class])
class BoxWithCompanion<T>(val value: T) {
    fun foo() = bar(this)

    companion object {
        val t = 42
        fun bar(x: Any) = x.getInt(false) + t
        private fun Any.getInt(f: Boolean) = if (f) hashCode() else 11
    }
}

@SpecializedClass(forTypes = [Int::class])
class BoxWithLambda<T>(val factory: () -> T) {
    fun next(): T {
        return factory()
    }
}

@CountBoxings
fun runClasses() {

    inline fun assert(value: Boolean, lazyMessage: () -> Any = { "Assertion error" }) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }

    inline fun assertEquals(expectedValue: Any, actualValue: Any, lazyMessage: () -> Any = { "(expected) $expectedValue != $actualValue (actual)" }) {
        if (expectedValue != actualValue) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }

    for (i in 1..10) {
        val x1 = ThatSimpleBox<Int>()
        val x2 = SimpleBox(i)
        val x3 = NoThatSimpleBox(i)
        val x4 = ManyConstructorsBox(i)
        val x5 = ClashingConstructorsBox(i)
        val x6 = BoxWithId(i).id()

        val x7 = BoxWithEqls(i)
        val x8 = x7
        val b1 = x7.eqls(x8)
        val b2 = x8.eqls(x7)

        val x9 = BoxWithCustomGetters(i, i + (2 * (i % 2) - 1))
        val b3 = x9.foo()
        val b4 = x9.bar()
        val x11 = x9.value1
        val x12 = x9.value2

        val x13 = BoxWithCustomSetters(i, i + (2 * (i % 2) - 1))
        val x14 = x13.x
        val x15 = x13.y

        val x16 = BoxWithNestedClass(i)
        val x17_1 = x16.foo(true) + x16.foo2(false) + x16.outerValue
        val x17_2 = x16.foo(false) + x16.foo2(true) + x16.outerValue
        val x18 = BoxWithNestedClass.Nested(i * 2)
        val x19 = x18.foo() + x18.innerValue

        val x20 = BoxWithInnerClass(i)
        val x21_1 = x20.foo(true) + x20.outerValue
        val x21_2 = x20.foo(false) + x20.outerValue
        val x22 = x20.Inner(i * 2)
        val x23 = x22.foo(true)
        val x24 = x22.foo(false)

        val d20 = BoxWithInnerClass(i.toDouble())
        val d21_1 = d20.foo(true) + d20.outerValue
        val d21_2 = d20.foo(false) + d20.outerValue
        val d22 = d20.Inner(i * 2.0)
        val d23 = d22.foo(true)
        val d24 = d22.foo(false)

        val s20 = BoxWithInnerClass("hello")
        val s21_1 = s20.foo(true) + s20.outerValue
        val s21_2 = s20.foo(false) + s20.outerValue
        val s22 = s20.Inner("world")
        val s23 = s22.foo(true)
        val s24 = s22.foo(false)

        val x25 = ManyConstructorsBox(i, i * 2)
        val x26 = ManyConstructorsBox(i * 2.0, i * 3.0, i)
        val x27 = ManyConstructorsBox(i + 1L, i * 2, false)
        val x28 = ClashingConstructorsBox(i * 3.1, i)
        val x29 = ClashingConstructorsBox(i + 42, i - 3L)

        val x30 = BoxWithCompanion(i)

        val x31 = BoxWithLambda { i }

        assertEquals(i, x2.value)
        assertEquals(i, x3.value)
        assertEquals(i, x4.value)
        assertEquals(i, x5.value)
        assertEquals(i, x6)

        assertEquals(i, x7.value)
        assertEquals(i, x8.value)
        assert(b1)
        assert(b2)

        assert(!b3)
        assert(b4)
        assertEquals(i, x11)
        assertEquals(i + (2 * (i % 2) - 1), x12)

        assertEquals(i, x13.value1)
        assertEquals(i + (2 * (i % 2) - 1), x13.value2)
        assertEquals(i, x14)
        assertEquals(i + (2 * (i % 2) - 1), x15)

        x13.bar(i + 42)

        assertEquals(i, x13.value1)
        assertEquals(i + (2 * (i % 2) - 1), x13.value2)
        assertEquals(i + (2 * (i % 2) - 1), x13.x)
        assertEquals(i + 42, x13.y)

        assertEquals(i * 3, x17_1)
        assertEquals(i * 3, x17_2)
        assertEquals(i * 4, x19)

        assertEquals(i * 2, x21_1)
        assertEquals(i * 2, x21_2)
        assertEquals(i, x23)
        assertEquals(i * 2, x24)

        assertEquals(i * 2.0, d21_1)
        assertEquals(i * 2.0, d21_2)
        assertEquals(i.toDouble(), d23)
        assertEquals(i * 2.0, d24)

        assertEquals("hellohello", s21_1)
        assertEquals("hellohello", s21_2)
        assertEquals("hello", s23)
        assertEquals("world", s24)

        assertEquals(i, x25.value)
        assertEquals(i * 3.0, x26.value)
        assertEquals(i + 1L, x27.value)
        assertEquals(i * 3.1, x28.value)
        assertEquals(i - 3L, x29.value)

        assertEquals(i, x30.value)
        assertEquals(53, BoxWithCompanion.Companion.bar(x30))
        assertEquals(53, x30.foo())

        assertEquals(i, x31.next())
        assertEquals(i, x31.next())
        assertEquals(i, x31.next())
    }
}
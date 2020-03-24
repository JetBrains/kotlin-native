package samples

import org.jetbrains.ring.CountBoxings

class ThatSimpleBox<T1>
class SimpleBox<T2>(val value: T2)
class NoThatSimpleBox<T3>(val value: T3) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other) || (other as? NoThatSimpleBox<*>)?.value == value
    }
}

class ManyConstructorsBox<T>(val value: T) {
    constructor(v: T, w: T) : this(v, 42, true)
    constructor(v: T, w: T, z: Int) : this(w, v)
    constructor(v: T, w: Int, t: Boolean) : this(v)
}

class ClashingConstructorsBox<T>(val value: T) {
    constructor(v: T, w: Int) : this(v)
    constructor(v: Int, w: T) : this(w)
}

class BoxWithId<T>(val value: T) {
    fun id() = value
}

class BoxWithEqls<T>(val value: T) {
    fun eqls(other: BoxWithEqls<T>) = value == other.value
}

class BoxWithCustomGetters<T>(val value1: T, val value2: T) {
    private val x = value1
        get() = if (foo()) field else value2
    private val y = value2
        get() = if (foo()) value1 else field

    fun foo() = value1 == value2
    fun bar() = x == y
}

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

class BoxWithNestedClass<T>(val outerValue: T) {
    fun foo(f: Boolean) = if (f) outerValue else Nested(outerValue).foo()

    class Nested<U>(val innerValue: U) {
        fun foo() = innerValue
    }

    fun foo2(f: Boolean) = if (f) outerValue else Nested(outerValue).foo()
}

class BoxWithInnerClass<T>(val outerValue: T) {
    fun foo(f: Boolean) = if (f) outerValue else Inner(outerValue).foo(f)

    inner class Inner(val innerValue: T) {
        fun foo(f: Boolean) = if (f) outerValue else innerValue
    }
}

@CountBoxings
fun runClasses() {
    for (i in 1..10) {
        val x1 = ThatSimpleBox<Int>()
        val x2 = SimpleBox(i)
        val x3 = NoThatSimpleBox(i)
        val x4 = ManyConstructorsBox(i)
        val x5 = ClashingConstructorsBox(i)
        val x6 = BoxWithId(i).id()

        val x7 = BoxWithEqls(i)
        val x8 = x7
        x7.eqls(x8)
        x8.eqls(x7)

        val x9 = BoxWithCustomGetters(i, i + (2 * (i % 2) - 1))
        val x10 = x9.bar()
        val x11 = x9.value1
        val x12 = x9.value2

        val x13 = BoxWithCustomSetters(i, i + (2 * (i % 2) - 1))
        x13.bar(i + 42)
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
        val x23 = x22.foo(true) + x22.foo(false)
    }
}
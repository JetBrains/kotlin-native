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
    }
}
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
    }
}
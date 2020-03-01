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

@CountBoxings
fun runClasses() {
    for (i in 1..10) {
        val x1 = ThatSimpleBox<Int>()
        val x2 = SimpleBox(i)
        val x3 = NoThatSimpleBox(i)
        val x4 = ManyConstructorsBox(i)
    }
}
package samples

import org.jetbrains.ring.CountBoxings

fun <U> libid__(x: U) = x

fun <T> libeqls__(x: T, y: T) = x == y

class MyHumbleBox<T>(val value: T) {
    constructor(v: T, w: T) : this(v, 42)
    constructor(v: T, w: T, z: Int) : this(w, z)
    constructor(v: T, w: Int) : this(v)
}

@CountBoxings
fun runLib(): Int {
    for (i in 1..10) {
        val j = libid__(i)
        libeqls__(i, j)
        MyHumbleBox(i)
    }
    return 0
}
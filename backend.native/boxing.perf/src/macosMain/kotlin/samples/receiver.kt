package samples

import org.jetbrains.ring.CountBoxings

class Box(val x: Int) {
    fun default__() = x - 1
}

class GenericBox<T>(val value: T) {
    fun default__() = if (value is Int) value - 1 else 0
}

fun <T> Box.doDefault__(): Int {
    return this.default__()
}

fun <T> GenericBox<T>.doDefault__(): Int {
    return this.default__()
}

fun <T> doDefault__(box: GenericBox<T>) : Int {
    return box.default__()
}

fun <T> GenericBox<T>.doDefault__(other: GenericBox<T>) = default__() + other.default__()

fun <T> T.eqls__(other: T) = this == other

fun <T> GenericBox<T>.eqls__(other: T) = this == other

@CountBoxings
fun runReceiver(): Int {
    for (i in 1..10) {
        Box(i).doDefault__<Int>()                           // 0 boxings
        GenericBox(i).doDefault__()                         // 10 boxings
        doDefault__(GenericBox(i + 1))                      // 10 boxings
        GenericBox(i - 1).doDefault__(GenericBox(i + 1))    // 20 boxings
        1.eqls__(2)                                         // 0 boxings
        GenericBox(1).eqls__(2)                             // 10 boxings
    }
    return 0
}
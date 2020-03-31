package samples

import kotlin.native.Specialized
import kotlin.native.SpecializedClass
import org.jetbrains.ring.CountBoxings

class Box(val x: Int) {
    fun default__() = x - 1
}

@SpecializedClass(forTypes = [Int::class])
class GenericBox<T>(val value: T) {
    fun default__() = 42
}

fun <@Specialized(forTypes = [Int::class]) T> Box.doDefault__(): Int {
    return this.default__()
}

fun <@Specialized(forTypes = [Int::class]) T> GenericBox<T>.doDefault__(): Int {
    return this.default__()
}

fun <@Specialized(forTypes = [Int::class]) T> doDefault__(box: GenericBox<T>) : Int {
    return box.default__()
}

fun <@Specialized(forTypes = [Int::class]) T> GenericBox<T>.doDefault__(other: GenericBox<T>) = default__() + other.default__()

fun <@Specialized(forTypes = [Int::class]) T> T.eqls__(other: T) = this == other

fun <@Specialized(forTypes = [Int::class]) T> GenericBox<T>.eqls__(other: T) = this.value == other

@CountBoxings
fun runReceiver(): Int {
    for (i in 1..10) {
        Box(i).doDefault__<Int>()
        GenericBox(i).doDefault__()
        doDefault__(GenericBox(i + 1))
        GenericBox(i - 1).doDefault__(GenericBox(i + 1))
        1.eqls__(2)
        GenericBox(1).eqls__(2)
    }
    return 0
}
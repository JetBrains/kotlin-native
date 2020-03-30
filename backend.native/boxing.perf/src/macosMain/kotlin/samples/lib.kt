package samples

import kotlin.native.Specialized
import org.jetbrains.ring.CountBoxings

fun <@Specialized(forTypes = [Int::class]) U> libid__(x: U) = x

fun <@Specialized(forTypes = [Int::class]) T> libeqls__(x: T, y: T) = x == y

@CountBoxings
fun runLib(): Int {
    for (i in 1..10) {
        val j = libid__(i)
        libeqls__(i, j)
    }
    return 0
}
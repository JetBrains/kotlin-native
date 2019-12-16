package lib
import kotlin.system.measureNanoTime

fun <U> id(x: U) = x

fun <T> eqls(x: T, y: T) = x == y

fun runCount(): Int {
    for (i in 1..10) {
        val j = id(i)
        eqls(i, j)
    }
    return 0
}
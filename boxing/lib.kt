package lib
import kotlin.system.measureNanoTime

fun runCount(): Int {
    for (i in 1..10) {
        println(i)
        println(i)
    }
    return 0
}
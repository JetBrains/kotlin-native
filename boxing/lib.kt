package lib

fun <U> id__(x: U) = x

fun <T> eqls__(x: T, y: T) = x == y

fun runCount(): Int {
    for (i in 1..10) {
        val j = id__(i)
        eqls__(i, j)
    }
    return 0
}
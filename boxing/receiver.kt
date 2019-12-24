package receiver

class Box(val x: Int?) {
    fun default__() = if (x is Int) x - 1 else 0
}

fun <T> Box.doDefault__(): Int {
    return this.default__()
}

fun Box.doDefault___Int(): Int {
    return this.default__()
}

fun runCount(): Int {
    for (i in 1..10) {
        Box(i).doDefault__<Int>()
    }
    return 0
}
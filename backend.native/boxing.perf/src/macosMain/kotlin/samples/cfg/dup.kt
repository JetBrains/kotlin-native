package samples.cfg

fun cfgDup(x: Int, y: Int) {
    var z = 1
    box(x)
    box(y)
    box(z)
    if (x < 5) {
        z++
        if (z < y) {
            box(y)
        } else {
            box(z)
        }
    }
}

fun cfgDup2(x: Int) {
    val y = 1
    box(x)
    if (x < y) {
        box(y)
    } else {
        box(x)
    }
}

fun cfgDup3(x: Int) {
    val y = 1
    if (x < y) {
        box(y)
        box(y)
    } else {
        box(x)
    }
}

fun box(a: Any?) {
    println(a)
}
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

fun cfgDup2(x: Int, z: Int) {
    val y = 1
    box(x)
    if (x < y) {
        box(y)
    } else {
        box(z)
    }
    box(y)
}

fun box(a: Any?) {
    println(a)
}
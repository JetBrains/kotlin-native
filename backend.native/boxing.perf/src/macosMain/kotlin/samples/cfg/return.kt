package samples.cfg

fun returnIf(x: Int) {
    if (x == 2) {
        return
    }
    println("x != 2")
}

fun returnIfElse(x: Int) {
    if (x == 2) {
        return
    } else {
        println("x != 2")
    }
    println("x")
}

fun returnWhen(x: Int) {
    when {
        x < 28 -> return
        x < 50 -> when {
            x < 41 -> println("x")
            else -> return
        }
        x < 74 -> return
        else -> println("y")
    }
    println("z")
}

fun returnValue(x: Int): Int {
    when {
        x < 28 -> return 28
        x < 50 -> when {
            x < 41 -> println("x")
            else -> return 41
        }
        x < 74 -> return 74
        else -> println("y")
    }
    println("z")
    return -1
}

fun cfgReturn() {
    returnIf(42)
    returnIfElse(42)
    returnWhen(42)
    returnValue(42)
}
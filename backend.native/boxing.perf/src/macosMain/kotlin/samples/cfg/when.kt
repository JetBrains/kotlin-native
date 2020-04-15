package samples.cfg

fun simpleIf() {
    val x = 1
    if (x < 28) {
        println("y")
    }
    val z = 2
}

fun simpleIfElse() {
    val x = 1
    if (x < 28) {
        println("y")
    } else {
        println("x")
    }
    val z = 2
}

fun simpleWhen() {
    val x = 1
    when {
        x < 28 -> println("y")
        x < 50 -> println("x")
//        else -> println("t")
    }
    val z = 2
}

fun simpleWhenElse() {
    val x = 1
    when {
        x < 28 -> println("y")
        x < 50 -> println("x")
//        else -> println("t")
    }
    val z = 2
}

fun nestedWhen() {
    val x = 1
    val y = 2
    when {
        x < 28 -> when (y) {
            1 -> println("x")
            2 -> println("y")
            else -> println("z")
        }
        x < 50 -> println("t")
    }
    val z = 3
}

fun cfgWhen() {
    simpleIf()
    simpleIfElse()
    simpleWhen()
    simpleWhenElse()
    nestedWhen()
}
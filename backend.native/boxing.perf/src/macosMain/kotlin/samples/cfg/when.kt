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

fun whenInitializer() {
    val x = 1
    val y = when (x) {
        1 -> 1
        2 -> 1
        3 -> 2
        4 -> 3
        5 -> 5
        6 -> 8
        7, 9 -> 13
        else -> 9000
    }
    val z = 2
}

fun whenSetValue() {
    val x = 1
    var y = 0
    y = when (x) {
        1 -> 1
        2 -> 1
        3 -> 2
        4 -> 3
        5 -> 5
        6 -> 8
        7, 9 -> 13
        else -> 9000
    }
    val z = 2
}

fun whenReturn(x: Int, y: Char) = when (x) {
    1, 2, 3 -> 3
    4, 5, 6 -> 6
    7, 8, 9 -> when (y) {
        '*', '0', '#' -> 0
        else -> 9000
    }
    else -> 0
}

fun emptyIf(x: Int) {
    if (x < 5) {}
}

fun emptyIfElse(x: Int) {
    if (x < 5) {} else {}
}

fun emptyIfOnly(x: Int) {
    if (x < 5) {} else { println("x") }
}

fun emptyElseOnly(x: Int) {
    if (x < 5) { println("x") } else {}
}

fun cfgWhen() {
    simpleIf()
    simpleIfElse()
    simpleWhen()
    simpleWhenElse()
    nestedWhen()
    whenInitializer()
    whenSetValue()
    whenReturn(8, '*')
    emptyIf(42)
    emptyIfElse(42)
    emptyIfOnly(42)
    emptyElseOnly(42)
}
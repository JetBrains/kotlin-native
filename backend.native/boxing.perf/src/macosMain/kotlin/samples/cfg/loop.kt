package samples.cfg

fun simpleForLoop(): Int {
    var x = 1
    for (i in 1..10) {
        x += i
    }
    return x
}

fun simpleDoWhileLoop(): Int {
    var x = 1
    var i = 0
    do {
        x += i
        i++
    } while (i < 10)
    return x
}

fun simpleWhileLoop(): Int {
    var x = 1
    var i = 0
    while (i < 10) {
        x += i
        i++
    }
    return x
}

fun whileLoopWithIf(): Int {
    var x = 1
    var i = 0
    while (i < 10) {
        if (i % 2 == 0) {
            x += i
        } else {
            x -= i
        }
        i++
    }
    return x
}

fun whileLoopWithIfAndReturn(): Int {
    var x = 1
    var i = 0
    while (i < 10) {
        if (i % 2 == 0) {
            x += i
        } else {
            return x
        }
        i++
    }
    return x
}

fun emptyLoop() {
    for (i in 1..10) {}
}

fun loopBreak(x: Int) {
    for (i in 1..10) {
        if (i + x == 15) {
            break
        }
        println(x + i)
    }
}

fun loopContinue(x: Int) {
    for (i in 1..10) {
        if (i + x == 15) {
            continue
        }
        println(x + i)
    }
}

fun nestedLoopBreak(x: Int) {
    for (i in 1..10) {
        for (j in 1..10) {
            if (i * j > x) {
                break
            }
            println(i * j)
        }
        println()
    }
}

fun nestedLoopContinue(x: Int) {
    for (i in 1..10) {
        for (j in 1..10) {
            if (i * j > x) {
                continue
            }
            println(i * j)
        }
        println()
    }
}

fun labeledLoopBreak(x: Int) {
    loop@ for (i in 1..10) {
        for (j in 1..10) {
            if (i * j > x) {
                break@loop
            }
            println(i * j)
        }
        println()
    }
}

fun cfgLoop() {
    simpleForLoop()
    simpleDoWhileLoop()
    simpleWhileLoop()
    whileLoopWithIf()
    whileLoopWithIfAndReturn()
    emptyLoop()
    loopBreak(8)
    loopContinue(8)
    nestedLoopBreak(42)
    nestedLoopContinue(42)
    labeledLoopBreak(42)
}
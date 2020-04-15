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

fun cfgLoop() {
    simpleForLoop()
    simpleDoWhileLoop()
    simpleWhileLoop()
    whileLoopWithIf()
    whileLoopWithIfAndReturn()
    emptyLoop()
}
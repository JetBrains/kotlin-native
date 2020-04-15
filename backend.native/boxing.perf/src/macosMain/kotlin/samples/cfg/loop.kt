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

fun cfgLoop() {
    simpleForLoop()
    simpleDoWhileLoop()
    simpleWhileLoop()
}
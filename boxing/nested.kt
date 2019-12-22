package nested

fun idLocal(x: Int): Int {
    fun <T> localId__(t: T) = t
    return localId__(x)
}

interface Foo {
    fun <T> anonId__(x: T): T
}

fun idAnonymous(x: Int): Int {
    val f = object : Foo {
        override fun <T> anonId__(x: T): T = x
    }
    return f.anonId__(x)
}

fun runCount(): Int {
    for (i in 1..10) {
        idLocal(i)
        idAnonymous(i)
    }
    return 0
}
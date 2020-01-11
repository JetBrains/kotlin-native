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

fun <T> idLocal2__(x: T): T {
    fun <U> localId__(u: U) = x
    val f = 1
    return localId__(f)
}

fun <T> idLocal3__(x: T): T {
    fun <U> localId1__(u: U) = u
    fun <V> localId2__(v: V) = localId1__(v)
    fun <W> localId3__(w: W) = localId2__(w)
    return localId3__(x)
}

fun <T> idDelegate__(x: T) = idLocal2__(x)
fun <T> idDelegate2__(x: T) = idDelegate__(x)
fun <T> idDelegate3__(x: T) = idDelegate2__(x)

fun runCount(): Int {
    for (i in 1..10) {
        idLocal(i)
        idAnonymous(i)
        idLocal2__(i)
        idLocal3__(i)
        idDelegate__(i)
        idDelegate2__(i)
        idDelegate3__(i)
    }
    return 0
}
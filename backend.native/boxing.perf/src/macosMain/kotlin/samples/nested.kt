package samples

import org.jetbrains.ring.CountBoxings

fun <U> id__(x: U) = x

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

fun <T> idMixDelegate3__(x: T) = idMixDelegate2__(x)
fun <T> idMixDelegate1__(x: T) = idDelegate__(x)
fun <T> idMixDelegate2__(x: T) = idMixDelegate1__(x)

fun <R> idManyTypes1__(x: R): R {
    return libid__(x)
}

fun <R> idManyTypes2__(x: R): R {
    return object : Foo {
        override fun <T> anonId__(x: T): T = idManyTypes1__(x)
    }.anonId__(x)
}

fun <R> idManyTypes3__(x: R): R {
    return object : Foo {
        override fun <T> anonId__(x: T): T = libid__(x)
    }.anonId__(x)
}

interface GenericFoo<T> {
    fun id__(x: T): T
}

fun <R> idManyTypes4__(x: R): R {
    return object : GenericFoo<R> {
        override fun id__(x: R): R = idManyTypes1__(x)
    }.id__(x)
}

fun <R> idManyTypes5__(x: R): R {
    return object : GenericFoo<R> {
        override fun id__(x: R): R = idManyTypes4__(x)
    }.id__(x)
}

@CountBoxings
fun runNested(): Int {
    for (i in 1..10) {
        idLocal(i)
        idAnonymous(i)
        idLocal2__(i)
        idLocal3__(i)
        idDelegate__(i)
        idDelegate3__(i)
        idMixDelegate3__(i)

        idLocal(i)
        idAnonymous(i)
        idLocal2__(i)
        idLocal3__(i)
        idDelegate__(i)
        idDelegate3__(i)
        idMixDelegate3__(i)

        idManyTypes1__(i)
        idManyTypes2__(i)
        idManyTypes3__(i)
        idManyTypes4__(i)
        idManyTypes5__(i)

        idManyTypes1__(i.toByte())
        idManyTypes2__(i.toByte())
        idManyTypes3__(i.toByte())
        idManyTypes4__(i.toByte())
        idManyTypes5__(i.toByte())

        idManyTypes1__(i.toShort())
        idManyTypes2__(i.toShort())
        idManyTypes3__(i.toShort())
        idManyTypes4__(i.toShort())
        idManyTypes5__(i.toShort())

        idManyTypes1__(i)
        idManyTypes2__(i)
        idManyTypes3__(i)
        idManyTypes4__(i)
        idManyTypes5__(i)

        idManyTypes1__(i.toByte())
        idManyTypes2__(i.toByte())
        idManyTypes3__(i.toByte())
        idManyTypes4__(i.toByte())
        idManyTypes5__(i.toByte())

        idManyTypes1__(i.toShort())
        idManyTypes2__(i.toShort())
        idManyTypes3__(i.toShort())
        idManyTypes4__(i.toShort())
        idManyTypes5__(i.toShort())
    }
    return 0
}
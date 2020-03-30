package samples

import kotlin.native.Specialized
import kotlin.native.SpecializedClass
import org.jetbrains.ring.CountBoxings

fun <@Specialized(forTypes = [Int::class]) U> id__(x: U) = x

fun idLocal(x: Int): Int {
    fun <@Specialized(forTypes = [Int::class]) T> localId__(t: T) = t
    return localId__(x)
}

interface Foo {
    fun <@Specialized(forTypes = [Int::class]) T> anonId__(x: T): T
}

fun idAnonymous(x: Int): Int {
    val f = object : Foo {
        override fun <@Specialized(forTypes = [Int::class]) T> anonId__(x: T): T = x
    }
    return f.anonId__(x)
}

fun <@Specialized(forTypes = [Int::class]) T> idLocal2__(x: T): T {
    fun <@Specialized(forTypes = [Int::class]) U> localId__(u: U) = x
    val f = 1
    return localId__(f)
}

fun <@Specialized(forTypes = [Int::class]) T> idLocal3__(x: T): T {
    fun <@Specialized(forTypes = [Int::class]) U> localId1__(u: U) = u
    fun <@Specialized(forTypes = [Int::class]) V> localId2__(v: V) = localId1__(v)
    fun <@Specialized(forTypes = [Int::class]) W> localId3__(w: W) = localId2__(w)
    return localId3__(x)
}

fun <@Specialized(forTypes = [Int::class]) T> idDelegate__(x: T) = idLocal2__(x)
fun <@Specialized(forTypes = [Int::class]) T> idDelegate2__(x: T) = idDelegate__(x)
fun <@Specialized(forTypes = [Int::class]) T> idDelegate3__(x: T) = idDelegate2__(x)

fun <@Specialized(forTypes = [Int::class]) T> idMixDelegate3__(x: T) = idMixDelegate2__(x)
fun <@Specialized(forTypes = [Int::class]) T> idMixDelegate1__(x: T) = idDelegate__(x)
fun <@Specialized(forTypes = [Int::class]) T> idMixDelegate2__(x: T) = idMixDelegate1__(x)

fun <@Specialized(forTypes = [Int::class, Byte::class, Short::class]) R> idManyTypes1__(x: R): R {
    return id__(x)
}

fun <@Specialized(forTypes = [Int::class, Byte::class, Short::class]) R> idManyTypes2__(x: R): R {
    return object : Foo {
        override fun <@Specialized(forTypes = [Int::class, Byte::class]) T> anonId__(x: T): T = idManyTypes1__(x)
    }.anonId__(x)
}

fun <@Specialized(forTypes = [Int::class, Byte::class, Short::class]) R> idManyTypes3__(x: R): R {
    return object : Foo {
        override fun <@Specialized(forTypes = [Int::class, Byte::class, Short::class]) T> anonId__(x: T): T = libid__(x)
    }.anonId__(x)
}

@SpecializedClass(forTypes = [Int::class, Byte::class, Short::class])
interface GenericFoo<T> {
    fun id__(x: T): T
}

fun <@Specialized(forTypes = [Int::class, Byte::class, Short::class]) R> idManyTypes4__(x: R): R {
    return object : GenericFoo<R> {
        override fun id__(x: R): R = idManyTypes1__(x)
    }.id__(x)
}

fun <@Specialized(forTypes = [Int::class, Byte::class, Short::class]) R> idManyTypes5__(x: R): R {
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
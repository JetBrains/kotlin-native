package org.jetbrains.ring.mapper

import org.jetbrains.ring.BENCHMARK_SIZE
import org.jetbrains.ring.RANDOM_BOUNDARY_INT
import org.jetbrains.ring.Random
import org.jetbrains.ring.SpecializedClass

interface Mapper2<T> {
    fun map(value: T): T
}

@SpecializedClass(forTypes = [Int::class])
class IdentityMapper2<T> : Mapper2<T> {
    override fun map(value: T) = value
}

@SpecializedClass(forTypes = [Int::class])
class DefaultMapper2<T>(val defaultValue: T) : Mapper2<T> {
    override fun map(value: T) = defaultValue
}

@SpecializedClass(forTypes = [Int::class])
class CompositeMapper2<T>(val mapperOne: Mapper2<T>, val mapperTwo: Mapper2<T>) : Mapper2<T> {
    override fun map(value: T) = mapperTwo.map(mapperOne.map(value))
}

fun mapper2() {
    var x = 0
    for (i in 1..BENCHMARK_SIZE) {
        val res = CompositeMapper2(
                IdentityMapper2(),
                DefaultMapper2(Random.nextDouble())
//                             ^^^^^^^^^^^^^^^^^^^ boxing here (off & on)
        ).map(i.toDouble())
//            ^^^^^^^^^^^^ boxing here (off & on)
        x = (x + (res - RANDOM_BOUNDARY_INT)).toInt() % RANDOM_BOUNDARY_INT
    }
}
package org.jetbrains.ring.mapper

import org.jetbrains.ring.BENCHMARK_SIZE
import org.jetbrains.ring.RANDOM_BOUNDARY_INT
import org.jetbrains.ring.Random
import org.jetbrains.ring.SpecializedClass

interface Mapper3<T> {
    fun map(value: T): T
}

@SpecializedClass(forTypes = [Int::class])
class IdentityMapper3<T> : Mapper3<T> {
    override fun map(value: T) = value
}

@SpecializedClass(forTypes = [Int::class])
class DefaultMapper3<T>(val defaultValue: T) : Mapper3<T> {
    override fun map(value: T) = defaultValue
}

@SpecializedClass(forTypes = [Double::class])
class CompositeMapper3<T>(val mapperOne: Mapper3<T>, val mapperTwo: Mapper3<T>) : Mapper3<T> {
    override fun map(value: T) = mapperTwo.map(mapperOne.map(value))
//                                             ^^^^^^^^^^^^^^^^^^^^ boxing here (on), bridge call
//                               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ boxing here (on), bridge call
}

// Benchmark
fun mapper3() {
    var x = 0
    for (i in 1..BENCHMARK_SIZE) {
        val res = CompositeMapper3(
                IdentityMapper3(),
                DefaultMapper3(Random.nextInt())
//                             ^^^^^^^^^^^^^^^^ boxing here (off)
        ).map(i)
//            ^ boxing here (off & on)
        x = (x + (res - RANDOM_BOUNDARY_INT)) % RANDOM_BOUNDARY_INT
    }
}
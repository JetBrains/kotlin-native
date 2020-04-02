package org.jetbrains.ring.mapper

import org.jetbrains.ring.BENCHMARK_SIZE
import org.jetbrains.ring.RANDOM_BOUNDARY_INT
import org.jetbrains.ring.Random
import org.jetbrains.ring.SpecializedClass

@SpecializedClass(forTypes = [Int::class])
interface Mapper1<T> {
    fun map(value: T): T
}

@SpecializedClass(forTypes = [Int::class])
class IdentityMapper1<T> : Mapper1<T> {
    override fun map(value: T) = value
}

@SpecializedClass(forTypes = [Int::class])
class DefaultMapper1<T>(val defaultValue: T) : Mapper1<T> {
    override fun map(value: T) = defaultValue
}

@SpecializedClass(forTypes = [Int::class])
class CompositeMapper1<T>(val mapperOne: Mapper1<T>, val mapperTwo: Mapper1<T>) : Mapper1<T> {
    override fun map(value: T) = mapperTwo.map(mapperOne.map(value))
}

// Benchmark
fun mapper1() {
    var x = 0
    for (i in 1..BENCHMARK_SIZE) {
        val res = CompositeMapper1(
                IdentityMapper1(),
                DefaultMapper1(Random.nextInt())
//                             ^^^^^^^^^^^^^^^^ boxing here (off)
        ).map(i)
//            ^ boxing here (off)
        x = (x + (res - RANDOM_BOUNDARY_INT)) % RANDOM_BOUNDARY_INT
    }
}
package org.jetbrains.ring.mapper

import org.jetbrains.ring.BENCHMARK_SIZE
import org.jetbrains.ring.RANDOM_BOUNDARY_INT
import org.jetbrains.ring.Random

interface Mapper0 {
    fun map(value: Int): Int
}

class IdentityMapper0 : Mapper0 {
    override fun map(value: Int) = value
}

class DefaultMapper0(val defaultValue: Int) : Mapper0 {
    override fun map(value: Int) = defaultValue
}

class CompositeMapper0(val mapperOne: Mapper0, val mapperTwo: Mapper0) : Mapper0 {
    override fun map(value: Int) = mapperTwo.map(mapperOne.map(value))
}

// Benchmark
fun mapper0() {
    var x = 0
    for (i in 1..BENCHMARK_SIZE) {
        val res = CompositeMapper0(
                IdentityMapper0(),
                DefaultMapper0(Random.nextInt())
//                             ^^^^^^^^^^^^^^^^ boxing here (off & on)
        ).map(i)
//            ^ boxing here (off & on)
        x = (x + (res - RANDOM_BOUNDARY_INT)) % RANDOM_BOUNDARY_INT
    }
}

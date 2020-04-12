package org.jetbrains.ring

@SpecializedClass(forTypes = [Int::class])
class SpecializableList<T>(private val array: Array<T>) {
    val size = array.size

    val isEmpty: Boolean
        get() = size == 0
    val isNotEmpty: Boolean
        get() = size > 0

    fun get(index: Int): T = array[index]
    fun set(index: Int, value: T) {
        array[index] = value
    }

    fun first() = array[0]

    fun transform(f: (T) -> T) {
        for (i in 0 until size) {
            array[i] = f(array[i])
        }
    }

    fun reduce(f: (T, T) -> T): T {
        if (isEmpty) throw RuntimeException()
        var result = get(0)
        for (i in 1 until size) {
            result = f(result, get(i))
        }
        return result
    }
}

// Benchmark
fun testListReduce(): Int {
    var x = 0
    for (i in 1..BENCHMARK_SIZE) {
        val list = SpecializableList(arrayOf(
                Random.nextInt(),
                Random.nextInt(),
                Random.nextInt(),
                Random.nextInt(),
                Random.nextInt()
        ))
        x = (x + (list.reduce { acc, v -> acc + v } - RANDOM_BOUNDARY_INT)) % RANDOM_BOUNDARY_INT
    }
    return x
}

@SpecializedClass(forTypes = [Int::class])
class Matrix2<T>(
        var i00: T, var i01: T,
        var i10: T, var i11: T
)

@SpecializedClass(forTypes = [Int::class])
class Matrix3<T>(
        var i00: T, var i01: T, var i02: T,
        var i10: T, var i11: T, var i12: T,
        var i20: T, var i21: T, var i22: T
)

// Benchmark
fun testMatrix2(): Int {
    var x = 0
    for (i in 1..BENCHMARK_SIZE) {
        val m = Matrix2(
                Random.nextInt(), Random.nextInt(),
                Random.nextInt(), Random.nextInt()
        )
        x = (x + (m.i00 + m.i01 + m.i10 + m.i11 - RANDOM_BOUNDARY_INT)) % RANDOM_BOUNDARY_INT
    }
    return x
}

// Benchmark
fun testMatrix3(): Int {
    var x = 0
    for (i in 1..BENCHMARK_SIZE) {
        val m = Matrix3(
                Random.nextInt(), Random.nextInt(), Random.nextInt(),
                Random.nextInt(), Random.nextInt(), Random.nextInt(),
                Random.nextInt(), Random.nextInt(), Random.nextInt()
        )
        x = (x + (m.i00 + m.i01 + m.i02 + m.i10 + m.i11 + m.i12 + m.i20 + m.i21 + m.i22 - RANDOM_BOUNDARY_INT)) % RANDOM_BOUNDARY_INT
    }
    return x
}
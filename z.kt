/*class A(val x: Int)

class B(val x: Int)

fun main(args: Array<String>) {
    val a = A(42)
    var i = 0
    while (i < 5) {
        val b = B(i)
	if (b.x == a.x)
	    println("True")
	i = i + 1
    }
}*/

val BENCHMARK_SIZE = 100

inline fun <T: Any> loadGenericInline(value: T, size: Int): Int {
    var acc = 0
    for (i in 0..size) {
        acc = acc xor value.hashCode()
    }
    return acc
}

open class InlineBenchmark {
    private var value = 2138476523

    fun calculateGenericInline(): Int {
        return loadGenericInline(value, BENCHMARK_SIZE)
    }
}

fun main(args: Array<String>) {
    val benchmark = InlineBenchmark()
    for (j in 0..99)
        for (i in 0..32767)
	    benchmark.calculateGenericInline()
}

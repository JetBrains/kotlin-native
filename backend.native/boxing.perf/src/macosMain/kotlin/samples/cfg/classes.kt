package samples.cfg

class A(val x: Int) {

    init {
        println(x)
    }

    constructor(s: String) : this(s.length) {
        println(s)
    }

    fun plus(y: Int) = x + y
    fun plus(y: String) = plus(y.toA().x)

    fun String.toA() = A(length)

    companion object {
        fun of(x: Int) = A(x)
    }
}

fun A.times(other: A) = A(x * other.x)
fun A.map(f: (Int) -> Int) = A(f(x))


class B(var x: Int)

fun B.foo() {
    for (i in 1..10) {
        if (i % 2 == 0) {
            x++
        }
        println(x)
    }
}

fun cfgClasses() {
    val a1 = A(42)
    val a2 = A("Hello world!")
    val a3 = A.of(81)
    val a4 = a3.plus("foo")
    val a5 = a1.map { it * 2 }.times(a2.map { it - 1 })
    println(a3.plus(a5.plus(a4)))

    B(42).foo()
}
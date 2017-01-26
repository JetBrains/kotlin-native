fun <T> foo(x: T, y: Int = 1) = y

fun main(args: Array<String>) {
    println(foo(Any()))
}
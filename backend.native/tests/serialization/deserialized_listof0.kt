
fun test_arrayList() {
    val l = listOf(1, 2, 3)
    val m = listOf<Int>()
    val n = l + m
    println(n)
}

fun <T> test_arrayList2(x: T, y: T, z: T) {
    val l = listOf<T>(x, y, z)
    val m = listOf<T>()
    val n = m + l
    println(l)
}

fun test_arrayList3() {
    val l = listOf<String>()
    val m = listOf<String>("a", "b", "c")
    val n = l + m
    println(n)
}

fun main(args: Array<String>) {
    test_arrayList()
    test_arrayList2<Int>(5, 6, 7)
    test_arrayList2<String>("a", "b", "c")
    test_arrayList3()
}


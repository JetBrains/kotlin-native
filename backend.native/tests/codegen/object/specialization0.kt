fun <T, S> box(value: T): T {
    return value
}

@Specialization("kfun:box(T)", "Int", "Float")
fun box_Int_Float(value: Int): Int {
    return value+2
}

fun main(args: Array<String>) {
    val b  = box<Int,Float>(15)
    println(b)
    val b2 = box_Int_Float(15)
    println(b2)
}


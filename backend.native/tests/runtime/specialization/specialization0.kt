
@Specialization("kfun:box_Int_Float(Int)", "Int", "Float")
@Specialization("kfun:box_Short_Double(Short)", "Short", "Double")
fun <T, S> box(value: T): T {
    return value
}

fun box_Int_Float(value: Int): Int {
    return value+2
}

fun box_Short_Double(value: Short): Short {
    return value
}

fun main(args: Array<String>) {
    val b1 = box_Short_Double(15.toShort())
    println(b1)

    val b2 = box_Int_Float(15)
    println(b2)

    val b3  = box<Short,Double>(15.toShort())
    println(b3)

    val b4  = box<Int,Float>(15)
    println(b4)
}


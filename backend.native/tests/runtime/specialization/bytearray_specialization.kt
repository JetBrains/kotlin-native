
fun main(args: Array<String>) {
    val x = ByteArray(2)
    val y = x.iterator()

    val a = Array<Byte>(5)

    var i = 0
    for (b in a) {
        a[i] = i.toByte()
        i = i+1
    }

    var sum = 0
    for (b in a) {
        sum += b.toInt()
    }
    println(sum)
}



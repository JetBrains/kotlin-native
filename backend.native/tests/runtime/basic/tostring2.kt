fun main(args : Array<String>) {
    val hello = "Hello"
    val array = toCharArray(hello)
    for (ch in array) {
        print(ch)
        print(" ")
    }
    println()
    println(fromCharArray(array, 0, array.size))
    {
        val values = ByteArray(2)
        values[0] = Byte.MIN_VALUE
        values[1] = Byte.MAX_VALUE
        for (v in values) {
            println(v)
        }
    }
    {
        val values = ShortArray(2)
        values[0] = Short.MIN_VALUE
        values[1] = Short.MAX_VALUE
        for (v in values) {
            println(v)
        }
    }
    {
        val values = IntArray(2)
        values[0] = Int.MIN_VALUE
        values[1] = Int.MAX_VALUE
        for (v in values) {
            println(v)
        }
    }
    {
        val values = FloatArray(5)
        values[0] = Float.MIN_VALUE
        values[1] = Float.MAX_VALUE
        values[2] = Float.NEGATIVE_INFINITY
        values[3] = Float.POSITIVE_INFINITY
        values[4] = Float.NaN
        for (v in values) {
            println(v)
        }
    }
    {
        val values = DoubleArray(5)
        values[0] = Double.MIN_VALUE
        values[1] = Double.MAX_VALUE
        values[2] = Double.NEGATIVE_INFINITY
        values[3] = Double.POSITIVE_INFINITY
        values[4] = Double.NaN
        for (v in values) {
            println(v)
        }
    }
}
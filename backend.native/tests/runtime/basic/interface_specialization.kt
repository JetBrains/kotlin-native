
interface III<T: Number> {
    fun foo(x: T, y: T): T 
}

@Specialization("kinf:III", "Float")
interface III_Float {
    fun foo(x: Float, y: Float): Float 
}

class AAA<T: Number>: III<T> {
    override fun foo(x: T, y: T):T  {
        print("Generic ")
        return x
    }
}

@Specialization("kclass:AAA", "Float")
class A_Int: III<Float> {
    override fun foo(x: Float, y: Float): Float {
        print("Specific Float ")
        print(x)
        print(" ")
        println(y)
        return x
    }
}

fun main(args: Array<String>) {
    val abyte: III<Byte>   = AAA<Byte>()
    val afloat: III<Float>  = AAA<Float>()

    // The non-specialized one doesn't work because we lack boxing
    //var b = abyte.foo(1, 3)

    var f = afloat.foo(1.2f, 3.4f)
}


public class A {

    fun setMyStr() {
        str = "OK"
    }

    fun getMyStr(): String {
        return str
    }

    @kotlin.native.ThreadLocal
    private companion object {
        private lateinit var str: String
    }
}

fun box(): String {
    val a = A()
    a.setMyStr()
    return a.getMyStr()
}
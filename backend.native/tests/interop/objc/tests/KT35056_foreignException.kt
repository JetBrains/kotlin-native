import kotlin.test.*
import objcTests.*
import kotlin.native.ForeignException
import kotlinx.cinterop.*


fun myExcCaught(e: Exception) {
    println("myExcCaught> $e")
}

fun myFinally() {
    println("myFinally> finally block>")
}

fun testInner(): String {
    var ret = ""
    try {
        raiseExc("Fire native exception!")
    } catch (e: ArithmeticException) {
        println("testInner> $e") // shouldn't happen
    } finally {
        ret = "testInner> finally block is OK"
    }
    return ret
}

@Test fun testKT35056() {
    try {
        val ret = testInner()
        assertEquals("testInner> finally block is OK", ret)
    } catch (e: ForeignException) {
        println("ForeignException caught: $e")
        val ret = logExc(e.nativeException)
        assertEquals("Fire native exception!", ret)
    } finally {
        myFinally()
    }
}

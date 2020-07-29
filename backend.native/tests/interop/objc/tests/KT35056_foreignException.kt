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

fun testInner() {
    try {
        raiseExc("Fire native exception!")
    } catch (e: ArithmeticException) {
        println("testInner> $e") // shouldn't happen
    } finally {
        println("testInner> finally block")
    }

}

@Test fun testKT35056() {
    try {
        testInner()
    } catch (e: ForeignException) {
        myExcCaught(e)
        val ret = logExc(e.objCException)
        assertEquals("Fire native exception!", ret)
    } finally {
        myFinally()
    }
}

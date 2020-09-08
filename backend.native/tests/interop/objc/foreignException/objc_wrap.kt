import kotlin.test.*
//import objcTests.*
import objc_wrap.*
import kotlinx.cinterop.*


fun testInner(name: String, reason: String) {
    var finallyBlockTest = "FAILED"
    var catchBlockTest = "NOT EXPECTED"
    try {
        raiseExc(name, reason)
    } catch (e: RuntimeException) {
        catchBlockTest = "This shouldn't happen"
    } finally {
        finallyBlockTest = "PASSED"
    }
    assertEquals("NOT EXPECTED", catchBlockTest)
    assertEquals("PASSED", finallyBlockTest)
}

@Test fun testKT35056() {
    val name = "Some native exception"
    val reason = "Illegal value"
    var finallyBlockTest = "FAILED"
    var catchBlockTest = "FAILED"
    try {
        testInner(name, reason)
    } catch (e: ForeignException) {
        val ret = logExc(e.nativeException) // return NSException name
        assertEquals(name, ret)
        assertEquals("$name:: $reason", e.message)
        catchBlockTest = "PASSED"
    } finally {
        finallyBlockTest = "PASSED"
    }
    assertEquals("PASSED", catchBlockTest)
    assertEquals("PASSED", finallyBlockTest)
}

fun main() {
    testKT35056()
}
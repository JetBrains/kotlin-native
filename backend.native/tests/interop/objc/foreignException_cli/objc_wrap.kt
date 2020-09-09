import kotlin.test.*
import objc_wrap.*
import kotlinx.cinterop.*
import platform.objc.*
import kotlin.system.exitProcess

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
        println("OK: ForeignException")
        catchBlockTest = "PASSED"
    } finally {
        finallyBlockTest = "PASSED"
    }
    assertEquals("PASSED", catchBlockTest)
    assertEquals("PASSED", finallyBlockTest)
}

fun abnormal_handler(x: Any?) : Unit {
    println("OK: Ends with uncaught exception handler")
    exitProcess(0)
}

fun main() {
    // Depending on the `foreignxceptionMode` option (def file or cinterop cli) this test should ends
    // normally with `ForeignException` handled or abnormally with `abnormal_handler`.
    // Test shall validate output (golden value) from `abnormal_handler`.

    objc_setUncaughtExceptionHandler(staticCFunction(::abnormal_handler))

    testKT35056()
}
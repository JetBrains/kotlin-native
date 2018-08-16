
import kotlin.test.*

@Test fun runTest() {
    var log = ""
    fun getIncr(ret: Int, impure: String): Int = ret.apply { log += impure }

    run {
        log = ""
        val result = getIncr(-10, "A") in getIncr(0, "B") until getIncr(10, "C")
        print(log)
        print(result)
        println()
    }

    run {
        log = ""
        val result = getIncr(5, "A") in getIncr(0, "B") until getIncr(10, "C")
        print(log)
        print(result)
        println()
    }
}

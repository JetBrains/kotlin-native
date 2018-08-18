package runtime.time.time0

import kotlin.system.*
import kotlin.test.*

@Test fun runTest() {
    val time1 = getTimeMillis()
    if (time1 >= 1508887000000) {
        println("since-epoch")
    }
    while (true) {
        val time2 = getTimeMillis()
        if (time2 > time1) {
            println("time-advances")
            break
        }
    }
}

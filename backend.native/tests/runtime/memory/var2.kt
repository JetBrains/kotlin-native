package runtime.memory.var2

import kotlin.test.*

@Test fun runTest() {
    var x = Any()

    for (i in 0..1) {
        val c = Any()
        if (i == 0) x = c
    }

    // x refcount is 1.

    val y = try {
        x
    } finally {
        x = Any()
    }

    y.use()
}

fun Any?.use() {
    var x = this
}
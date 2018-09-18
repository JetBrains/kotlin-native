package runtime.workers.worker10

import kotlin.test.*

import kotlin.native.concurrent.*

class Data(val x: Int)

val topInt = 1
val topString = "string"
val topData = Data(42)
@SharedImmutable
val topSharedData = Data(43)

@Test fun runTest() {
    val worker = Worker.start()


    assertEquals(topInt, 1)
    assertEquals(topString, "string")
    assertEquals(topData.x, 42)

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> topInt == 1
    }).consume {
        result -> assertEquals(true, result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> topString == "string"
    }).consume {
        result -> assertEquals(true, result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> try {
            topData.x == 42
        } catch (e: IncorrectDereferenceException) {
            false
        }
    }).consume {
        result -> assertEquals(false, result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> try {
            topSharedData.x == 43
        } catch (e: Throwable) {
            false
        }
    }).consume {
        result -> assertEquals(true, result)
    }

    worker.requestTermination().result
    println("OK")
}
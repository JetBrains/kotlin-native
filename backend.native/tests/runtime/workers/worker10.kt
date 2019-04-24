package runtime.workers.worker10

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlinx.cinterop.StableRef

class Data(val x: Int)

val topInt = 1
val topString = "string"
var topStringVar = "string"
val topSharedStringWithGetter: String
        get() = "top"
val topData = Data(42)
@SharedImmutable
val topSharedData = Data(43)

@Test fun runTest1() {
    val worker = Worker.start()

    assertEquals(1, topInt)
    assertEquals("string", topString)
    assertEquals(42, topData.x)
    assertEquals(43, topSharedData.x)
    assertEquals("top", topSharedStringWithGetter)

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
        topStringVar == "string"
    } catch (e: IncorrectDereferenceException) {
        false
    }
    }).consume {
        result -> assertEquals(false, result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> try {
        topSharedStringWithGetter == "top"
    } catch (e: IncorrectDereferenceException) {
        false
    }
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

val atomicRef = AtomicReference<Any?>(Any().freeze())
@SharedImmutable
val stableRef = StableRef.create(Any().freeze())
val semaphore = AtomicInt(0)

@Test fun runTest2() {
    semaphore.value = 0
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { null }) {
        val value = atomicRef.value
        semaphore.increment()
        while (semaphore.value != 2) {}
        println(value.toString() != "")
    }
    while (semaphore.value != 1) {}
    atomicRef.value = null
    kotlin.native.internal.GC.collect()
    semaphore.increment()
    future.result
    worker.requestTermination().result
}

@Test fun runTest3() {
    semaphore.value = 0
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { null }) {
        val value = stableRef.get()
        semaphore.increment()
        while (semaphore.value != 2) {}
        println(value.toString() != "")
    }
    while (semaphore.value != 1) {}
    stableRef.dispose()
    kotlin.native.internal.GC.collect()
    semaphore.increment()
    future.result
    worker.requestTermination().result
}
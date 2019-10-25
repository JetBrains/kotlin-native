/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker4

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest1() {
    withWorker {
        val future = execute(TransferMode.SAFE, { 41 }) { input ->
            input + 1
        }
        future.consume { result ->
            println("Got $result")
        }
    }
    println("OK")
}

@Test fun runTest2() {
    withWorker {
        val counter = AtomicInt(0)

        executeAfter(0, {
            assertTrue(Worker.current.park(10_000_000_000, false))
            assertEquals(counter.value, 0)
            assertTrue(Worker.current.processQueue())
            assertEquals(1, counter.value)
            // Let main proceed.
            counter.increment()  // counter becomes 2 here.
            assertTrue(Worker.current.park(10_000_000_000, true))
            assertEquals(3, counter.value)
        }.freeze())

        executeAfter(0, {
            counter.increment()
        }.freeze())

        while (counter.value == 1) {
            Worker.current.park(1_000_000)
        }

        executeAfter(0, {
            counter.increment()
        }.freeze())

        while (counter.value == 2) {
            Worker.current.park(1_000_000)
        }
    }
}

@Test fun runTest3() {
    val worker = Worker.start(name = "Lumberjack")
    val counter = AtomicInt(0)
    worker.executeAfter(0, {
        assertEquals("Lumberjack", Worker.current.name)
        counter.increment()
    }.freeze())

    while (counter.value == 0) {
        Worker.current.park(1_000_000)
    }
    assertEquals("Lumberjack", worker.name)
    worker.requestTermination().result
    assertFailsWith<IllegalStateException> {
        println(worker.name)
    }
}
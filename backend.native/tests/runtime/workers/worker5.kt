/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker5

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { "zzz" }) {
        input -> input.length
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().result
    println("OK")
}

var done = false

@Test fun runTest1() {
    val worker = Worker.currentOrInit
    done = false
    // Here we request execution of the operation on the current worker.
    worker.executeAfter(0, {
        done = true
    }.freeze())
    while (!done)
        worker.processQueue()
    Worker.currentDeinit()
}

@Test fun runTest2() {
    val worker = Worker.currentOrInit
    val future = worker.requestTermination(false)
    worker.processQueue()
    assertEquals(future.state, FutureState.COMPUTED)
    future.consume {}
    Worker.currentDeinit()
}

@Test fun runTest3() {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { }) {
        Worker.currentDeinit()
    }.result
    // Ensure worker is terminated.
    assertFailsWith<IllegalStateException> { worker.execute(TransferMode.SAFE, { }) { println("BUG") }.result }
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker12

import kotlin.test.*

import kotlin.native.concurrent.*

const val MAX_REQUESTS = 100

class ConsumerState {
    var index = 0
    val seen = mutableSetOf<Future<String>>()
    fun isUnprocessed(): Boolean = seen.size < MAX_REQUESTS
}

@ThreadLocal
var consumerState: ConsumerState? = null

@Test fun runTest() {
    val workerProducer = Worker.start()
    val workerConsumer = Worker.start()

    workerConsumer.execute(TransferMode.SAFE, { Unit }, { consumerState = ConsumerState() }).result

    workerConsumer.setFutureProcessor<String> {
        it ->
        consumerState!!.seen += it
        assertEquals(it.result, "Producer: input #${consumerState!!.index++}")
    }

    val futures = mutableSetOf<Future<String>>()
    repeat(MAX_REQUESTS ) {
        it ->
        futures += workerProducer.execute(TransferMode.SAFE, { "input #$it" }) {
            input -> "Producer: $input"
        }.also { it.setFutureSubscriber(workerConsumer) }
    }

    while (workerConsumer.execute(TransferMode.SAFE, { Unit }, { _  -> consumerState!!.isUnprocessed() }).result) {
        waitForMultipleFutures(futures, 1000)
    }

    workerProducer.requestTermination(true).result
    workerConsumer.requestTermination(true).result
    println("OK")
}

@ThreadLocal
var thenState: ThenState? = null

class ThenState {
    init {
        Worker.current?.let {
            it.setFutureProcessor<Any?> {
                future ->
                thenState!!.pending.remove(future)?.let {
                    callback -> callback(future)
                }
            }
        }
    }

    fun deinit() {
        Worker.current?.let {
            it.setFutureProcessor<Any?> {}
            thenState = null
        }
    }

    internal val pending = mutableMapOf<Future<*>, (Future<*>) -> Unit>()

    fun <T> register(future: Future<T>, code: (Future<T>) -> Unit) {
        Worker.current?.let { it ->
            @Suppress("UNCHECKED_CAST")
            pending[future] = code as (Future<*>) -> Unit
            future.setFutureSubscriber(it)
        }
    }
}

@SharedImmutable
val spinner = AtomicInt(0)

fun <T> Future<T>.then(code: (Future<T>) -> Unit) {
    thenState!!.register(this, code)
}

@Test fun testThen() {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { 20 }, {
        base ->
        thenState = ThenState()
        Worker.current!!.execute(TransferMode.SAFE, { base }, { it -> it + 1}).then {
            future ->
            val current = future.result
            println("then $current")
            spinner.increment()
            Worker.current!!.execute(TransferMode.SAFE, { current }, { it -> it + 2}).then {
                future2 ->
                val current = future2.result
                println("then $current")
                spinner.increment()
            }
        }
    }).discard()
    // Kinda spinlock.
    while (spinner.value < 2) {}
    worker.requestTermination(true).result
}

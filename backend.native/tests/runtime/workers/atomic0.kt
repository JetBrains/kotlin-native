package runtime.workers.atomic0

import kotlin.test.*

import konan.worker.*

fun test1(workers: Array<Worker>) {
    val atomic = AtomicInt(15)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].schedule(TransferMode.CHECKED, { atomic }) {
            input -> input.increment()
        }
    })
    futures.forEach {
        it.result()
    }
    println(atomic.get())
}

fun test2(workers: Array<Worker>) {
    val atomic = AtomicInt(0)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].schedule(TransferMode.CHECKED, { atomic to workerIndex }) {
            (place, index) ->
            while (place.compareAndSwap(index, index + 1) != index) {}
            println(index)
        }
    })
    futures.forEach {
        it.result()
    }
    println(atomic.get())
}

@Test fun runTest() {
    val COUNT = 20
    val workers = Array(COUNT, { _ -> startWorker()})

    test1(workers)
    test2(workers)
}
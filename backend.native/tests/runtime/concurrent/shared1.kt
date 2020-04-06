/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.concurrent.shared1

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.native.ref.WeakReference

class A(var a: Int)

val global1: DisposableSharedRef<A> = DisposableSharedRef(A(3))

@Test fun testGlobal() {
    assertEquals(3, global1.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global1
    }

    val value = future.result
    assertEquals(3, value.get().a)
    worker.requestTermination().result
}

val global2: DisposableSharedRef<A> = DisposableSharedRef(A(3))

@Test fun testGlobalDenyAccessOnWorker() {
    assertEquals(3, global2.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        var result = 0
        assertFailsWith<IncorrectDereferenceException> {
            result = global2.get().a
        }
        result
    }

    val value = future.result
    assertEquals(0, value)
    worker.requestTermination().result
}

val global3: DisposableSharedRef<A> = DisposableSharedRef(A(3).freeze())

@Test fun testGlobalAccessOnWorkerFrozenInitially() {
    assertEquals(3, global3.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global3.get().a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global4: DisposableSharedRef<A> = DisposableSharedRef(A(3))

@Test fun testGlobalAccessOnWorkerFrozenBeforePassing() {
    assertEquals(3, global4.get().a)
    global4.get().freeze()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global4.get().a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global5: DisposableSharedRef<A> = DisposableSharedRef(A(3))

@Test fun testGlobalAccessOnWorkerFrozenBeforeAccess() {
    val semaphore: AtomicInt = AtomicInt(0)

    assertEquals(3, global5.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        global5.get().a
    }

    while (semaphore.value < 1) {}
    global5.get().freeze()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global6: DisposableSharedRef<A> = DisposableSharedRef(A(3))

@Test fun testGlobalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    assertEquals(3, global6.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
        semaphore.increment()
        while (semaphore.value < 2) {}
        global6
    }

    while (semaphore.value < 1) {}
    global6.get().a = 4
    semaphore.increment()

    val value = future.result
    assertEquals(4, value.get().a)
    worker.requestTermination().result
}

val global7: DisposableSharedRef<A> = DisposableSharedRef(A(3))

@Test fun testGlobalDispose() {
    assertEquals(3, global7.get().a)

    global7.dispose()
    global7.dispose()
}

val global8: DisposableSharedRef<A> = DisposableSharedRef(A(3))

@Test fun testGlobalAccessAfterDispose() {
    assertEquals(3, global8.get().a)

    global8.dispose()
    assertFailsWith<IllegalStateException> {
        global8.get().a
    }
}

@Test fun testLocal() {
    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local
    }

    val value = future.result
    assertEquals(3, value.get().a)
    worker.requestTermination().result
}

@Test fun testLocalDenyAccessOnWorker() {
    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        var result = 0
        assertFailsWith<IncorrectDereferenceException> {
            result = local.get().a
        }
        result
    }

    val value = future.result
    assertEquals(0, value)
    worker.requestTermination().result
}

@Test fun testLocalAccessOnWorkerFrozenInitially() {
    val local = DisposableSharedRef(A(3).freeze())
    assertEquals(3, local.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.get().a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test fun testLocalAccessOnWorkerFrozenBeforePassing() {
    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)
    local.get().freeze()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.get().a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test fun testLocalAccessOnWorkerFrozenBeforeAccess() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        local.get().a
    }

    while (semaphore.value < 1) {}
    local.get().freeze()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test fun testLocalDenyAccessOnMainThread() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        DisposableSharedRef(A(3))
    }

    val value = future.result
    assertFailsWith<IncorrectDereferenceException> {
        value.get()
    }

    worker.requestTermination().result
}

@Test fun testLocalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}
        local
    }

    while (semaphore.value < 1) {}
    local.get().a = 4
    semaphore.increment()

    val value = future.result
    assertEquals(4, value.get().a)
    worker.requestTermination().result
}

@Test fun testLocalDispose() {
    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    local.dispose()
    local.dispose()
}

@Test fun testLocalAccessAfterDispose() {
    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    local.dispose()
    assertFailsWith<IllegalStateException> {
        local.get().a
    }
}

fun getWeaksAndAtomicReference(initial: Int): Triple<AtomicReference<DisposableSharedRef<A>?>, WeakReference<DisposableSharedRef<A>>, WeakReference<A>> {
    val local = DisposableSharedRef(A(initial))
    val localRef: AtomicReference<DisposableSharedRef<A>?> = AtomicReference(local)
    val localWeak = WeakReference(local)
    val localValueWeak = WeakReference(local.get())

    assertNotNull(localWeak.get())
    assertNotNull(localValueWeak.get())

    return Triple(localRef, localWeak, localValueWeak)
}

fun <T: Any> callDispose(ref: AtomicReference<DisposableSharedRef<T>?>) {
    ref.value!!.dispose()
}

@Test fun testCollect() {
    val (localRef, localWeak, localValueWeak) = getWeaksAndAtomicReference(3)

    localRef.value = null
    GC.collect()

    // Last reference to DisposableSharedRef is gone, so it and it's referent are destroyed.
    assertNull(localWeak.get())
    assertNull(localValueWeak.get())
}

@Test fun testDisposeAndCollect() {
    val (localRef, localWeak, localValueWeak) = getWeaksAndAtomicReference(3)

    callDispose(localRef)
    GC.collect()

    // localRef still contains a reference to DisposableSharedRef. But it's referent is
    // destroyed because of explicit dispose call.
    assertNotNull(localWeak.get())
    assertNull(localValueWeak.get())
}

fun collectInWorker(worker: Worker, semaphore: AtomicInt): Pair<WeakReference<A>, Future<Unit>> {
    val (localRef, _, localValueWeak) = getWeaksAndAtomicReference(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(localRef, semaphore) }) { (localRef, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        localRef.value = null
        GC.collect()
    }

    while (semaphore.value < 1) {}
    // At this point worker is spinning on semaphore. localRef still contains reference to
    // DisposableSharedRef, so referent is kept alive.
    GC.collect()
    assertNotNull(localValueWeak.get())

    return Pair(localValueWeak, future)
}

@Test fun testCollectInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (localValueWeak, future) = collectInWorker(worker, semaphore)
    semaphore.increment()
    future.result

    // At this point DisposableSharedRef no longer has a reference, so it's referent is destroyed.
    // DisposableSharedRef, so referent is kept alive.
    GC.collect()
    assertNull(localValueWeak.get())

    worker.requestTermination().result
}

fun doNotCollectInWorker(worker: Worker, semaphore: AtomicInt): Future<DisposableSharedRef<A>> {
    val local = DisposableSharedRef(A(3))

    return worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        GC.collect()
        local
    }
}

@Test fun testDoNotCollectInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val future = doNotCollectInWorker(worker, semaphore)
    while (semaphore.value < 1) {}
    GC.collect()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value.get().a)
    worker.requestTermination().result
}

fun disposeInWorker(worker: Worker, semaphore: AtomicInt): Triple<WeakReference<DisposableSharedRef<A>>, WeakReference<A>, Future<AtomicReference<DisposableSharedRef<A>?>>> {
    val (localRef, localWeak, localValueWeak) = getWeaksAndAtomicReference(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(localRef, semaphore) }) { (localRef, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        callDispose(localRef)
        GC.collect()
        localRef
    }

    while (semaphore.value < 1) {}
    // At this point worker is spinning on semaphore. localRef still contains reference to
    // DisposableSharedRef, so referent is kept alive.
    GC.collect()
    assertNotNull(localValueWeak.get())

    return Triple(localWeak, localValueWeak, future)
}

@Test fun testDisposeInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (localWeak, localValueWeak, future) = disposeInWorker(worker, semaphore)
    semaphore.increment()
    val localRef = future.result

    // At this point localRef still has a reference, but it's explicitly disposed,
    // so referent is destroyed.
    GC.collect()
    assertNotNull(localWeak.get())
    assertNull(localValueWeak.get())

    worker.requestTermination().result
}

@Test fun testDisposeOnMainThreadAndAccessInWorker() {
    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    local.dispose()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        var result = 0
        assertFailsWith<IllegalStateException> {
            result = local.get().a
        }
        result
    }

    val value = future.result
    assertEquals(0, value)
    worker.requestTermination().result
}

@Test fun testDisposeInWorkerAndAccessOnMainThread() {
    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.dispose()
    }

    future.result
    assertFailsWith<IllegalStateException> {
        local.get().a
    }
    worker.requestTermination().result
}

class B1 {
    lateinit var b2: DisposableSharedRef<B2>
}

data class B2(val b1: DisposableSharedRef<B1>)

fun createCyclicGarbage(): Triple<AtomicReference<DisposableSharedRef<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val b1 = DisposableSharedRef(B1())
    val refB1: AtomicReference<DisposableSharedRef<B1>?> = AtomicReference(b1)
    val weakB1 = WeakReference(b1.get())

    val b2 = DisposableSharedRef(B2(b1))
    val weakB2 = WeakReference(b2.get())

    b1.get().b2 = b2

    return Triple(refB1, weakB1, weakB2)
}

@Test fun doesNotCollectCyclicGarbage() {
    val (refB1, weakB1, weakB2) = createCyclicGarbage()

    refB1.value = null
    GC.collect()

    // If these asserts fail, that means DisposableSharedRef managed to clean up cyclic garbage all by itself.
    assertNotNull(weakB1.get())
    assertNotNull(weakB2.get())
}

@Test fun collectCyclicGarbageWithExplicitDispose() {
    val (refB1, weakB1, weakB2) = createCyclicGarbage()

    callDispose(refB1)
    GC.collect()

    assertNull(weakB1.get())
    assertNull(weakB2.get())
}

fun createCrossThreadCyclicGarbage(
    worker: Worker
): Triple<AtomicReference<DisposableSharedRef<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val b1 = DisposableSharedRef(B1())
    val refB1: AtomicReference<DisposableSharedRef<B1>?> = AtomicReference(b1)
    val weakB1 = WeakReference(b1.get())

    val future = worker.execute(TransferMode.SAFE, { b1 }) { b1 ->
        val b2 = DisposableSharedRef(B2(b1))
        Pair(b2, WeakReference(b2.get()))
    }
    val (b2, weakB2) = future.result

    b1.get().b2 = b2

    return Triple(refB1, weakB1, weakB2)
}

@Test fun doesNotCollectCrossThreadCyclicGarbage() {
    val worker = Worker.start()

    val (refB1, weakB1, weakB2) = createCrossThreadCyclicGarbage(worker)

    refB1.value = null
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    // If these asserts fail, that means DisposableSharedRef managed to clean up cyclic garbage all by itself.
    assertNotNull(weakB1.get())
    assertNotNull(weakB2.get())

    worker.requestTermination().result
}

@Test fun collectCrossThreadCyclicGarbageWithExplicitDispose() {
    val worker = Worker.start()

    val (refB1, weakB1, weakB2) = createCrossThreadCyclicGarbage(worker)

    callDispose(refB1)
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    assertNull(weakB1.get())
    assertNull(weakB2.get())

    worker.requestTermination().result
}

@Test fun concurrentAccess() {
    val workerCount = 10
    val workerUnlocker = AtomicInt(0)

    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val workers = Array(workerCount) {
        Worker.start()
    }
    val futures = Array(workers.size) {
        workers[it].execute(TransferMode.SAFE, { Pair(local, workerUnlocker) }) { (local, workerUnlocker) ->
            while (workerUnlocker.value < 1) {}

            assertFailsWith<IncorrectDereferenceException> {
                local.get()
            }
            Unit
        }
    }
    workerUnlocker.increment()

    for (future in futures) {
        future.result
    }

    for (worker in workers) {
        worker.requestTermination().result
    }
}

@Test fun concurrentDispose() {
    val workerCount = 10
    val workerUnlocker = AtomicInt(0)

    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val workers = Array(workerCount) {
        Worker.start()
    }
    val futures = Array(workers.size) {
        workers[it].execute(TransferMode.SAFE, { Pair(local, workerUnlocker) }) { (local, workerUnlocker) ->
            while (workerUnlocker.value < 1) {}

            local.dispose()
        }
    }
    workerUnlocker.increment()

    for (future in futures) {
        future.result
    }

    assertFailsWith<IllegalStateException> {
        local.get().a
    }

    for (worker in workers) {
        worker.requestTermination().result
    }
}

@Test fun concurrentDisposeAndAccess() {
    val workerUnlocker = AtomicInt(0)

    val local = DisposableSharedRef(A(3))
    assertEquals(3, local.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, workerUnlocker) }) { (local, workerUnlocker) ->
        while (workerUnlocker.value < 1) {}

        local.dispose()
    }
    workerUnlocker.increment()

    var result = 0
    // This is a race, but it should either get value successfully or get IllegalStateException.
    // Any other kind of failure is unacceptable.
    try {
        result = local.get().a
    } catch(e: IllegalStateException) {
        result = 3
    }
    assertEquals(3, result)

    future.result
    worker.requestTermination().result
}

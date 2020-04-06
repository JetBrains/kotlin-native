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

val global1: DisposableSharedReference<A> = DisposableSharedReference(A(3))

@Test fun testGlobal() {
    assertEquals(3, global1.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global1
    }

    val value = future.result
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

val global2: DisposableSharedReference<A> = DisposableSharedReference(A(3))

@Test fun testGlobalDenyAccessOnWorker() {
    assertEquals(3, global2.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        assertFailsWith<IncorrectDereferenceException> {
            global2.value
        }
        Unit
    }

    future.result
    worker.requestTermination().result
}

val global3: DisposableSharedReference<A> = DisposableSharedReference(A(3).freeze())

@Test fun testGlobalAccessOnWorkerFrozenInitially() {
    assertEquals(3, global3.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global3.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global4: DisposableSharedReference<A> = DisposableSharedReference(A(3))

@Test fun testGlobalAccessOnWorkerFrozenBeforePassing() {
    assertEquals(3, global4.value.a)
    global4.value.freeze()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global4.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global5: DisposableSharedReference<A> = DisposableSharedReference(A(3))

@Test fun testGlobalAccessOnWorkerFrozenBeforeAccess() {
    val semaphore: AtomicInt = AtomicInt(0)

    assertEquals(3, global5.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        global5.value.a
    }

    while (semaphore.value < 1) {}
    global5.value.freeze()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global6: DisposableSharedReference<A> = DisposableSharedReference(A(3))

@Test fun testGlobalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    assertEquals(3, global6.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
        semaphore.increment()
        while (semaphore.value < 2) {}
        global6
    }

    while (semaphore.value < 1) {}
    global6.value.a = 4
    semaphore.increment()

    val value = future.result
    assertEquals(4, value.value.a)
    worker.requestTermination().result
}

val global7: DisposableSharedReference<A> = DisposableSharedReference(A(3))

@Test fun testGlobalDispose() {
    assertEquals(3, global7.value.a)

    global7.dispose()
    global7.dispose()
}

val global8: DisposableSharedReference<A> = DisposableSharedReference(A(3))

@Test fun testGlobalAccessAfterDispose() {
    assertEquals(3, global8.value.a)

    global8.dispose()
    assertFailsWith<IllegalStateException> {
        global8.value.a
    }
}

@Test fun testLocal() {
    val local = DisposableSharedReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local
    }

    val value = future.result
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

@Test fun testLocalDenyAccessOnWorker() {
    val local = DisposableSharedReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        assertFailsWith<IncorrectDereferenceException> {
            local.value
        }
        Unit
    }

    future.result
    worker.requestTermination().result
}

@Test fun testLocalAccessOnWorkerFrozenInitially() {
    val local = DisposableSharedReference(A(3).freeze())
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test fun testLocalAccessOnWorkerFrozenBeforePassing() {
    val local = DisposableSharedReference(A(3))
    assertEquals(3, local.value.a)
    local.value.freeze()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test fun testLocalAccessOnWorkerFrozenBeforeAccess() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = DisposableSharedReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        local.value.a
    }

    while (semaphore.value < 1) {}
    local.value.freeze()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test fun testLocalDenyAccessOnMainThread() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        DisposableSharedReference(A(3))
    }

    val value = future.result
    assertFailsWith<IncorrectDereferenceException> {
        value.value
    }

    worker.requestTermination().result
}

@Test fun testLocalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = DisposableSharedReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}
        local
    }

    while (semaphore.value < 1) {}
    local.value.a = 4
    semaphore.increment()

    val value = future.result
    assertEquals(4, value.value.a)
    worker.requestTermination().result
}

@Test fun testLocalDispose() {
    val local = DisposableSharedReference(A(3))
    assertEquals(3, local.value.a)

    local.dispose()
    local.dispose()
}

@Test fun testLocalAccessAfterDispose() {
    val local = DisposableSharedReference(A(3))
    assertEquals(3, local.value.a)

    local.dispose()
    assertFailsWith<IllegalStateException> {
        local.value.a
    }
}

fun getOwnerAndWeaks(initial: Int): Triple<AtomicReference<DisposableSharedReference<A>?>, WeakReference<DisposableSharedReference<A>>, WeakReference<A>> {
    val sharedRef = DisposableSharedReference(A(initial))
    val sharedRefOwner: AtomicReference<DisposableSharedReference<A>?> = AtomicReference(sharedRef)
    val sharedRefWeak = WeakReference(sharedRef)
    val sharedRefValueWeak = WeakReference(sharedRef.value)

    return Triple(sharedRefOwner, sharedRefWeak, sharedRefValueWeak)
}

fun <T: Any> callDispose(ref: AtomicReference<DisposableSharedReference<T>?>) {
    ref.value!!.dispose()
}

@Test fun testCollect() {
    val (sharedRefOwner, sharedRefWeak, sharedRefValueWeak) = getOwnerAndWeaks(3)

    sharedRefOwner.value = null
    GC.collect()

    // Last reference to DisposableSharedReference is gone, so it and it's referent are destroyed.
    assertNull(sharedRefWeak.value)
    assertNull(sharedRefValueWeak.value)
}

@Test fun testDisposeAndCollect() {
    val (sharedRefOwner, sharedRefWeak, sharedRefValueWeak) = getOwnerAndWeaks(3)

    callDispose(sharedRefOwner)
    GC.collect()

    // sharedRefOwner still contains a reference to DisposableSharedReference. But it's referent is
    // destroyed because of explicit dispose call.
    assertNotNull(sharedRefWeak.value)
    assertNull(sharedRefValueWeak.value)
}

fun collectInWorker(worker: Worker, semaphore: AtomicInt): Pair<WeakReference<A>, Future<Unit>> {
    val (sharedRefOwner, _, sharedRefValueWeak) = getOwnerAndWeaks(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(sharedRefOwner, semaphore) }) { (sharedRefOwner, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        sharedRefOwner.value = null
        GC.collect()
    }

    while (semaphore.value < 1) {}
    // At this point worker is spinning on semaphore. sharedRefOwner still contains reference to
    // DisposableSharedReference, so referent is kept alive.
    GC.collect()
    assertNotNull(sharedRefValueWeak.value)

    return Pair(sharedRefValueWeak, future)
}

@Test fun testCollectInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (sharedRefValueWeak, future) = collectInWorker(worker, semaphore)
    semaphore.increment()
    future.result

    // At this point DisposableSharedReference no longer has a reference, so it's referent is destroyed.
    // DisposableSharedReference, so referent is kept alive.
    GC.collect()
    assertNull(sharedRefValueWeak.value)

    worker.requestTermination().result
}

fun doNotCollectInWorker(worker: Worker, semaphore: AtomicInt): Future<DisposableSharedReference<A>> {
    val sharedRef = DisposableSharedReference(A(3))

    return worker.execute(TransferMode.SAFE, { Pair(sharedRef, semaphore) }) { (sharedRef, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        GC.collect()
        sharedRef
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
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

fun disposeInWorker(worker: Worker, semaphore: AtomicInt): Triple<WeakReference<DisposableSharedReference<A>>, WeakReference<A>, Future<AtomicReference<DisposableSharedReference<A>?>>> {
    val (sharedRefOwner, sharedRefWeak, sharedRefValueWeak) = getOwnerAndWeaks(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(sharedRefOwner, semaphore) }) { (sharedRefOwner, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        callDispose(sharedRefOwner)
        GC.collect()
        sharedRefOwner
    }

    while (semaphore.value < 1) {}
    // At this point worker is spinning on semaphore. sharedRefOwner still contains reference to
    // DisposableSharedReference, so referent is kept alive.
    GC.collect()
    assertNotNull(sharedRefValueWeak.value)

    return Triple(sharedRefWeak, sharedRefValueWeak, future)
}

@Test fun testDisposeInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (sharedRefWeak, sharedRefValueWeak, future) = disposeInWorker(worker, semaphore)
    semaphore.increment()
    val sharedRefOwner = future.result

    // At this point sharedRefOwner still has a reference, but it's explicitly disposed,
    // so referent is destroyed.
    GC.collect()
    assertNotNull(sharedRefWeak.value)
    assertNull(sharedRefValueWeak.value)

    worker.requestTermination().result
}

@Test fun testDisposeOnMainThreadAndAccessInWorker() {
    val sharedRef = DisposableSharedReference(A(3))
    assertEquals(3, sharedRef.value.a)

    sharedRef.dispose()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { sharedRef }) { sharedRef ->
        var result = 0
        assertFailsWith<IllegalStateException> {
            result = sharedRef.value.a
        }
        result
    }

    val value = future.result
    assertEquals(0, value)
    worker.requestTermination().result
}

@Test fun testDisposeInWorkerAndAccessOnMainThread() {
    val sharedRef = DisposableSharedReference(A(3))
    assertEquals(3, sharedRef.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { sharedRef }) { sharedRef ->
        sharedRef.dispose()
    }

    future.result
    assertFailsWith<IllegalStateException> {
        sharedRef.value.a
    }
    worker.requestTermination().result
}

class B1 {
    lateinit var b2: DisposableSharedReference<B2>
}

data class B2(val b1: DisposableSharedReference<B1>)

fun createCyclicGarbage(): Triple<AtomicReference<DisposableSharedReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val sharedRef1 = DisposableSharedReference(B1())
    val sharedRef1Owner: AtomicReference<DisposableSharedReference<B1>?> = AtomicReference(sharedRef1)
    val sharedRef1Weak = WeakReference(sharedRef1.value)

    val sharedRef2 = DisposableSharedReference(B2(sharedRef1))
    val sharedRef2Weak = WeakReference(sharedRef2.value)

    sharedRef1.value.b2 = sharedRef2

    return Triple(sharedRef1Owner, sharedRef1Weak, sharedRef2Weak)
}

@Test fun doesNotCollectCyclicGarbage() {
    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCyclicGarbage()

    sharedRef1Owner.value = null
    GC.collect()

    // If these asserts fail, that means DisposableSharedReference managed to clean up cyclic garbage all by itself.
    assertNotNull(sharedRef1Weak.value)
    assertNotNull(sharedRef2Weak.value)
}

@Test fun collectCyclicGarbageWithExplicitDispose() {
    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCyclicGarbage()

    callDispose(sharedRef1Owner)
    GC.collect()

    assertNull(sharedRef1Weak.value)
    assertNull(sharedRef2Weak.value)
}

fun createCrossThreadCyclicGarbage(
    worker: Worker
): Triple<AtomicReference<DisposableSharedReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val sharedRef1 = DisposableSharedReference(B1())
    val sharedRef1Owner: AtomicReference<DisposableSharedReference<B1>?> = AtomicReference(sharedRef1)
    val sharedRef1Weak = WeakReference(sharedRef1.value)

    val future = worker.execute(TransferMode.SAFE, { sharedRef1 }) { sharedRef1 ->
        val sharedRef2 = DisposableSharedReference(B2(sharedRef1))
        Pair(sharedRef2, WeakReference(sharedRef2.value))
    }
    val (sharedRef2, sharedRef2Weak) = future.result

    sharedRef1.value.b2 = sharedRef2

    return Triple(sharedRef1Owner, sharedRef1Weak, sharedRef2Weak)
}

@Test fun doesNotCollectCrossThreadCyclicGarbage() {
    val worker = Worker.start()

    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCrossThreadCyclicGarbage(worker)

    sharedRef1Owner.value = null
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    // If these asserts fail, that means DisposableSharedReference managed to clean up cyclic garbage all by itself.
    assertNotNull(sharedRef1Weak.value)
    assertNotNull(sharedRef2Weak.value)

    worker.requestTermination().result
}

@Test fun collectCrossThreadCyclicGarbageWithExplicitDispose() {
    val worker = Worker.start()

    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCrossThreadCyclicGarbage(worker)

    callDispose(sharedRef1Owner)
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    assertNull(sharedRef1Weak.value)
    assertNull(sharedRef2Weak.value)

    worker.requestTermination().result
}

@Test fun concurrentAccess() {
    val workerCount = 10
    val workerUnlocker = AtomicInt(0)

    val sharedRef = DisposableSharedReference(A(3))
    assertEquals(3, sharedRef.value.a)

    val workers = Array(workerCount) {
        Worker.start()
    }
    val futures = Array(workers.size) {
        workers[it].execute(TransferMode.SAFE, { Pair(sharedRef, workerUnlocker) }) { (sharedRef, workerUnlocker) ->
            while (workerUnlocker.value < 1) {}

            assertFailsWith<IncorrectDereferenceException> {
                sharedRef.value
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

    val sharedRef = DisposableSharedReference(A(3))
    assertEquals(3, sharedRef.value.a)

    val workers = Array(workerCount) {
        Worker.start()
    }
    val futures = Array(workers.size) {
        workers[it].execute(TransferMode.SAFE, { Pair(sharedRef, workerUnlocker) }) { (sharedRef, workerUnlocker) ->
            while (workerUnlocker.value < 1) {}

            sharedRef.dispose()
        }
    }
    workerUnlocker.increment()

    for (future in futures) {
        future.result
    }

    assertFailsWith<IllegalStateException> {
        sharedRef.value.a
    }

    for (worker in workers) {
        worker.requestTermination().result
    }
}

@Test fun concurrentDisposeAndAccess() {
    val workerUnlocker = AtomicInt(0)

    val sharedRef = DisposableSharedReference(A(3))
    assertEquals(3, sharedRef.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(sharedRef, workerUnlocker) }) { (sharedRef, workerUnlocker) ->
        while (workerUnlocker.value < 1) {}

        sharedRef.dispose()
    }
    workerUnlocker.increment()

    var result = 0
    // This is a race, but it should either get value successfully or get IllegalStateException.
    // Any other kind of failure is unacceptable.
    try {
        result = sharedRef.value.a
    } catch(e: IllegalStateException) {
        result = 3
    }
    assertEquals(3, result)

    future.result
    worker.requestTermination().result
}

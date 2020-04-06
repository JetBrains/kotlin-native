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
        assertFailsWith<IncorrectDereferenceException> {
            global2.get()
        }
        Unit
    }

    future.result
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
        assertFailsWith<IncorrectDereferenceException> {
            local.get()
        }
        Unit
    }

    future.result
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

fun getOwnerAndWeaks(initial: Int): Triple<AtomicReference<DisposableSharedRef<A>?>, WeakReference<DisposableSharedRef<A>>, WeakReference<A>> {
    val sharedRef = DisposableSharedRef(A(initial))
    val sharedRefOwner: AtomicReference<DisposableSharedRef<A>?> = AtomicReference(sharedRef)
    val sharedRefWeak = WeakReference(sharedRef)
    val sharedRefValueWeak = WeakReference(sharedRef.get())

    return Triple(sharedRefOwner, sharedRefWeak, sharedRefValueWeak)
}

fun <T: Any> callDispose(ref: AtomicReference<DisposableSharedRef<T>?>) {
    ref.value!!.dispose()
}

@Test fun testCollect() {
    val (sharedRefOwner, sharedRefWeak, sharedRefValueWeak) = getOwnerAndWeaks(3)

    sharedRefOwner.value = null
    GC.collect()

    // Last reference to DisposableSharedRef is gone, so it and it's referent are destroyed.
    assertNull(sharedRefWeak.get())
    assertNull(sharedRefValueWeak.get())
}

@Test fun testDisposeAndCollect() {
    val (sharedRefOwner, sharedRefWeak, sharedRefValueWeak) = getOwnerAndWeaks(3)

    callDispose(sharedRefOwner)
    GC.collect()

    // sharedRefOwner still contains a reference to DisposableSharedRef. But it's referent is
    // destroyed because of explicit dispose call.
    assertNotNull(sharedRefWeak.get())
    assertNull(sharedRefValueWeak.get())
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
    // DisposableSharedRef, so referent is kept alive.
    GC.collect()
    assertNotNull(sharedRefValueWeak.get())

    return Pair(sharedRefValueWeak, future)
}

@Test fun testCollectInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (sharedRefValueWeak, future) = collectInWorker(worker, semaphore)
    semaphore.increment()
    future.result

    // At this point DisposableSharedRef no longer has a reference, so it's referent is destroyed.
    // DisposableSharedRef, so referent is kept alive.
    GC.collect()
    assertNull(sharedRefValueWeak.get())

    worker.requestTermination().result
}

fun doNotCollectInWorker(worker: Worker, semaphore: AtomicInt): Future<DisposableSharedRef<A>> {
    val sharedRef = DisposableSharedRef(A(3))

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
    assertEquals(3, value.get().a)
    worker.requestTermination().result
}

fun disposeInWorker(worker: Worker, semaphore: AtomicInt): Triple<WeakReference<DisposableSharedRef<A>>, WeakReference<A>, Future<AtomicReference<DisposableSharedRef<A>?>>> {
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
    // DisposableSharedRef, so referent is kept alive.
    GC.collect()
    assertNotNull(sharedRefValueWeak.get())

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
    assertNotNull(sharedRefWeak.get())
    assertNull(sharedRefValueWeak.get())

    worker.requestTermination().result
}

@Test fun testDisposeOnMainThreadAndAccessInWorker() {
    val sharedRef = DisposableSharedRef(A(3))
    assertEquals(3, sharedRef.get().a)

    sharedRef.dispose()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { sharedRef }) { sharedRef ->
        var result = 0
        assertFailsWith<IllegalStateException> {
            result = sharedRef.get().a
        }
        result
    }

    val value = future.result
    assertEquals(0, value)
    worker.requestTermination().result
}

@Test fun testDisposeInWorkerAndAccessOnMainThread() {
    val sharedRef = DisposableSharedRef(A(3))
    assertEquals(3, sharedRef.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { sharedRef }) { sharedRef ->
        sharedRef.dispose()
    }

    future.result
    assertFailsWith<IllegalStateException> {
        sharedRef.get().a
    }
    worker.requestTermination().result
}

class B1 {
    lateinit var b2: DisposableSharedRef<B2>
}

data class B2(val b1: DisposableSharedRef<B1>)

fun createCyclicGarbage(): Triple<AtomicReference<DisposableSharedRef<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val sharedRef1 = DisposableSharedRef(B1())
    val sharedRef1Owner: AtomicReference<DisposableSharedRef<B1>?> = AtomicReference(sharedRef1)
    val sharedRef1Weak = WeakReference(sharedRef1.get())

    val sharedRef2 = DisposableSharedRef(B2(sharedRef1))
    val sharedRef2Weak = WeakReference(sharedRef2.get())

    sharedRef1.get().b2 = sharedRef2

    return Triple(sharedRef1Owner, sharedRef1Weak, sharedRef2Weak)
}

@Test fun doesNotCollectCyclicGarbage() {
    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCyclicGarbage()

    sharedRef1Owner.value = null
    GC.collect()

    // If these asserts fail, that means DisposableSharedRef managed to clean up cyclic garbage all by itself.
    assertNotNull(sharedRef1Weak.get())
    assertNotNull(sharedRef2Weak.get())
}

@Test fun collectCyclicGarbageWithExplicitDispose() {
    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCyclicGarbage()

    callDispose(sharedRef1Owner)
    GC.collect()

    assertNull(sharedRef1Weak.get())
    assertNull(sharedRef2Weak.get())
}

fun createCrossThreadCyclicGarbage(
    worker: Worker
): Triple<AtomicReference<DisposableSharedRef<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val sharedRef1 = DisposableSharedRef(B1())
    val sharedRef1Owner: AtomicReference<DisposableSharedRef<B1>?> = AtomicReference(sharedRef1)
    val sharedRef1Weak = WeakReference(sharedRef1.get())

    val future = worker.execute(TransferMode.SAFE, { sharedRef1 }) { sharedRef1 ->
        val sharedRef2 = DisposableSharedRef(B2(sharedRef1))
        Pair(sharedRef2, WeakReference(sharedRef2.get()))
    }
    val (sharedRef2, sharedRef2Weak) = future.result

    sharedRef1.get().b2 = sharedRef2

    return Triple(sharedRef1Owner, sharedRef1Weak, sharedRef2Weak)
}

@Test fun doesNotCollectCrossThreadCyclicGarbage() {
    val worker = Worker.start()

    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCrossThreadCyclicGarbage(worker)

    sharedRef1Owner.value = null
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    // If these asserts fail, that means DisposableSharedRef managed to clean up cyclic garbage all by itself.
    assertNotNull(sharedRef1Weak.get())
    assertNotNull(sharedRef2Weak.get())

    worker.requestTermination().result
}

@Test fun collectCrossThreadCyclicGarbageWithExplicitDispose() {
    val worker = Worker.start()

    val (sharedRef1Owner, sharedRef1Weak, sharedRef2Weak) = createCrossThreadCyclicGarbage(worker)

    callDispose(sharedRef1Owner)
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    assertNull(sharedRef1Weak.get())
    assertNull(sharedRef2Weak.get())

    worker.requestTermination().result
}

@Test fun concurrentAccess() {
    val workerCount = 10
    val workerUnlocker = AtomicInt(0)

    val sharedRef = DisposableSharedRef(A(3))
    assertEquals(3, sharedRef.get().a)

    val workers = Array(workerCount) {
        Worker.start()
    }
    val futures = Array(workers.size) {
        workers[it].execute(TransferMode.SAFE, { Pair(sharedRef, workerUnlocker) }) { (sharedRef, workerUnlocker) ->
            while (workerUnlocker.value < 1) {}

            assertFailsWith<IncorrectDereferenceException> {
                sharedRef.get()
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

    val sharedRef = DisposableSharedRef(A(3))
    assertEquals(3, sharedRef.get().a)

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
        sharedRef.get().a
    }

    for (worker in workers) {
        worker.requestTermination().result
    }
}

@Test fun concurrentDisposeAndAccess() {
    val workerUnlocker = AtomicInt(0)

    val sharedRef = DisposableSharedRef(A(3))
    assertEquals(3, sharedRef.get().a)

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
        result = sharedRef.get().a
    } catch(e: IllegalStateException) {
        result = 3
    }
    assertEquals(3, result)

    future.result
    worker.requestTermination().result
}

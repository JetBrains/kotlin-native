/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.concurrent.shared0

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.native.ref.WeakReference

class A(var a: Int)

val global1: SharedRef<A> = SharedRef.create(A(3))

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

val global2: SharedRef<A> = SharedRef.create(A(3))

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

val global3: SharedRef<A> = SharedRef.create(A(3))

@Test fun testGlobalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    assertEquals(3, global3.get().a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
        semaphore.increment()
        while (semaphore.value < 2) {}
        global3
    }

    while (semaphore.value < 1) {}
    global3.get().a = 4
    semaphore.increment()

    val value = future.result
    assertEquals(4, value.get().a)
    worker.requestTermination().result
}

val global4: SharedRef<A> = SharedRef.create(A(3))

@Test fun testGlobalDispose() {
    assertEquals(3, global4.get().a)

    global4.dispose()
}

@Test fun testLocal() {
    val local = SharedRef.create(A(3))
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
    val local = SharedRef.create(A(3))
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

@Test fun testLocalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = SharedRef.create(A(3))
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
    val local = SharedRef.create(A(3))
    assertEquals(3, local.get().a)

    local.dispose()
}

fun getWeaksAndAtomicReference(initial: Int): Triple<AtomicReference<SharedRef<A>?>, WeakReference<SharedRef<A>>, WeakReference<A>> {
    val local = SharedRef.create(A(initial))
    val localRef: AtomicReference<SharedRef<A>?> = AtomicReference(local)
    val localWeak = WeakReference(local)
    val localValueWeak = WeakReference(local.get())

    assertNotNull(localWeak.get())
    assertNotNull(localValueWeak.get())

    return Triple(localRef, localWeak, localValueWeak)
}

@Test fun testCollect() {
    val (localRef, localWeak, localValueWeak) = getWeaksAndAtomicReference(3)

    localRef.value = null
    GC.collect()

    assertNull(localWeak.get())
    assertNull(localValueWeak.get())
}

@Test fun testDisposeAndCollect() {
    val (localRef, localWeak, localValueWeak) = getWeaksAndAtomicReference(3)

    localRef.value!!.dispose()
    GC.collect()

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
    GC.collect()
    assertNull(localValueWeak.get())
    worker.requestTermination().result
}

fun doNotCollectInWorker(worker: Worker, semaphore: AtomicInt): Future<SharedRef<A>> {
    val local = SharedRef.create(A(3))

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

fun disposeInWorker(worker: Worker, semaphore: AtomicInt): Triple<WeakReference<SharedRef<A>>, WeakReference<A>, Future<Unit>> {
    val (localRef, localWeak, localValueWeak) = getWeaksAndAtomicReference(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(localRef, semaphore) }) { (localRef, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {}

        localRef.value!!.dispose()
        GC.collect()
    }

    while (semaphore.value < 1) {}
    GC.collect()
    assertNotNull(localValueWeak.get())

    return Triple(localWeak, localValueWeak, future)
}

@Test fun testDisposeInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (localWeak, localValueWeak, future) = disposeInWorker(worker, semaphore)
    semaphore.increment()

    future.result
    GC.collect()
    assertNotNull(localWeak.get())
    assertNull(localValueWeak.get())
    worker.requestTermination().result
}

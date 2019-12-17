import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.test.*

fun test1() {
    val a = AtomicReference<Any?>(null)
    val b = AtomicReference<Any?>(null)
    a.value = b
    b.value = a
}

class Holder(var other: Any?)

fun test2() {
    val array = arrayOf(AtomicReference<Any?>(null), AtomicReference<Any?>(null))
    val obj1 = Holder(array).freeze()
    array[0].value = obj1
}

fun test3() {
    val a1 = FreezableAtomicReference<Any?>(null)
    val head = Holder(null)
    var current = head
    repeat(30) {
        val next = Holder(null)
        current.other = next
        current = next
    }
    a1.value = head
    current.other = a1
    current.freeze()
}


fun makeIt(): Holder {
    val atomic = AtomicReference<Holder?>(null)
    val holder = Holder(atomic).freeze()
    atomic.value = holder
    return holder
}


fun test4() {
    val holder = makeIt()
    // To clean rc count coming from rememberNewContainer().
    kotlin.native.internal.GC.collect()
    // Request cyclic collection.
    kotlin.native.internal.GC.collectCyclic()
    // Ensure we processed delayed release.
    repeat(10) {
        // Wait a bit and process queue.
        Worker.current.park(10)
        Worker.current.processQueue()
        kotlin.native.internal.GC.collect()
    }
    val value = @Suppress("UNCHECKED_CAST") (holder.other as? AtomicReference<Holder?>?)
    assertTrue(value != null)
    assertTrue(value.value == holder)
}

fun main() {
    kotlin.native.internal.GC.cyclicCollector = true
    test1()
    test2()
    test3()
    test4()
}
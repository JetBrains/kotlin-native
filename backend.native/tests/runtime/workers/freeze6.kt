/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze6

import kotlin.test.*
import kotlin.native.concurrent.*


data class Hi(val s: String)
data class Nested(val hi: Hi)

@Test
fun ensureNeverFrozenNoFreezeChild(){
    val noFreeze = Hi("qwert")
    noFreeze.ensureNeverFrozen()

    val nested = Nested(noFreeze)
    assertFails { nested.freeze() }

    println("OK")
}

@Test
fun ensureNeverFrozenFailsTarget(){
    val noFreeze = Hi("qwert")
    noFreeze.ensureNeverFrozen()

    assertFalse(noFreeze.isFrozen)
    assertFails { noFreeze.freeze() }
    assertFalse(noFreeze.isFrozen)
    println("OK")
}

fun createRef1(): FreezableAtomicReference<Any?> {
    val ref = FreezableAtomicReference<Any?>(null)
    ref.value = ref
    ref.freeze()
    ref.value = null

    return ref
}

var global = 0

@Test
fun ensureFreezableHandlesCycles1() {
    val ref = createRef1()
    kotlin.native.internal.GC.collect()

    val obj: Any = ref
    global = obj.hashCode()
}

class Node(var ref: Any?)

/**
 *    ref1 -> Node <- ref3
 *             |        /\
 *             V        |
 *             ref2  ---
 */
fun createRef2(): FreezableAtomicReference<Node?> {
    val ref1 = FreezableAtomicReference<Node?>(null)

    val node = Node(null)
    val ref3 = FreezableAtomicReference<Any?>(node)
    val ref2 = FreezableAtomicReference<Any?>(ref3)

    node.ref = ref2
    ref1.value = node

    ref1.freeze()
    ref3.value = null

    return ref1
}

@Test
fun ensureFreezableHandlesCycles2() {
    val ref = createRef2()
    kotlin.native.internal.GC.collect()

    assert(ref.value.toString().length > 0)
    global = ref.value!!.ref!!.hashCode()
}

fun createRef3(): FreezableAtomicReference<Any?> {
    val ref = FreezableAtomicReference<Any?>(null)
    val node = Node(ref)
    ref.value = node
    ref.freeze()
    return ref
}

@Test
fun ensureFreezableHandlesCycles3() {
    val ref = createRef3()
    ref.value = null
    kotlin.native.internal.GC.collect()

    val obj: Any = ref
    assert(obj.toString().length > 0)
    global = obj.hashCode()
}
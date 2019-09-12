/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze6

import kotlin.test.*
import kotlin.native.concurrent.*


data class Hi(val s:String)
data class Nested(val hi:Hi)

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

fun createInvalidRef1(): FreezableAtomicReference<Any?> {
    val ref = FreezableAtomicReference<Any?>(null)
    ref.value = ref
    ref.freeze()
    ref.value = null
    return ref
}

var global = 0

@Test
fun ensureFreezableHandlesCycles1() {
    val ref = createInvalidRef1()
    kotlin.native.internal.GC.collect()

    val obj: Any = ref
    global = obj.hashCode()
}

fun createInvalidRef2(): FreezableAtomicReference<Any?> {
    val ref1 = FreezableAtomicReference<Any?>(null)
    val ref2 = FreezableAtomicReference<Any?>(ref1)
    val ref3 = FreezableAtomicReference<Any?>(ref1)
    ref1.value = ref2

    return ref3
}

@Test
fun ensureFreezableHandlesCycles2() {
    val ref = createInvalidRef2()
    ref.freeze()
    kotlin.native.internal.GC.collect()

    global = ref.hashCode()
}
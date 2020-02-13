/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.lazy1

import kotlin.test.*

import kotlin.native.concurrent.*

class Leak {
    val leak by lazy { this }
}

class SelfReference {
    val x = 17
    val self by lazy { this }
    val recursion: Int by lazy {
        if (x < 17) 42 else recursion
    }
    val freezer: Int by lazy {
        freeze()
        42
    }
}



@Test fun runTest1() {
    assertFailsWith<IllegalStateException> {
        println(SelfReference().recursion)
    }
    assertFailsWith<IllegalStateException> {
        println(SelfReference().freeze().recursion)
    }
}

// Disabled, as requires cyclic collector.
// @Test
fun runTest2() {
    var sum = 0
    for (i in 1 .. 100) {
        val self = SelfReference().freeze()
        assertEquals(self, self.self)
        sum += self.self.hashCode()
    }
}

@Test fun runTest3() {
    assertFailsWith<InvalidMutabilityException> {
        println(SelfReference().freezer)
    }
    println("OK")
}
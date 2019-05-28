/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze1

import kotlin.test.*

import kotlin.native.concurrent.*

@ShareByValue
class Node(var previous: Node?, var data: Int)

fun makeAcyclic(count: Int): Node {
    val first = Node(null, 0)
    var current = first
    for (index in 1 .. count - 1) {
        current = Node(current, index)
    }
    return current
}

fun makeCycle(count: Int): Node {
    val first = Node(null, 0)
    var current = first
    for (index in 1 .. count - 1) {
        current = Node(current, index)
    }
    first.previous = current
    return first
}

@ShareByValue
data class Node2(var leaf1: Node2?, var leaf2: Node2?)

fun makeDiamond(): Node2 {
    val bottom = Node2(null, null)
    val mid1prime = Node2(bottom, null)
    val mid1 = Node2(mid1prime, null)
    val mid2 = Node2(bottom, null)
    return Node2(mid1, mid2)
}

@Test fun runTest0() {
    makeCycle(10).freeze()

    // Must be able to freeze diamond shaped graph.
    val diamond = makeDiamond().freeze()

    val immutable = Node(null, 4).freeze()
    try {
        immutable.data = 42
    } catch (e: InvalidMutabilityException) {
        println("OK, cannot mutate frozen")
    }
}

@Test fun runTest1() {
    val simple = "Hello"
    assert(simple.toFrozen() === simple)

    val cycleFrozen = makeCycle(10).toFrozen()
    assert(cycleFrozen.isFrozen)
    assert(cycleFrozen === cycleFrozen.toFrozen())

    val diamondFrozen = makeDiamond().toFrozen()
    assert(diamondFrozen.isFrozen)
    assert(diamondFrozen === diamondFrozen.toFrozen())

    val mapClone = mapOf("me" to "you", "they" to "them").toFrozen()
    assertEquals(mapClone["me"], "you")
    assertEquals(mapClone["they"], "them")

    val diamond = makeDiamond()
    diamond.leaf1!!.ensureNeverFrozen()
    // Ensure that not freezing bit doesn't propagate on clone.
    assert(diamond.toFrozen() != null)
    diamond.ensureNeverCloned()
    assertFailsWith<IllegalArgumentException> { diamond.toFrozen() }
}

@Test fun runTest2() {
    val list = makeAcyclic(4)
    var current: Node? = list
    repeat(4) {
        current = current!!.previous
    }
    // Structurally equivalent.
    assertEquals(current, null)

    val cyclicList = makeCycle(16)
    current = cyclicList
    repeat(12) {
        current = current!!.previous
    }
    assert(current != cyclicList)
    repeat(4) {
        current = current!!.previous
    }
    assert(current == cyclicList)
}

data class EasyShare(val name: String)

@Test fun runTest3() {
    // Failing clone.
    assertFailsWith<IllegalArgumentException> { Any().toFrozen() }
    assertFailsWith<IllegalArgumentException> { listOf(Any()).toFrozen() }
    assertFailsWith<IllegalArgumentException> { arrayOf(Any()).toFrozen() }
    assertFailsWith<IllegalArgumentException> { setOf(Any()).toFrozen() }
    assertFailsWith<IllegalArgumentException> { mapOf(Any() to Any()).toFrozen() }

    // Successful clone.
    assert("Hello".toFrozen() == "Hello")
    assertEquals(EasyShare("Hi"), EasyShare("Hi").toFrozen())
    assertEquals(2, setOf(1, 1, 2).toFrozen().size)
    // Not yet supported.
    //assertEquals(3, listOf(1, 2, 3).toFrozen()[2])
    assertEquals(5, arrayOf(4, 5, 6).toFrozen()[1])
    assertEquals(3, mapOf(1 to 3).toFrozen()[1])
}
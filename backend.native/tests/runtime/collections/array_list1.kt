fun assertTrue(cond: Boolean) {
    if (!cond)
       println("FAIL")
}

fun assertFalse(cond: Boolean) {
    if (cond)
        println("FAIL")
}

fun assertEquals(value1: Any, value2: Any) {
    if (value1 != value2)
        println("FAIL")
}

fun testBasic() {
    val a = ArrayList<Int>()
    assertTrue(a.isEmpty())
    assertEquals(0, a.size)

    assertTrue(a.add("1"))
    assertTrue(a.add("2"))
    assertTrue(a.add("3"))
    assertFalse(a.isEmpty())
    assertEquals(3, a.size)
    assertEquals("1", a[0])
    assertEquals("2", a[1])
    assertEquals("3", a[2])

    a[0] = "11"
    assertEquals("11", a[0])

    assertEquals("11", a.removeAt(0))
    assertEquals("2", a.size)
    assertEquals("2", a[0])
    assertEquals("3", a[1])

    a.add("1", "22")
    assertEquals(3, a.size)
    assertEquals("2", a[0])
    assertEquals("22", a[1])
    assertEquals("3", a[2])

    a.clear()
    assertTrue(a.isEmpty())
    assertEquals(0, a.size)
}

fun makeList() {
    val a = ArrayList<String>()
    a.add("1")
    a.add("2")
    a.add("3")
    return a
}

fun testIterator() {
    val a = makeList()
    val it = a.iterator()
    assertTrue(it.hasNext())
    assertEquals("1", it.next())
    assertTrue(it.hasNext())
    assertEquals("2", it.next())
    assertTrue(it.hasNext())
    assertEquals("3", it.next())
    assertFalse(it.hasNext())
}

fun testRemove() {
    val a = makeList()
    assertTrue(a.remove("2"))
    assertEquals(2, a.size)
    assertEquals("1", a[0])
    assertEquals("3", a[1])
    assertFalse(a.remove("2"))
    assertEquals(2, a.size)
    assertEquals("1", a[0])
    assertEquals("3", a[1])
}

/* Enable with ranges and listOf().
fun testEquals() {
    val a = makeList(listOf(1, 2, 3))
    assertTrue(a == listOf(1, 2, 3))
    assertFalse(a == listOf(1, 2, 4))
    assertFalse(a == listOf(1, 2))
}

fun testHashCode() {
    val a = ArrayList(listOf(1, 2, 3))
    assertTrue(a.hashCode() == listOf(1, 2, 3).hashCode())
}

fun testToString() {
    val a = ArrayList(listOf(1, 2, 3))
    assertTrue(a.toString() == listOf(1, 2, 3).toString())
}

fun testSubList() {
    val a0 = ArrayList(listOf(0, 1, 2, 3, 4))
    val a = a0.subList(1, 4)
    assertEquals(3, a.size)
    assertEquals(1, a[0])
    assertEquals(2, a[1])
    assertEquals(3, a[2])
    assertTrue(a == listOf(1, 2, 3))
    assertTrue(a.hashCode() == listOf(1, 2, 3).hashCode())
    assertTrue(a.toString() == listOf(1, 2, 3).toString())
}

fun testResize() {
    val a = ArrayList<Int>()
    val n = 10000
    for (i in 1..n)
        assertTrue(a.add(i))
    assertEquals(n, a.size)
    for (i in 1..n)
        assertEquals(i, a[i - 1])
    a.trimToSize()
    assertEquals(n, a.size)
    for (i in 1..n)
        assertEquals(i, a[i - 1])
} */


fun main(args : Array<String>) {
    testBasic()
    testIterator()
    testRemove()
    println("OK")
}
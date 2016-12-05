fun assertTrue(cond: Boolean) {
    if (!cond)
        println("FAIL")
}

fun assertFalse(cond: Boolean) {
    if (cond)
        println("FAIL")
}

fun assertEquals(value1: String, value2: String) {
    if (value1 != value2)
        println("FAIL")
}

fun assertEquals(value1: Any?, value2: Any?) {
    if (value1 != value2)
        println("FAIL")
}

fun assertEquals(value1: Int, value2: Int) {
    if (value1 != value2)
        println("FAIL")
}


fun testBasic() {
    val m = HashMap<String, String>()
    assertTrue(m.isEmpty())
    assertEquals(0, m.size)

    assertFalse(m.containsKey("1"))
    assertFalse(m.containsValue("a"))
    assertEquals(null, m.get("1"))

    assertEquals(null, m.put("1", "a"))
    assertTrue(m.containsKey("1"))
    assertTrue(m.containsValue("a"))
    assertEquals("a", m.get("1"))
    assertFalse(m.isEmpty())
    assertEquals(1, m.size)

    assertFalse(m.containsKey("2"))
    assertFalse(m.containsValue("b"))
    assertEquals(null, m.get("2"))

    assertEquals(null, m.put("2", "b"))
    assertTrue(m.containsKey("1"))
    assertTrue(m.containsValue("a"))
    assertEquals("a", m.get("1"))
    assertTrue(m.containsKey("2"))
    assertTrue(m.containsValue("b"))
    assertEquals("b", m.get("2"))
    assertFalse(m.isEmpty())
    assertEquals(2, m.size)

    assertEquals("b", m.put("2", "bb"))
    assertTrue(m.containsKey("1"))
    assertTrue(m.containsValue("a"))
    assertEquals("a", m.get("1"))
    assertTrue(m.containsKey("2"))
    assertTrue(m.containsValue("a"))
    assertTrue(m.containsValue("bb"))
    assertEquals("bb", m.get("2"))
    assertFalse(m.isEmpty())
    assertEquals(2, m.size)

    assertEquals("a", m.remove("1"))
    assertFalse(m.containsKey("1"))
    assertFalse(m.containsValue("a"))
    assertEquals(null, m.get("1"))
    assertTrue(m.containsKey("2"))
    assertTrue(m.containsValue("bb"))
    assertEquals("bb", m.get("2"))
    assertFalse(m.isEmpty())
    assertEquals(1, m.size)

    assertEquals("bb", m.remove("2"))
    assertFalse(m.containsKey("1"))
    assertFalse(m.containsValue("a"))
    assertEquals(null, m.get("1"))
    assertFalse(m.containsKey("2"))
    assertFalse(m.containsValue("bb"))
    assertEquals(null, m.get("2"))
    assertTrue(m.isEmpty())
    assertEquals(0, m.size)
}

fun main(args : Array<String>) {
    testBasic()
    println("OK")
}
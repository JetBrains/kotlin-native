package runtime.workers.freeze5

import kotlin.test.*
object Keys {
    internal val myMap: Map<String, List<String>> = mapOf(
            "val1" to listOf("a1", "a2", "a3"),
            "val2" to listOf("b1", "b2")
    )

    fun getKey(name: String): String {
        for (entry in myMap) {
            if (entry.key  == name) {
                return entry.key
            }
        }
        return name
    }

    fun getValue(name: String): String {
        for (entry in myMap) {
            if (entry.value.contains(name)) {
                return entry.key
            }
        }
        return name
    }

    fun getEntry(name: String): String {
        for (entry in myMap.entries) {
            if (entry.key == name) {
                return entry.key
            }
        }
        return name
    }
}
@Test fun runTest() {
    assertEquals(Keys.getKey("val2"), "val2")
    assertEquals(Keys.getValue("a1"), "val1")
    assertEquals(Keys.getEntry("a2"), "a2")
    println("OK")
}
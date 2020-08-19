import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.properties.resolvablePropertyString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResolvablePropertiesTests {

    @Test
    fun `trivial symbol resolve`() {
        val props = propertiesOf(
                "key1" to "value1",
                "key2" to "\$key1"
        )
        assertEquals("value1", props.resolvablePropertyString("key2"))
    }

    @Test
    fun `trivial circular dependency`() {
        val props = propertiesOf(
                "key1" to "\$key2",
                "key2" to "\$key1"
        )
        assertFailsWith(IllegalStateException::class) {
            props.resolvablePropertyString("key2")
        }
    }

    @Test
    fun `list expansion`() {
        val props = propertiesOf(
                "k1" to "v1 v2",
                "k2" to "\$k1 v3",
                "k3" to "\$k2"
        )
        assertEquals(listOf("v1", "v2", "v3"), props.resolvablePropertyList("k3"))
    }

    @Test
    fun `double list expansion`() {
        val props = propertiesOf(
                "k1" to "v1 v2",
                "k2" to "\$k1 \$k1"
        )
        assertEquals(listOf("v1", "v2", "v1", "v2"), props.resolvablePropertyList("k2"))
    }

    companion object {
        private fun propertiesOf(vararg pairs: Pair<String, Any>): Properties =
                Properties().also {
                    it.putAll(mapOf(*pairs))
                }
    }
}
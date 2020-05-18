import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test
fun testStructWithNSObject() {
    memScoped {
        val struct = alloc<CStructWithNSString>();
        struct.nsString = "hello";
        assertEquals("hello", struct.nsString)
    }
}
import cstructs.*
import kotlinx.cinterop.*
import kotlin.test.*

fun main() {
    produceComplex().useContents {
        assertEquals(ui, 128u)
        ui = 333u
        assertEquals(ui, 333u)

        assertEquals(t.i, 1)
        t.i += 15
        assertEquals(t.i, 16)

        assertEquals(next, null)
        next = this.ptr
        assertEquals(next, this.ptr)
        // Check null pointer because it has Nothing? type.
        next = null
        assertEquals(next, null)

        assertEquals(vec4f, vectorOf(1.0f, 1.0f, 1.0f, 1.0f))
        vec4f = vectorOf(0.0f, 0.0f, 0.0f, 0.0f)
        assertEquals(vec4f, vectorOf(0.0f, 0.0f, 0.0f, 0.0f))

        assertEquals(e, E.R)
        e = E.G
        assertEquals(e, E.G)

        assertEquals(arr[0], -51)
        assertEquals(arr[1], -19)
        arr[0] = 51
        arr[1] = 19
        assertEquals(arr[0], 51)
        assertEquals(arr[1], 19)
    }
}
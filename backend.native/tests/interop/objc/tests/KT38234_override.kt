import kotlin.test.*
import objcTests.*

class KT38234Impl : P1Protocol, Base() {
    override fun foo(): Int = 566
}

@Test fun testKT38234() {
    assertEquals(566, KT38234Impl().callFoo())
}

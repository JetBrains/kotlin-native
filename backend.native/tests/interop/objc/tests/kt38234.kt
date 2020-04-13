import kotlin.test.*
import objcTests.*

class KT38234Impl : KT38234(), KT38234Protocol {
    override fun foo(): Int = 456
}

@Test fun testKT38234() {
    assertEquals(456, KT38234Impl().callFoo())
}
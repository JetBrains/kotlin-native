import kotlinx.cinterop.*
import kotlin.native.*
import kotlin.test.*
import ctypes.*

fun main() {
    getStructWithConstFields().useContents {
        assertEquals(111, x)
        assertEquals(222, y)
    }

    assertEquals(1u, ForwardDeclaredEnum.ONE.value)

    assertEquals(6, vlaSum(3, cValuesOf(1, 2, 3)))
    assertEquals(10, vlaSum2D(2, cValuesOf(1, 2, 3, 4)))
    assertEquals(21, vlaSum2DBothDimensions(2, 3, cValuesOf(1, 2, 3, 4, 5, 6)))

    // Not supported by clang:
    // assertEquals(10, vlaSum2DForward(cValuesOf(1, 2, 3, 4), 2))

    assertEquals(0u, StrictEnum1.StrictEnum1A.value)
    assertEquals(1u, StrictEnum2.StrictEnum2B.value)
    assertEquals(0u, NonStrictEnum1A)
    assertEquals(1u, NonStrictEnum2B)
    assertEquals(1, EnumCharBase.EnumCharBaseB.value)
    assertEquals(3, sendEnum(EnumCharBase.EnumCharBaseB))
    assertEquals('a'.toByte(), EnumExplicitCharA)
    assertEquals('b'.toByte(), EnumExplicitCharB)
    assertEquals(EnumExplicitCharA, EnumExplicitCharDup)

    // FIXME: KT-36285
    if (Platform.osFamily != OsFamily.LINUX && Platform.cpuArchitecture != CpuArchitecture.ARM32) {
        assertEquals(49, sendV4I(vectorOf(1, 2, 3, 4)))
    }
    assertEquals(49, (sendV4F(vectorOf(1f, 2f, 3f, 4f)) + 0.00001).toInt())

    memScoped {
        val vector = alloc<KVector4i32Var>().also {
            it.value = vectorOf(1, 2, 3, 4)
        }
        assertEquals(vector.value, vectorOf(1, 2, 3, 4))
    }
}


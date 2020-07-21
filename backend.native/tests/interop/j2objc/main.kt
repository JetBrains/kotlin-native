import kotlinx.cinterop.*
import kotlin.test.*

fun main(args: Array<String>) {
  autoreleasepool {
    testMethods()
  }
}

private fun testMethods() {
  val myObject = j2objctest.Foo()
  val myExtensionObject = j2objctest.ExtendsFoo()

  assertEquals(100, myObject.return100())
  assertEquals(43, myObject.returnNum(43))
  assertEquals(47, myObject.add2(16,31))

  assertEquals(6, myExtensionObject.add3(1,2,3))
  assertEquals(-10, myExtensionObject.add3(-12,3,-1))
  assertEquals(100, myExtensionObject.return100())
  assertEquals(-19, myExtensionObject.add2(-9,-10))

  assertEquals(100, j2objctest.Foo.return100Static())
}

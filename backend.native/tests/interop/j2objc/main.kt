import kotlinx.cinterop.*
import kotlin.test.*

fun main(args: Array<String>) {
  autoreleasepool {
    testMethods()
  }
}

private fun testMethods() {
  val myObject = j2objctest.Foo()
  assertEquals(100, myObject.return100())
  assertEquals(43, myObject.returnNum(43))
  assertEquals(47, myObject.add2(16,31))

  assertEquals(100, j2objctest.Foo.return100Static())
}
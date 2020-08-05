import kotlinx.cinterop.*
import kotlin.test.*

fun main(args: Array<String>) {
  autoreleasepool {
    testMethods()
  }
}

private fun testMethods() {
  val myObject = j2objctest.ComTestFoo()
  val myExtensionObject = j2objctest.ComTestExtendsFoo()

  assertEquals(100, myObject.return100())
  assertEquals(43, myObject.returnNum(43))
  assertEquals(47, myObject.add2(16,31))
  assertEquals("Hello world!", myObject.returnString("Hello world!"))

  assertEquals(100, myExtensionObject.return100())
  assertEquals(-10, myExtensionObject.returnNum(-10))
  assertEquals(1, myExtensionObject.add2(-9,-10))
//  assertTrue(myExtensionObject.returnFoo() is j2objctest.ComTestFoo)

  // add2/add3 overridden to x-y/x-(y-z)
  assertEquals(-15, myExtensionObject.add2(16,31))
  assertEquals(2, myExtensionObject.add3(1,2,3))
  assertEquals(-16, myExtensionObject.add3(-12,3,-1))

  assertEquals(47, doAddTo(myObject, 16,31))
  assertEquals(-15, doAddTo(myExtensionObject, 16,31))
  assertEquals(100, j2objctest.ComTestFoo.return100Static())
}

fun doAddTo(obj: j2objctest.ComTestFoo, a: Int, b: Int): Int {
  return obj.add2(a,b)
}

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals


class J2ObjcManglingTest {
  private val parser = J2ObjCParser()

  @Before
  fun setUp() {
    parser.className = "com/Example/Foo"
    parser.methodDescriptors.add(Triple("foo","()V",0))
    parser.methodDescriptors.add(Triple("foo", "(I)V",0))
    parser.methodDescriptors.add(Triple("foo", "(II)V", 0))
    parser.methodDescriptors.add(Triple("foo", "(IFZDCJSB)V", 0))
    parser.parameterNames.add(listOf<String>())
    parser.parameterNames.add(listOf<String>("a"))
    parser.parameterNames.add(listOf<String>("a","b"))
    parser.parameterNames.add(listOf<String>("a","b","c","d","e","f","g","h","i"))
  }

  @Test
  fun `package name is added to class name`() {
    val generated = parser.buildClass()
    assertEquals("ComExampleFoo", generated.name)
  }

  @Test
  fun `j2objc selector generated properly`() {
    val generated = parser.buildClass()
    assertEquals("foo", generated.methods.get(0).selector)
    assertEquals("fooWithInt:", generated.methods.get(1).selector)
    assertEquals("fooWithInt:withInt:", generated.methods.get(2).selector)
    assertEquals("fooWithInt:withFloat:withBoolean:withDouble:withChar:withLong:withShort:withByte:", generated.methods.get(3).selector)
  }

  @Test
  fun `kotlin function name generated properly`() {
    val generated = parser.buildClass()
    assertEquals("foo", generated.methods.get(0).nameOverride)
    assertEquals("foo", generated.methods.get(1).nameOverride)
    assertEquals("foo", generated.methods.get(2).nameOverride)
    assertEquals("foo", generated.methods.get(3).nameOverride)
  }

}

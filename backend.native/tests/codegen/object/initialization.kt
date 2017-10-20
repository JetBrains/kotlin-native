package codegen.`object`.initialization

import kotlin.test.*

open class A(val a:Int, val b:Int)

open class B(val c:Int, d:Int):A(c, d)

open class C(i:Int, j:Int):B(i + j, 42)

class D (i: Int, j:Int) : C(i, j){
   constructor(i: Int, j:Int, k:Int) : this(i, j) {
      foo(i)
   }
   constructor():this(1, 2)
}

fun foo(i:Int) : Unit {}


fun foo(i:Int, j:Int):Int {
   val c = D(i, j)
   return c.c
}

@Test fun runTest() {
   if (foo(2, 3) != 5) throw Error()
}
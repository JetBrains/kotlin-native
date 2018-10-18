/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.innerClass.genericOuter

import kotlin.test.*

class Outer<X>(val x: X) {
    inner class Inner(val y: String) 
}

fun box(): String {
    val n = Outer("null").Inner("a")
    return "OK"
}

@Test fun runTest() {
    println(box())
}

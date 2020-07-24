/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.unchecked_cast4

import kotlin.test.*

@Test
fun runTest() {
    CI1I2().uncheckedCast<CI1I2>()
    CI1I2().uncheckedCast<OtherCI1I2>()

    try {
        Any().uncheckedCast<CI1I2>()
    }
    catch (e: TypeCastException) {
        throw AssertionError("TypeCastException is not valid here")
    }
    catch (e: ClassCastException) {
        assertEquals("kotlin.Any cannot be cast to codegen.basics.unchecked_cast4.C", e.message)
    }

    println("Ok")
}

fun <R : C> Any?.uncheckedCast() where R : I1, R : I2 {
    this as R
}

interface I1
interface I2
open class C

class CI1I2 : C(), I1, I2
class OtherCI1I2 : C(), I1, I2
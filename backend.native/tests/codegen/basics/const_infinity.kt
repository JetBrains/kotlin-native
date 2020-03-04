/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.const_infinity

import kotlin.test.*

const val fpInfConst = 1.0F / 0.0F
val fpInfVal = 1.0F / 0.0F

@Test
fun runTest() {
    if (fpInfConst!=Float.POSITIVE_INFINITY) throw Error()
    if (fpInfVal != Float.POSITIVE_INFINITY) throw Error()
    if (fpInfConst!=fpInfVal) throw Error()
}
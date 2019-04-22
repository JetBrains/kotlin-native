/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.complexNumbers

const val benchmarkSize = 10000

expect class ComplexNumbersBenchmark() {
    fun generateNumbersSequence(): List<Any>
    fun sumComplex()
    fun subComplex()
    fun classInheritance()
    fun categoryMethods()
    fun stringToObjC()
    fun stringFromObjC()
    fun fft()
    fun invertFft()
}
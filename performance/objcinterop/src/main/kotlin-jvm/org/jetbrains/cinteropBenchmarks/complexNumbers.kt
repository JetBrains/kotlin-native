/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.complexNumbers

actual class ComplexNumbersBenchmark actual constructor() {
    actual fun generateNumbersSequence(): List<Any> {
        error("Benchmark generateNumbersSequence is unsupported on JVM!")
    }

    actual fun sumComplex() {
        error("Benchmark sumComplex is unsupported on JVM!")
    }

    actual fun subComplex() {
        error("Benchmark subComplex is unsupported on JVM!")
    }
    actual fun classInheritance() {
        error("Benchmark classInheritance is unsupported on JVM!")
    }
    actual fun categoryMethods() {
        error("Benchmark categoryMethods is unsupported on JVM!")
    }
    actual fun stringToObjC() {
        error("Benchmark stringToObjC is unsupported on JVM!")
    }
    actual fun stringFromObjC() {
        error("Benchmark stringToObjC is unsupported on JVM!")
    }
    actual fun fft() {
        error("Benchmark fft is unsupported on JVM!")
    }
    actual fun invertFft() {
        error("Benchmark invertFft is unsupported on JVM!")
    }
}

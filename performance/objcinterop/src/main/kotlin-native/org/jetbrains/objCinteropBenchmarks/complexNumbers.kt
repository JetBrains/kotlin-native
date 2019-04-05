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
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.sqrt
import kotlin.random.Random

const val benchmarkSize = 10000

actual class ComplexNumbersBenchmark actual constructor() {
    val complexNumbersSequence by lazy {
        generateNumbersSequence(benchmarkSize)
    }

    fun randomNumber() = Random.nextDouble(0, benchmarkSize.toDouble())

    actual fun generateNumbersSequence(size: Int) {
        val result = mutableListOf<Complex>()
        for (i in 1..size) {
            result.add(Complex(randomNumber(), randomNumber()))
        }
        return result
    }

    actual fun sumComplex() {

    }
}

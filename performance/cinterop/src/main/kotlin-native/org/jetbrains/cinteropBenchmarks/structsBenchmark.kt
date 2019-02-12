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

package org.jetbrains.structsBenchmarks
import kotlinx.cinterop.*
import kotlin.math.sqrt

const val benchmarkSize = 100000

actual fun structBenchmark() {
    memScoped {
        val elementsList = mutableListOf<ElementS>()
        // Fill list
        for (i in 1..benchmarkSize) {
            val element = alloc<ElementS>()
            element.floatValue = i + sqrt(i.toDouble()).toFloat()
            element.integer = i.toLong()
            itoa(i, element.string, 10)
            elementsList.add(element)
        }
        val summary = elementsList.map { multiplyElementS(it.readValue(), (0..10).random()) }
                .reduce { acc, it -> sumElementSPtr(acc.ptr, it.ptr)!!.pointed.readValue() }
        val intValue = summary.useContents { integer }
        if (intValue < 20000000000)
            error("Summary value is expected to be greater than 20000000000")
    }
}
actual fun unionBenchmark() {

}

actual fun enumBenchmark() {

}

actual fun structAndUnionBencmark() {

}
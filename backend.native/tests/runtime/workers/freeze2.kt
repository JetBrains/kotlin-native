/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package runtime.workers.freeze2

import kotlin.test.*

import konan.worker.*

data class Data(var int: Int)

/* An awful hack: we mark data as immutable, while it is not. */
@konan.internal.Immutable
data class SharedData(var field: Any)

@Test fun runTest() {
    // Ensure that we can not mutate frozen objects and arrays.
    val a0 = Data(2)
    var ok = false
    a0.int++
    a0.freeze()
    try {
        a0.int++
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen object: $a0")

    ok = false
    val a1 = ByteArray(2)
    a1[1]++
    a1.freeze()
    try {
        a1[1]++
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen ByteArray")

    ok = false
    val a2 = ShortArray(2)
    a2[1]++
    a2.freeze()
    try {
        a2[1]++
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen ShortArray")

    ok = false
    val a3 = IntArray(2)
    a3[1]++
    a3.freeze()
    try {
        a3[1]++
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen IntArray")

    ok = false
    val a4 = LongArray(2)
    a4[1]++
    a4.freeze()
    try {
        a4[1]++
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen LongArray")

    ok = false
    val a5 = BooleanArray(2)
    a5[1] = true
    a5.freeze()
    try {
        a5[1] = false
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen BooleanArray")

    ok = false
    val a6 = CharArray(2)
    a6[1] = 'a'
    a6.freeze()
    try {
        a6[1] = 'b'
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen CharArray")

    ok = false
    val a7 = FloatArray(2)
    a7[1] = 1.0f
    a7.freeze()
    try {
        a7[1] = 2.0f
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen FloatArray")

    ok = false
    val a8 = DoubleArray(2)
    a8[1] = 1.0
    a8.freeze()
    try {
        a8[1] = 2.0
    } catch (e: InvalidMutabilityException) {
        ok = true
    }
    if (!ok) throw Error("can mutate frozen DoubleArray")

    // Ensure that String and integral boxes are frozen by default.
    val worker = startWorker()
    val data = SharedData("Hello")
    worker.schedule(TransferMode.CHECKED, { data } ) {
        input -> println("Worker 1: $input")
    }.result()

    data.field = 42
    worker.schedule(TransferMode.CHECKED, { data } ) {
        input -> println("Worker2: $input")
    }.result()

    data.field = 239L
    worker.schedule(TransferMode.CHECKED, { data } ) {
        input -> println("Worker3: $input")
    }.result()

    data.field = 'a'
    worker.schedule(TransferMode.CHECKED, { data } ) {
        input -> println("Worker4: $input")
    }.result()

    worker.requestTermination().result()

    println("OK")
}
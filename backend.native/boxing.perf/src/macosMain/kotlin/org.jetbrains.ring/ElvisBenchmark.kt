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

package org.jetbrains.ring

open class ElvisBenchmark {

    class Value(var value: Int)
    class ValueValue(var value: Value)
    class ValueValueValue(var value: ValueValue)

    class ValueNValue(var value: Value?)

    var array : Array<Value?> = arrayOf()
    var array2: Array<ValueValue?> = arrayOf()
    var array3: Array<ValueValueValue?> = arrayOf()
    var array4: Array<ValueNValue> = arrayOf()

    init {
        array = Array(BENCHMARK_SIZE) {
            if (Random.nextInt(BENCHMARK_SIZE) < BENCHMARK_SIZE / 10) null else Value(Random.nextInt(1000000))
        }
        array2 = Array(BENCHMARK_SIZE) {
            if (Random.nextInt(BENCHMARK_SIZE) < BENCHMARK_SIZE / 10) null else ValueValue(Value(Random.nextInt(1000000)))
        }
        array3 = Array(BENCHMARK_SIZE) {
            if (Random.nextInt(BENCHMARK_SIZE) < BENCHMARK_SIZE / 10) null else ValueValueValue(ValueValue(Value(Random.nextInt(1000000))))
        }

        array4 = Array(BENCHMARK_SIZE) { ValueNValue(array[it]) }
    }

    //Benchmark
    @CountBoxings
    fun testElvis1() {
        for (obj in array) {
            Blackhole.consume(obj?.value ?: 0)
        }
    }

    //Benchmark
    @CountBoxings
    fun testElvis2() {
        for (obj in array2) {
            Blackhole.consume(obj?.value?.value ?: 0)
        }
    }

    //Benchmark
    @CountBoxings
    fun testElvis3() {
        for (obj in array3) {
            Blackhole.consume(obj?.value?.value?.value ?: 0)
        }
    }

    //Benchmark
    @CountBoxings
    fun testElvis4() {
        for (obj in array4) {
            Blackhole.consume(obj?.value?.value ?: 0)
        }
    }
}

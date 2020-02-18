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

open class ClassStreamBenchmark {
    private var _data: Iterable<Value>? = null
    val data: Iterable<Value>
        get() = _data!!

    init {
        _data = classValues(BENCHMARK_SIZE)
    }

    //Benchmark
    @CountBoxings
    fun copy(): List<Value> {
        return data.asSequence().toList()
    }

    //Benchmark
    @CountBoxings
    fun copyManual(): List<Value> {
        val list = ArrayList<Value>()
        for (item in data.asSequence()) {
            list.add(item)
        }
        return list
    }

    //Benchmark
    @CountBoxings
    fun filterAndCount(): Int {
        return data.asSequence().filter { filterLoad(it) }.count()
    }

    //Benchmark
    @CountBoxings
    fun filterAndMap() {
        for (item in data.asSequence().filter { filterLoad(it) }.map { mapLoad(it) })
            Blackhole.consume(item)
    }

    //Benchmark
    @CountBoxings
    fun filterAndMapManual() {
        for (it in data.asSequence()) {
            if (filterLoad(it)) {
                val item = mapLoad(it)
                Blackhole.consume(item)
            }
        }
    }

    //Benchmark
    @CountBoxings
    fun filter() {
        for (item in data.asSequence().filter { filterLoad(it) })
            Blackhole.consume(item)
    }

    //Benchmark
    @CountBoxings
    fun filterManual(){
        for (it in data.asSequence()) {
            if (filterLoad(it))
                Blackhole.consume(it)
        }
    }

    //Benchmark
    @CountBoxings
    fun countFilteredManual(): Int {
        var count = 0
        for (it in data.asSequence()) {
            if (filterLoad(it))
                count++
        }
        return count
    }

    //Benchmark
    @CountBoxings
    fun countFiltered(): Int {
        return data.asSequence().count { filterLoad(it) }
    }

    //Benchmark
//    fun countFilteredLocal(): Int {
//        return data.asSequence().cnt { filterLoad(it) }
//    }

    //Benchmark
    @CountBoxings
    fun reduce(): Int {
        return data.asSequence().fold(0) {acc, it -> if (filterLoad(it)) acc + 1 else acc }
    }
}
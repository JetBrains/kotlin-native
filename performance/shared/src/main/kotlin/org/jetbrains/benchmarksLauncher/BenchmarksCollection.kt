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

package org.jetbrains.benchmarksLauncher

interface BenchmarkEntry

class InstanceBenchmarkEntry(val ctor: ()->Any, val lambda: (Any) -> Any?): BenchmarkEntry {
    companion object {
        inline fun <reified T: Any> create(noinline ctor: ()->T, crossinline lambda: T.() -> Any?) = InstanceBenchmarkEntry(ctor) { (it as T).lambda() }
    }
}

class FunctionBenchmarkEntry(val lambda: () -> Any?) : BenchmarkEntry

class BenchmarksCollection(private val benchmarks: MutableMap<String, BenchmarkEntry> = mutableMapOf()) :
        MutableMap<String, BenchmarkEntry> by benchmarks

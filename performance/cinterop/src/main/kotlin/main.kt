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


import org.jetbrains.structsProducedByMacrosBenchmarks.*
import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.structsBenchmarks.*
import org.jetbrains.typesBenchmarks.*
import org.jetbrains.kliopt.*

class CinteropLauncher : Launcher() {
    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
                    "macros" to FunctionBenchmarkEntry(::macrosBenchmark),
                    "struct" to FunctionBenchmarkEntry(::structBenchmark),
                    "union" to FunctionBenchmarkEntry(::unionBenchmark),
                    "enum" to FunctionBenchmarkEntry(::enumBenchmark),
                    "stringToC" to InstanceBenchmarkEntry.create(::StringBenchmark, { stringToCBenchmark() }),
                    "stringToKotlin" to InstanceBenchmarkEntry.create(::StringBenchmark, { stringToKotlinBenchmark() }),
                    "intMatrix" to InstanceBenchmarkEntry.create(::IntMatrixBenchmark, { intMatrixBenchmark() }),
                    "int" to InstanceBenchmarkEntry.create(::IntBenchmark, { intBenchmark() }),
                    "boxedInt" to InstanceBenchmarkEntry.create(::BoxedIntBenchmark, { boxedIntBenchmark() })
            )
    )
}

fun main(args: Array<String>) {
    val launcher = CinteropLauncher()
    BenchmarksRunner.runBenchmarks(args, { parser: ArgParser ->
        launcher.launch(parser.get<Int>("warmup")!!, parser.get<Int>("repeat")!!, parser.get<String>("prefix")!!,
                parser.getAll<String>("filter"), parser.getAll<String>("filterRegex"))
    }, benchmarksListAction = launcher::benchmarksListAction)
}
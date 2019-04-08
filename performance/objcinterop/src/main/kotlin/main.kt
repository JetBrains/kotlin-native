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


import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.complexNumbers.*
import org.jetbrains.kliopt.*

class ObjCInteropLauncher(numWarmIterations: Int, numberOfAttempts: Int, prefix: String): Launcher(numWarmIterations, numberOfAttempts, prefix) {
    val complexNumbersBecnhmark = ComplexNumbersBenchmark()
    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
                    "generateNumbersSequence" to complexNumbersBecnhmark::generateNumbersSequence,
                    "sumComplex" to complexNumbersBecnhmark::sumComplex,
                    "subComplex" to complexNumbersBecnhmark::subComplex,
                    "classInheritance" to complexNumbersBecnhmark::classInheritance,
                    "categoryMethods" to complexNumbersBecnhmark::categoryMethods,
                    "stringToObjC" to complexNumbersBecnhmark::stringToObjC,
                    "stringFromObjC" to complexNumbersBecnhmark::stringFromObjC,
                    "fft" to complexNumbersBecnhmark::fft,
                    "invertFft" to complexNumbersBecnhmark::invertFft
            )
    )
}

fun main(args: Array<String>) {
    BenchmarksRunner.runBenchmarks(args, { parser: ArgParser ->
        ObjCInteropLauncher(parser.get<Int>("warmup")!!, parser.get<Int>("repeat")!!, parser.get<String>("prefix")!!).launch(parser.getAll<String>("filter"))
    })
}
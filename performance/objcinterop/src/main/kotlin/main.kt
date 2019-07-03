/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.complexNumbers.*
import org.jetbrains.kliopt.*

class ObjCInteropLauncher: Launcher() {
    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
                    "generateNumbersSequence" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { generateNumbersSequence() }),
                    "sumComplex" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { sumComplex() }),
                    "subComplex" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { subComplex() }),
                    "classInheritance" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { classInheritance() }),
                    "categoryMethods" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { categoryMethods() }),
                    "stringToObjC" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { stringToObjC() }),
                    "stringFromObjC" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { stringFromObjC() }),
                    "fft" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { fft() }),
                    "invertFft" to InstanceBenchmarkEntry.create(::ComplexNumbersBenchmark, { invertFft() })
            )
    )
}

fun main(args: Array<String>) {
    val launcher = ObjCInteropLauncher()
    BenchmarksRunner.runBenchmarks(args, { parser: ArgParser ->
        launcher.launch(parser.get<Int>("warmup")!!, parser.get<Int>("repeat")!!, parser.get<String>("prefix")!!,
                parser.getAll<String>("filter"), parser.getAll<String>("filterRegex"))
    }, benchmarksListAction = launcher::benchmarksListAction)
}
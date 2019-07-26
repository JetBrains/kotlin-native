/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.kliopt.*

fun bellardPi() {
    for (n in 1 .. 1000 step 9)
            pi_nth_digit(n)
}

class NumericalLauncher : Launcher() {
    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
                    "bellardPi" to BenchmarkEntry(::bellardPi)
            )
    )
}

fun main(args: Array<String>) {
    val launcher = NumericalLauncher()
    BenchmarksRunner.runBenchmarks(args, { arguments: BenchmarkArguments ->
        if (arguments is BaseBenchmarkArguments) {
            launcher.launch(arguments.warmup, arguments.repeat, arguments.prefix,
                    arguments.filter, arguments.filterRegex, arguments.verbose)
        } else emptyList()
    }, benchmarksListAction = launcher::benchmarksListAction)
}
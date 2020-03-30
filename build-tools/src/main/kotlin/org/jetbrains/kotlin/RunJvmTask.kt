/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.benchmark.LogLevel
import org.jetbrains.kotlin.benchmark.Logger
import org.jetbrains.report.json.*
import java.io.ByteArrayOutputStream
import java.io.File

open class RunJvmTask: JavaExec() {
    @Input
    @Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    var filter: String = ""
    @Input
    @Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    var filterRegex: String = ""
    @Input
    @Option(option = "verbose", description = "Verbose mode of running benchmarks")
    var verbose: Boolean = false
    @Input
    var warmupCount: Int = 0
    @Input
    var repeatCount: Int = 0
    @Input
    var repeatingType = BenchmarkRepeatingType.INTERNAL
    @Input
    var outputFileName: String? = null

    private var predefinedArgs: List<String> = emptyList()

    private fun executeTask(warmupCount: Int, repeatCount: Int,
                            filterArgs: List<String>, filterRegexArgs: List<String>): String {
        // Firstly clean arguments.
        setArgs(emptyList())
        args(predefinedArgs)
        args(filterArgs)
        args(filterRegexArgs)
        args("-w", warmupCount)
        args("-r", repeatCount)
        if (repeatingType == BenchmarkRepeatingType.INTERNAL) {
            if (verbose) {
                args("-v")
            }
            args("-o", outputFileName)
        }
        standardOutput = ByteArrayOutputStream()
        super.exec()
        return if (repeatingType == BenchmarkRepeatingType.EXTERNAL) {
            standardOutput.toString().removePrefix("[").removeSuffix("]")
        } else ""
    }

    private fun getBenchmarksList(filterArgs: List<String>, filterRegexArgs: List<String>): List<String> {
        // Firstly clean arguments.
        setArgs(emptyList())
        args("list")
        standardOutput = ByteArrayOutputStream()
        super.exec()
        val benchmarks = standardOutput.toString().lines()
        val regexes = filterRegexArgs.map { it.toRegex() }
        return if (filterArgs.isNotEmpty() || regexes.isNotEmpty()) {
            benchmarks.filter { benchmark -> benchmark in filterArgs || regexes.any { it.matches(benchmark) } }
        } else benchmarks.filter { !it.isEmpty() }
    }

    private fun execSeparateBenchmarkRepeatedly(benchmark: String): List<String> {
        val logger = if (verbose) Logger(LogLevel.DEBUG) else Logger()
        logger.log("Warm up iterations for benchmark $benchmark\n")
        for (i in 0..warmupCount) {
            executeTask(0, 1, listOf("-f", benchmark), emptyList())
        }
        val result = mutableListOf<String>()
        logger.log("Running benchmark $benchmark ")
        for (i in 0.until(repeatCount)) {
            logger.log(".", usePrefix = false)
            val benchmarkReport = JsonTreeParser.parse(
                    executeTask(0, 1, listOf("-f", benchmark), emptyList())
            ).jsonObject
            val modifiedBenchmarkReport = JsonObject(HashMap(benchmarkReport.content).apply {
                put("repeat", JsonLiteral(i))
                put("warmup", JsonLiteral(warmupCount))
            })
            result.add(modifiedBenchmarkReport.toString())
        }
        logger.log("\n", usePrefix = false)
        return result
    }

    private fun execBenchmarkRepeatedly(filterArgs: List<String>, filterRegexArgs: List<String>) {
        val benchmarksToRun = getBenchmarksList(filterArgs, filterRegexArgs)
        val results = benchmarksToRun.flatMap { benchmark ->
            execSeparateBenchmarkRepeatedly(benchmark)
        }
        File(outputFileName).printWriter().use { out ->
            out.println("[${results.joinToString(",")}]")
        }
    }

    @TaskAction
    override fun exec() {
        assert(outputFileName != null) { "Output file name should be always set" }
        predefinedArgs = args ?: emptyList()
        val filterArgs = filter.splitCommaSeparatedOption("-f")
        val filterRegexArgs = filterRegex.splitCommaSeparatedOption("-fr")
        when (repeatingType) {
            BenchmarkRepeatingType.INTERNAL -> executeTask(warmupCount, repeatCount, filterArgs, filterRegexArgs)
            BenchmarkRepeatingType.EXTERNAL -> execBenchmarkRepeatedly(filterArgs, filterRegexArgs)
        }

    }
}
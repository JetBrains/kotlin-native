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
@file:UseExperimental(ExperimentalCli::class)
package org.jetbrains.benchmarksLauncher

import org.jetbrains.report.BenchmarkResult
import kotlinx.cli.*

typealias RecordTimeMeasurement = (Int, Int, Double) -> Unit

abstract class Launcher {
    abstract val benchmarks: BenchmarksCollection

    fun add(name: String, benchmark: AbstractBenchmarkEntry) {
        benchmarks[name] = benchmark
    }

    fun measureLambda(repeatNumber: Int, lambda: () -> Any?): Long {
        var i = repeatNumber
        cleanup()
        return measureNanoTime {
            while (i-- > 0) lambda()
            cleanup()
        }
    }

    enum class LogLevel { DEBUG, OFF }

    class Logger(val level: LogLevel = LogLevel.OFF) {
         fun log(message: String, messageLevel: LogLevel = LogLevel.DEBUG, usePrefix: Boolean = true) {
            if (messageLevel == level) {
                if (usePrefix) {
                    printStderr("[$level][${currentTime()}] $message")
                } else {
                    printStderr("$message")
                }
            }
        }
    }

    fun measureRepeatedly(logger: Logger,
                          numWarmIterations: Int,
                          numberOfAttempts: Int,
                          name: String,
                          recordMeasurement: RecordTimeMeasurement,
                          lambda: () -> Any?) {
        logger.log("Warm up iterations for benchmark $name\n")
        measureLambda(numWarmIterations, lambda)
        var autoEvaluatedNumberOfMeasureIteration = 1
        while (true) {
            var j = autoEvaluatedNumberOfMeasureIteration
            val time = measureLambda(j, lambda)
            if (time >= 100L * 1_000_000) // 100ms
                break
            autoEvaluatedNumberOfMeasureIteration *= 2
        }
        logger.log("Running benchmark $name ")
        for (k in 0..numberOfAttempts) {
            logger.log(".", usePrefix = false)
            var i = autoEvaluatedNumberOfMeasureIteration
            val time = measureLambda(i, lambda)
            val scaledTime = time * 1.0 / autoEvaluatedNumberOfMeasureIteration
            // Save benchmark object
            recordMeasurement(k, numWarmIterations, scaledTime)
        }
        logger.log("\n", usePrefix = false)
    }

    fun measureOnce(logger: Logger,
                    name: String,
                    recordMeasurement: RecordTimeMeasurement,
                    lambda: () -> Any?) {
        logger.log("Skipping warm up for benchmark $name\n")
        logger.log("Running benchmark $name .")
        val time = measureLambda(1, lambda)
        recordMeasurement(0, 0, time.toDouble())
    }

    fun runBenchmark(logger: Logger,
                     numWarmIterations: Int,
                     numberOfAttempts: Int,
                     name: String,
                     recordMeasurement: RecordTimeMeasurement,
                     benchmark: AbstractBenchmarkEntry) {
        when (benchmark) {
            is BenchmarkEntryWithInit -> {
                val benchmarkInstance = benchmark.ctor?.invoke()
                measureRepeatedly(logger,
                                  numWarmIterations,
                                  numberOfAttempts,
                                  name,
                                  recordMeasurement) {
                    benchmark.lambda(benchmarkInstance!!)
                }
            }
            is BenchmarkEntry -> {
                measureRepeatedly(logger,
                                  numWarmIterations,
                                  numberOfAttempts,
                                  name,
                                  recordMeasurement,
                                  benchmark.lambda)
            }
            is BenchmarkEntryStatic -> {
                // TODO: Static benchmarks still need to be run several times to get proper statistics,
                // but they need to be run from separate processes.
                measureOnce(logger,
                            name,
                            recordMeasurement) {
                    benchmark.run()
                }
            }
            else -> error("unknown benchmark type $benchmark")
        }
    }

    fun launch(numWarmIterations: Int,
               numberOfAttempts: Int,
               prefix: String = "",
               filters: Collection<String>? = null,
               filterRegexes: Collection<String>? = null,
               verbose: Boolean): List<BenchmarkResult> {
        val logger = if (verbose) Logger(LogLevel.DEBUG) else Logger()
        val regexes = filterRegexes?.map { it.toRegex() } ?: listOf()
        val filterSet = filters?.toHashSet() ?: hashSetOf()
        // Filter benchmarks using given filters, or run all benchmarks if none were given.
        val runningBenchmarks = if (filterSet.isNotEmpty() || regexes.isNotEmpty()) {
            benchmarks.filterKeys { benchmark -> benchmark in filterSet || regexes.any { it.matches(benchmark) } }
        } else benchmarks
        if (runningBenchmarks.isEmpty()) {
            printStderr("No matching benchmarks found\n")
            error("No matching benchmarks found")
        }
        val benchmarkResults = mutableListOf<BenchmarkResult>()
        for ((name, benchmark) in runningBenchmarks) {
            val recordMeasurement : RecordTimeMeasurement = { iteration: Int, warmupCount: Int, durationNs: Double ->
                benchmarkResults.add(BenchmarkResult("$prefix$name", BenchmarkResult.Status.PASSED,
                        durationNs / 1000, BenchmarkResult.Metric.EXECUTION_TIME, durationNs / 1000,
                        iteration + 1, warmupCount))
            }
            try {
                runBenchmark(logger, numWarmIterations, numberOfAttempts, name, recordMeasurement, benchmark)
            } catch (e: Throwable) {
                printStderr("Failed to run benchmark $name: $e\n")
                error("Failed to run benchmark $name: $e")
            }
        }
        return benchmarkResults
    }

    fun benchmarksListAction() {
        benchmarks.keys.forEach {
            println(it)
        }
    }
}

abstract class BenchmarkArguments(argParser: ArgParser)

class BaseBenchmarkArguments(argParser: ArgParser): BenchmarkArguments(argParser) {
    val warmup by argParser.option(ArgType.Int, shortName = "w", description = "Number of warm up iterations")
            .default(20)
    val repeat by argParser.option(ArgType.Int, shortName = "r", description = "Number of each benchmark run").
            default(60)
    val prefix by argParser.option(ArgType.String, shortName = "p", description = "Prefix added to benchmark name")
            .default("")
    val output by argParser.option(ArgType.String, shortName = "o", description = "Output file")
    val filter by argParser.option(ArgType.String, shortName = "f", description = "Benchmark to run").multiple()
    val filterRegex by argParser.option(ArgType.String, shortName = "fr",
            description = "Benchmark to run, described by a regular expression").multiple()
    val verbose by argParser.option(ArgType.Boolean, shortName = "v", description = "Verbose mode of running")
            .default(false)
}

object BenchmarksRunner {
    fun parse(args: Array<String>, benchmarksListAction: ()->Unit): BenchmarkArguments? {
        class List: Subcommand("list", "Show list of benchmarks") {
            override fun execute() {
                benchmarksListAction()
            }
        }

        // Parse args.
        val argParser = ArgParser("benchmark")
        argParser.subcommands(List())
        val argumentsValues = BaseBenchmarkArguments(argParser)
        return if (argParser.parse(args).commandName == "benchmark") argumentsValues else null
    }

    fun collect(results: List<BenchmarkResult>, arguments: BenchmarkArguments) {
        if (arguments is BaseBenchmarkArguments) {
            JsonReportCreator(results).printJsonReport(arguments.output)
        }
    }

    fun runBenchmarks(args: Array<String>,
                      run: (parser: BenchmarkArguments) -> List<BenchmarkResult>,
                      parseArgs: (args: Array<String>, benchmarksListAction: ()->Unit) -> BenchmarkArguments? = this::parse,
                      collect: (results: List<BenchmarkResult>, arguments: BenchmarkArguments) -> Unit = this::collect,
                      benchmarksListAction: ()->Unit) {
        val arguments = parseArgs(args, benchmarksListAction)
        arguments?.let {
            val results = run(arguments)
            collect(results, arguments)
        }
    }
}

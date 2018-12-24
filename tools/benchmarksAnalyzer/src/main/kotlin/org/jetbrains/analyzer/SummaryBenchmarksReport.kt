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


package org.jetbrains.benchmarksAnalyzer
import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.Environment
import org.jetbrains.report.Compiler
import org.jetbrains.report.BenchmarksReport
import kotlin.math.abs

typealias SummaryBench = Pair<MeanVarianceBench?, MeanVarianceBench?>
typealias BenchTable = Map<String, MeanVarianceBench>
typealias SummaryBenchTable = Map<String, SummaryBench>
typealias ScoreChange = Pair<MeanVariance, MeanVariance>

// Summary report with comparasion of separate benchmarks results
class SummaryBenchmarksReport (val currentReport: BenchmarksReport,
                               val previousReport: BenchmarksReport? = null,
                               val meaningfulChangesValue: Double = 0.5) {

    // Lists of benchmarks in different status
    private val failedBenchs = mutableListOf<String>()
    private val addedBenchs = mutableListOf<String>()
    private val removedBenchs = mutableListOf<String>()
    private val newFailures = mutableListOf<FieldChange>()
    private val newPasses = mutableListOf<FieldChange>()

    // Maps with changes of performance.
    private var regressions = mutableMapOf<String, ScoreChange>()
    private var improvements = mutableMapOf<String, ScoreChange>()

    // Report created by joining comparing reports.
    private val mergedReport = mutableMapOf<String, SummaryBench>()

    // Summary value of report - geometric mean
    private lateinit var geoMeanBench: SummaryBench
    private var geoMeanScoreChange: ScoreChange? = null

    // Changes in environment and tools.
    private val envChanges = mutableListOf<FieldChange>()
    private val kotlinChanges = mutableListOf<FieldChange>()

    init {
        // Count avarage values for each benchmark.
        val currentBenchTable = collectMeanResults(currentReport.benchmarks)
        var previousBenchTable: BenchTable? = null
        previousReport?.let {
            previousBenchTable = collectMeanResults(previousReport.benchmarks)
            // Check changes in environment and tools
            analyzeEnvChanges(currentReport.env, previousReport.env)
            analyzeKotlinChanges(currentReport.compiler, previousReport.compiler)
        }
        createMergedReport(currentBenchTable, previousBenchTable)
        previousBenchTable?.let {
            analyzePerformanceChanges()
        }
    }

    // Create geometric mean.
    private fun createGeoMeanBenchmark(benchTable: BenchTable): MeanVarianceBench {
        val geoMeanBenchName = "Geometric mean"
        val geoMean = Statistics.geometricMean(benchTable.toList().map { (_, value) -> value.meanBenchmark.score })
        val varianceGeoMean = Statistics.geometricMean(benchTable.toList().map { (_, value) -> value.varianceBenchmark.score })
        val meanBench = BenchmarkResult(geoMeanBenchName, geoMean)
        val varianceBench = BenchmarkResult(geoMeanBenchName, varianceGeoMean)
        return MeanVarianceBench(meanBench, varianceBench)
    }

    // Merge current and compare to report
    private fun createMergedReport(curBenchs: BenchTable, prevBenchs: BenchTable?) {
        // Find absent benchmarks.
        for ((name, current) in curBenchs) {
            val curBench = current.meanBenchmark
            // Check status.
            if (curBench.status == BenchmarkResult.Status.FAILED) {
                failedBenchs.add(name)
            }
            // Check existance of benchmark in previous results.
            if (prevBenchs == null || name !in prevBenchs) {
                mergedReport[name] = SummaryBench(current, null)
                addedBenchs.add(name)
            } else {
                val prevBench = prevBenchs[name]!!.meanBenchmark
                mergedReport[name] = SummaryBench(current, prevBenchs[name])
                // Explore change of status
                if (prevBench.status != curBench.status) {
                    val statusChange = FieldChange("$name", prevBench.status, curBench.status)
                    if (curBench.status == BenchmarkResult.Status.FAILED) {
                        newFailures.add(statusChange)
                    } else {
                        newPasses.add(statusChange)
                    }
                }
            }
        }

        // Find removed becnhmarks.
        prevBenchs?.let {
            for ((name, prevBench) in prevBenchs) {
                if (name !in curBenchs) {
                    mergedReport[name] = SummaryBench(null, prevBench)
                    removedBenchs.add(name)
                }
            }
        }

        // Calculate geometric mean.
        val curGeoMean = createGeoMeanBenchmark(curBenchs)
        val prevGeoMean = if (prevBenchs != null) createGeoMeanBenchmark(prevBenchs) else null
        geoMeanBench = SummaryBench(curGeoMean, prevGeoMean)
    }

    // Analyze and collect changes in performance between same becnhmarks.
    private fun analyzePerformanceChanges() {
        for ((name, element) in mergedReport) {
            val current = element.first
            val previous = element.second
            if (current != null && previous != null) {
                // Calculate metrics for showing difference
                val percent = current.calcPercentageDiff(previous)
                val ratio = current.calcRatio(previous)

                // Save changes if they are meaningful
                if (percent.mean >= meaningfulChangesValue) {
                    regressions[name] = Pair(percent, ratio)
                } else if (percent.mean <= -meaningfulChangesValue) {
                    improvements[name] = Pair(percent, ratio)
                }
            }
        }
        // Sort regressions and improvements
        regressions = regressions.toList().sortedByDescending { (_, value) -> value.first.mean }.toMap().toMutableMap()
        improvements = improvements.toList().sortedBy { (_, value) -> value.first.mean }.toMap().toMutableMap()

        // Calculate change for geometric mean
        geoMeanScoreChange = Pair(geoMeanBench.first!!.calcPercentageDiff(geoMeanBench.second!!),
                geoMeanBench.first!!.calcRatio(geoMeanBench.second!!))
    }

    private fun addFieldChange(list: MutableList<FieldChange>, field: String, previous: Any, current: Any) {
        FieldChange.getFieldChangeOrNull(field, previous, current)?.let {
            list.add(it)
        }
    }

    private fun analyzeEnvChanges(curEnv: Environment, prevEnv: Environment) {
        addFieldChange(envChanges,"Machine CPU", prevEnv.machine.cpu, curEnv.machine.cpu)
        addFieldChange(envChanges,"Machine OS", prevEnv.machine.os, curEnv.machine.os)
        addFieldChange(envChanges,"JDK version", prevEnv.jdk.version, curEnv.jdk.version)
        addFieldChange(envChanges,"JDK vendor", prevEnv.jdk.vendor, curEnv.jdk.vendor)
    }

    private fun analyzeKotlinChanges(curCompiler: Compiler, prevCompiler: Compiler) {
        addFieldChange(kotlinChanges,"Backend type", prevCompiler.backend.type, curCompiler.backend.type)
        addFieldChange(kotlinChanges,"Backend version", prevCompiler.backend.version, curCompiler.backend.version)
        addFieldChange(kotlinChanges,"Backend flags", prevCompiler.backend.flags, curCompiler.backend.flags)
        addFieldChange(kotlinChanges,"Kotlin version", prevCompiler.kotlinVersion, curCompiler.kotlinVersion)
    }

    // Calculate avarage results for bencmarks (each becnhmark can be run several times)
    fun collectMeanResults(benchmarks: Map<String, List<BenchmarkResult>>): BenchTable {
        val summaryBenchmarks = mutableMapOf<String, MeanVarianceBench>()
        for ((name, resultsSet) in benchmarks) {
            val repeatSeq = IntArray(resultsSet.size)
            var curStatus = BenchmarkResult.Status.PASSED
            var currentWarmup = -1

            // Collect common becnhmark values and check them.
            resultsSet.forEachIndexed { index, result ->
                // If there was at least one failure, summary is marked as failure
                if (result.status == BenchmarkResult.Status.FAILED) {
                    curStatus = result.status
                }
                repeatSeq[index] = result.repeat
                if (currentWarmup != -1)
                    if (result.warmup != currentWarmup)
                        println("Check data consistency. Warmup value for benchmark '${result.name}' differs.")
                currentWarmup = result.warmup
            }

            repeatSeq.sort()
            // Check if there are missed loop during running benchmarks.
            repeatSeq.forEachIndexed { index, element ->
                if (index != 0)
                    if ((element - repeatSeq[index - 1]) != 1)
                        println("Check data consistency. For benchmark '$name' there is no run between ${repeatSeq[index - 1]} and $element.")
            }

            // Create mean and variance benchmarks result.
            val scoreMeanVariance = Statistics.getMeanVariance(resultsSet.map{it.score})
            val runtimeInUsMeanVariance = Statistics.getMeanVariance(resultsSet.map{it.runtimeInUs})
            val meanBench = BenchmarkResult(name, curStatus, scoreMeanVariance.mean,
                                            runtimeInUsMeanVariance.mean, repeatSeq[resultsSet.size - 1],
                                            currentWarmup)
            val varianceBench = BenchmarkResult(name, curStatus, scoreMeanVariance.variance,
                                                runtimeInUsMeanVariance.variance, repeatSeq[resultsSet.size - 1],
                                                currentWarmup)
            summaryBenchmarks[name] = MeanVarianceBench(meanBench, varianceBench)
        }

        return summaryBenchmarks
    }

    // Print report using render
    fun print(renderInstance: Render, onlyChanges: Boolean = false, outputFile: String? = null) {
        val content = renderInstance.render(onlyChanges)
        outputFile?.let {
            writeToFile(outputFile, content)
        } ?: run {
            println(content)
        }
    }

    // Common interface for printing report in different formats.
    interface Render {
        fun render(onlyChanges: Boolean = false): String
        fun renderPerformanceSummary()
        fun renderStatusSummary()
        fun renderEnvChanges()
        fun renderPerformanceDetails(onlyChanges: Boolean = false)
    }

    // Report render to text format
    inner class TextRender: Render {
        private var content = ""
        private val headerSeparator = "================="

        private fun append(text: String = "") {
            content += "$text\n"
        }

        override fun render(onlyChanges: Boolean): String {
            renderEnvChanges()
            renderStatusSummary()
            renderPerformanceSummary()
            renderPerformanceDetails(onlyChanges)
            return content
        }

        private fun printBucketInfo(bucket: Collection<Any>, name: String) {
            if (!bucket.isEmpty()) {
                append("$name: ${bucket.size}")
            }
        }

        private fun printStatusChangeInfo(bucket: List<FieldChange>, name: String) {
            if (!bucket.isEmpty()) {
                append("$name:")
                for (change in bucket) {
                    append(change.renderAsText())
                }
            }
        }

        override fun renderEnvChanges() {
            if (!envChanges.isEmpty() || !kotlinChanges.isEmpty()) {
                append(ChangeReport("Environment", envChanges).renderAsTextReport())
                append(ChangeReport("Compiler", kotlinChanges).renderAsTextReport())
                append()
            }
        }

        private fun printPerformanceBucket(bucket: Map<String, ScoreChange>, bucketName: String) {
            if (!bucket.isEmpty()) {
                var percentsList = bucket.values.map{ it.first.mean }
                // Maps of regressions and improvements are sorted
                val maxValue = percentsList.first()
                var geomeanValue: Double
                if (percentsList.first() > 0.0) {
                    geomeanValue = Statistics.geometricMean(percentsList)
                } else {
                    // Geometric mean can be counted on positive numbers
                    val precision = abs(maxValue) + 1
                    percentsList = percentsList.map{it + precision}
                    geomeanValue = Statistics.geometricMean(percentsList) - precision
                }

                append("$bucketName: Maximum = ${formatValue(maxValue, true)}," +
                        " Geometric mean = ${formatValue(geomeanValue, true)}")
            }
        }

        override fun renderStatusSummary() {
            append("Status summary")
            append(headerSeparator)
            if (failedBenchs.isEmpty()) {
                append("All benchmarks passed!")
            }
            if (!failedBenchs.isEmpty() || !addedBenchs.isEmpty() || !removedBenchs.isEmpty()) {
                printBucketInfo(failedBenchs, "Failed benchmarks")
                printBucketInfo(addedBenchs, "Added benchmarks")
                printBucketInfo(removedBenchs, "Removed benchmarks")
            }
            append("Total becnhmarks number: ${mergedReport.keys.size}")
            append()

            if (!newFailures.isEmpty() || !newPasses.isEmpty()) {
                append("Changes in status")
                append(headerSeparator)
                printStatusChangeInfo(newFailures, "New failures")
                printStatusChangeInfo(newPasses, "New passes")
                append()
            }
        }

        override fun renderPerformanceSummary() {
            if (!regressions.isEmpty() || !improvements.isEmpty()) {
                append("Performance summary")
                append(headerSeparator)
                printPerformanceBucket(regressions, "Regressions")
                printPerformanceBucket(improvements, "Improvements")
                append()
            }
        }

        private fun formatValue(number: Double, isPercent: Boolean = false): String {
            if (isPercent) {
                return format(number, 2) + "%"
            }
            return format(number, 4)
        }

        private fun formatColumn(content:String, isWide: Boolean = false): String {
            val width = if (isWide) 50 else 25
            return content.padEnd(width, ' ')
        }

        private fun printBenchmarksDetails(fullSet: MutableMap<String, SummaryBench>,
                                           bucket: Map<String, ScoreChange>?) {
            if (bucket != null) {
                // There are changes in performance.
                // Output changed benchmarks.
                for ((name, change) in bucket) {
                    append("${formatColumn(name, true)} " +
                            "${formatColumn(fullSet[name]!!.first!!.toString())} " +
                            "${formatColumn(fullSet[name]!!.second!!.toString())} " +
                            "${formatColumn(change.first.toString() + " %")} " +
                            "${formatColumn(change.second.toString())}")
                    fullSet.remove(name)
                }
            } else {
                // Output all values without performance changes.
                val placeholder = "-"
                for ((name, value) in fullSet) {
                    append("${formatColumn(name, true)} " +
                            "${formatColumn(value.first!!.toString())} " +
                            "${formatColumn(value.second!!.toString())} " +
                            "${formatColumn(placeholder)} " +
                            "${formatColumn(placeholder)}")
                }
            }
        }

        private fun printTableLineSeparator() {
            val tableWidth = 50 + 25 * 4
            val placeholder = "-"
            append("${placeholder.padEnd(tableWidth, '-')}")
        }

        private fun printPerformanceTableHeader() {
            val bench = formatColumn("Benchmark", true)
            val current = formatColumn("Current score")
            val previous = formatColumn("Previous score")
            val percent = formatColumn("Percent")
            val ratio = formatColumn("Ratio")
            append("$bench $current $previous $percent $ratio")
            printTableLineSeparator()
        }

        override fun renderPerformanceDetails(onlyChanges: Boolean) {
            append("Performance details")
            append(headerSeparator)

            if (onlyChanges) {
                if (regressions.isEmpty() && improvements.isEmpty()) {
                    append("All becnhmarks are stable.")
                }
            }

            printPerformanceTableHeader()

            // Copy report for eliminating already printed benchmarks.
            val fullSet = mergedReport.toMutableMap()
            // Print geometric mean
            val geoMeanChangeMap = if (geoMeanScoreChange != null)
                                        mapOf(geoMeanBench.first!!.meanBenchmark.name to geoMeanScoreChange!!)
                                    else null
            printBenchmarksDetails(mutableMapOf(geoMeanBench.first!!.meanBenchmark.name to geoMeanBench),
                                         geoMeanChangeMap)
            printTableLineSeparator()
            printBenchmarksDetails(fullSet, regressions)
            printBenchmarksDetails(fullSet, improvements)
            if (!onlyChanges) {
                // Print all left results
                printBenchmarksDetails(fullSet, null)
            }

        }
    }

    fun getTextRender(): TextRender {
        return TextRender()
    }
}
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


package org.jetbrains.analyzer
import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.Environment
import org.jetbrains.report.Compiler
import org.jetbrains.report.BenchmarksReport
import kotlin.math.abs

typealias SummaryBenchmark = Pair<MeanVarianceBenchmark?, MeanVarianceBenchmark?>
typealias BenchmarksTable = Map<String, MeanVarianceBenchmark>
typealias SummaryBenchmarksTable = Map<String, SummaryBenchmark>
typealias ScoreChange = Pair<MeanVariance, MeanVariance>

// Summary report with comparasion of separate benchmarks results.
class SummaryBenchmarksReport (val currentReport: BenchmarksReport,
                               val previousReport: BenchmarksReport? = null,
                               val meaningfulChangesValue: Double = 0.5) {

    // Lists of benchmarks in different status.
    private val failedBenchmarks = mutableListOf<String>()
    private val addedBenchmarks = mutableListOf<String>()
    private val benchmarksWithChangedStatus = mutableListOf<FieldChange<BenchmarkResult.Status>>()

    private lateinit var removedBenchmarks: List<String>

    // Maps with changes of performance.
    private lateinit var regressions: Map<String, ScoreChange>
    private lateinit var improvements: Map<String, ScoreChange>

    // Report created by joining comparing reports.
    private val mergedReport = mutableMapOf<String, SummaryBenchmark>()

    // Summary value of report - geometric mean.
    private lateinit var geoMeanBenchmark: SummaryBenchmark
    private var geoMeanScoreChange: ScoreChange? = null

    // Changes in environment and tools.
    private val envChanges = mutableListOf<FieldChange<String>>()
    private val kotlinChanges = mutableListOf<FieldChange<String>>()

    init {
        // Count avarage values for each benchmark.
        val currentBenchmarksTable = collectMeanResults(currentReport.benchmarks)
        val previousBenchmarksTable = previousReport?.let {
            // Check changes in environment and tools.
            analyzeEnvChanges(currentReport.env, previousReport.env)
            analyzeKotlinChanges(currentReport.compiler, previousReport.compiler)
            collectMeanResults(previousReport.benchmarks)
        }
        createMergedReport(currentBenchmarksTable, previousBenchmarksTable)
        previousBenchmarksTable?.let {
            analyzePerformanceChanges()
        } ?: run {
            regressions = mapOf<String, ScoreChange>()
            improvements = mapOf<String, ScoreChange>()
        }
    }

    // Create geometric mean.
    private fun createGeoMeanBenchmark(benchTable: BenchmarksTable): MeanVarianceBenchmark {
        val geoMeanBenchmarkName = "Geometric mean"
        val geoMean = geometricMean(benchTable.toList().map { (_, value) -> value.meanBenchmark.score })
        val varianceGeoMean = geometricMean(benchTable.toList().map { (_, value) -> value.varianceBenchmark.score })
        val meanBenchmark = BenchmarkResult(geoMeanBenchmarkName, geoMean)
        val varianceBenchmark = BenchmarkResult(geoMeanBenchmarkName, varianceGeoMean)
        return MeanVarianceBenchmark(meanBenchmark, varianceBenchmark)
    }

    // Merge current and compare to report.
    private fun createMergedReport(currentBenchmarks: BenchmarksTable, previousBenchmarks: BenchmarksTable?) {
        currentBenchmarks.forEach { (name, current) ->
            val currentBenchmark = current.meanBenchmark
            // Check status.
            if (currentBenchmark.status == BenchmarkResult.Status.FAILED) {
                failedBenchmarks.add(name)
            }
            // Check existance of benchmark in previous results.
            if (previousBenchmarks == null || name !in previousBenchmarks) {
                mergedReport[name] = SummaryBenchmark(current, null)
                addedBenchmarks.add(name)
            } else {
                val previousBenchmark = previousBenchmarks[name]!!.meanBenchmark
                mergedReport[name] = SummaryBenchmark(current, previousBenchmarks[name])
                // Explore change of status.
                if (previousBenchmark.status != currentBenchmark.status) {
                    val statusChange = FieldChange("$name", previousBenchmark.status, currentBenchmark.status)
                    benchmarksWithChangedStatus.add(statusChange)
                }
            }
        }

        // Find removed benchmarks.
        removedBenchmarks = previousBenchmarks?.filter { (key, _) -> key !in currentBenchmarks }?.toList()
                ?.map { (key, value) ->
                        mergedReport[key] = SummaryBenchmark(null, value)
                    key
        } ?: listOf<String>()

        // Calculate geometric mean.
        val currentGeoMean = createGeoMeanBenchmark(currentBenchmarks)
        val previousGeoMean = previousBenchmarks?. let { createGeoMeanBenchmark(previousBenchmarks) }
        geoMeanBenchmark = SummaryBenchmark(currentGeoMean, previousGeoMean)
    }

    private fun getBenchmarkPerfomanceChange(name: String, benchmark: SummaryBenchmark): Pair<String, ScoreChange>? {
        val (current, previous) = benchmark
        current?.let {
            previous?.let {
                // Calculate metrics for showing difference.
                val percent = current.calcPercentageDiff(previous)
                val ratio = current.calcRatio(previous)
                if (abs(percent.mean) >= meaningfulChangesValue) {
                    return Pair(name, Pair(percent, ratio))
                }
            }
        }
        return null
    }

    // Analyze and collect changes in performance between same becnhmarks.
    private fun analyzePerformanceChanges() {
        val performanceChanges = mergedReport.asSequence().map {(name, element) ->
            getBenchmarkPerfomanceChange(name, element)
        }.filterNotNull().groupBy {
            if (it.second.first.mean > 0) "regressions" else "improvements"
        }

        // Sort regressions and improvements.
        regressions = performanceChanges["regressions"]
                ?.sortedByDescending { it.second.first.mean }?.map { it.first to it.second }
                ?.toMap() ?: mapOf<String, ScoreChange>()
        improvements = performanceChanges["improvements"]
                ?.sortedBy { it.second.first.mean }?.map { it.first to it.second }
                ?.toMap() ?: mapOf<String, ScoreChange>()

        // Calculate change for geometric mean.
        val (current, previous) = geoMeanBenchmark
        geoMeanScoreChange = current?. let {
            previous?. let {
                Pair(current.calcPercentageDiff(previous), current.calcRatio(previous))
            }
        }
    }

    private fun <T> MutableList<FieldChange<T>>.addFieldChange(field: String, previous: T, current: T) {
        FieldChange.getFieldChangeOrNull(field, previous, current)?.let {
            add(it)
        }
    }

    private fun analyzeEnvChanges(currentEnv: Environment, previousEnv: Environment) {
        envChanges.apply {
            addFieldChange("Machine CPU", previousEnv.machine.cpu, currentEnv.machine.cpu)
            addFieldChange("Machine OS", previousEnv.machine.os, currentEnv.machine.os)
            addFieldChange("JDK version", previousEnv.jdk.version, currentEnv.jdk.version)
            addFieldChange("JDK vendor", previousEnv.jdk.vendor, currentEnv.jdk.vendor)
        }
    }

    private fun analyzeKotlinChanges(currentCompiler: Compiler, previousCompiler: Compiler) {
        kotlinChanges.apply {
            addFieldChange("Backend type", previousCompiler.backend.type.type, currentCompiler.backend.type.type)
            addFieldChange("Backend version", previousCompiler.backend.version, currentCompiler.backend.version)
            addFieldChange("Backend flags", previousCompiler.backend.flags.toString(),
                    currentCompiler.backend.flags.toString())
            addFieldChange( "Kotlin version", previousCompiler.kotlinVersion, currentCompiler.kotlinVersion)
        }
    }

    // Common interface for printing report in different formats.
    interface Render {
        fun render(onlyChanges: Boolean = false): String
        fun renderPerformanceSummary()
        fun renderStatusSummary()
        fun renderEnvChanges()
        fun renderPerformanceDetails(onlyChanges: Boolean = false)
        fun print(onlyChanges: Boolean = false, outputFile: String? = null)
    }

    // Report render to text format.
    inner class TextRender: Render {
        private var content = StringBuilder()
        private val headerSeparator = "================="
        private val wideColumnWidth = 50
        private val standardColumnWidth = 25

        private fun append(text: String = "") {
            content.append("$text\n")
        }

        // Print report using render.
        override fun print(onlyChanges: Boolean, outputFile: String?) {
            val content = render(onlyChanges)
            outputFile?.let {
                writeToFile(outputFile, content)
            } ?: println(content)
        }

        override fun render(onlyChanges: Boolean): String {
            renderEnvChanges()
            renderStatusSummary()
            renderPerformanceSummary()
            renderPerformanceDetails(onlyChanges)
            return content.toString()
        }

        private fun printBucketInfo(bucket: Collection<Any>, name: String) {
            if (!bucket.isEmpty()) {
                append("$name: ${bucket.size}")
            }
        }

        private fun <T> printStatusChangeInfo(bucket: List<FieldChange<T>>, name: String) {
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
                // Maps of regressions and improvements are sorted.
                val maxValue = percentsList.first()
                var geomeanValue: Double
                if (percentsList.first() > 0.0) {
                    geomeanValue = geometricMean(percentsList)
                } else {
                    // Geometric mean can be counted on positive numbers.
                    val precision = abs(maxValue) + 1
                    percentsList = percentsList.map{it + precision}
                    geomeanValue = geometricMean(percentsList) - precision
                }

                append("$bucketName: Maximum = ${formatValue(maxValue, true)}," +
                        " Geometric mean = ${formatValue(geomeanValue, true)}")
            }
        }

        override fun renderStatusSummary() {
            append("Status summary")
            append(headerSeparator)
            if (failedBenchmarks.isEmpty()) {
                append("All benchmarks passed!")
            }
            if (!failedBenchmarks.isEmpty() || !addedBenchmarks.isEmpty() || !removedBenchmarks.isEmpty()) {
                printBucketInfo(failedBenchmarks, "Failed benchmarks")
                printBucketInfo(addedBenchmarks, "Added benchmarks")
                printBucketInfo(removedBenchmarks, "Removed benchmarks")
            }
            append("Total becnhmarks number: ${mergedReport.keys.size}")
            append()

            if (!benchmarksWithChangedStatus.isEmpty()) {
                append("Changes in status")
                append(headerSeparator)
                printStatusChangeInfo(benchmarksWithChangedStatus
                        .filter { it.current == BenchmarkResult.Status.FAILED }, "New failures")
                printStatusChangeInfo(benchmarksWithChangedStatus
                        .filter { it.current == BenchmarkResult.Status.PASSED }, "New passes")
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

        private fun formatValue(number: Double, isPercent: Boolean = false): String =
                if (isPercent) format(number, 2) + "%" else format(number)

        private fun formatColumn(content:String, isWide: Boolean = false): String =
            content.padEnd(if (isWide) wideColumnWidth else standardColumnWidth, ' ')

        private fun printBenchmarksDetails(fullSet: Map<String, SummaryBenchmark>,
                                           bucket: Map<String, ScoreChange>? = null) {
            bucket?.let {
                // There are changes in performance.
                // Output changed benchmarks.
                for ((name, change) in bucket) {
                    append("${formatColumn(name, true)} " +
                            "${formatColumn(fullSet[name]!!.first!!.toString())}" +
                            "${formatColumn(fullSet[name]!!.second!!.toString())}" +
                            "${formatColumn(change.first.toString() + " %")}" +
                            "${formatColumn(change.second.toString())}")
                }
            } ?: let {
                // Output all values without performance changes.
                val placeholder = "-"
                for ((name, value) in fullSet) {
                    append("${formatColumn(name, true)}" +
                            "${formatColumn(value.first?.toString() ?: placeholder)}" +
                            "${formatColumn(value.second?.toString() ?: placeholder)}" +
                            "${formatColumn(placeholder)}" +
                            "${formatColumn(placeholder)}")
                }
            }
        }

        private fun printTableLineSeparator(tableWidth: Int) =
            append("${"-".padEnd(tableWidth, '-')}")

        private fun printPerformanceTableHeader(): Int {
            val wideColumns = listOf(formatColumn("Benchmark", true))
            val standardColumns = listOf(formatColumn("Current score"),
                    formatColumn("Previous score"),
                    formatColumn("Percent"),
                    formatColumn("Ratio"))
            val tableWidth = wideColumnWidth * wideColumns.size + standardColumnWidth * standardColumns.size
            append("${wideColumns.joinToString(separator = "")}${standardColumns.joinToString(separator = "")}")
            printTableLineSeparator(tableWidth)
            return tableWidth
        }

        override fun renderPerformanceDetails(onlyChanges: Boolean) {
            append("Performance details")
            append(headerSeparator)

            if (onlyChanges) {
                if (regressions.isEmpty() && improvements.isEmpty()) {
                    append("All becnhmarks are stable.")
                }
            }

            val tableWidth = printPerformanceTableHeader()

            // Print geometric mean.
            val geoMeanChangeMap = geoMeanScoreChange?.
                                    let { mapOf(geoMeanBenchmark.first!!.meanBenchmark.name to geoMeanScoreChange!!) }
            printBenchmarksDetails(mutableMapOf(geoMeanBenchmark.first!!.meanBenchmark.name to geoMeanBenchmark),
                                         geoMeanChangeMap)
            printTableLineSeparator(tableWidth)
            printBenchmarksDetails(mergedReport, regressions)
            printBenchmarksDetails(mergedReport, improvements)
            if (!onlyChanges) {
                // Print all remaining results.
                printBenchmarksDetails(mergedReport.filter { it.key !in regressions.keys &&
                                                             it.key !in improvements.keys })
            }

        }
    }

    fun getTextRender(): TextRender {
        return TextRender()
    }
}
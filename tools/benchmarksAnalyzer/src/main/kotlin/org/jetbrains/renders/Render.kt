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


package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.BenchmarkResult

import kotlin.math.abs

// Common interface for printing report in different formats.
interface Render {
    fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean = false): String
    // Print report using render.
    fun print(report: SummaryBenchmarksReport, onlyChanges: Boolean = false, outputFile: String? = null) {
        val content = render(report, onlyChanges)
        outputFile?.let {
            writeToFile(outputFile, content)
        } ?: println(content)
    }
}

// Report render to text format.
class TextRender: Render {
    private val content = StringBuilder()
    private val headerSeparator = "================="
    private val wideColumnWidth = 50
    private val standardColumnWidth = 25

    private fun append(text: String = "") {
        content.append("$text\n")
    }

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        renderEnvChanges(report.envChanges, "Environment")
        renderEnvChanges(report.kotlinChanges, "Compiler")
        renderStatusSummary(report)
        renderStatusChangesDetails(report.getBenchmarksWithChangedStatus())
        renderPerformanceSummary(report.regressions, report.improvements)
        renderPerformanceDetails(report, onlyChanges)
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

    fun renderEnvChanges(envChanges: List<FieldChange<String>>, bucketName: String) {
        if (!envChanges.isEmpty()) {
            append(ChangeReport(bucketName, envChanges).renderAsTextReport())
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

    fun renderStatusChangesDetails(benchmarksWithChangedStatus: List<FieldChange<BenchmarkResult.Status>>) {
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

    fun renderStatusSummary(report: SummaryBenchmarksReport) {
        append("Status summary")
        append(headerSeparator)

        val failedBenchmarks = report.failedBenchmarks
        val addedBenchmarks = report.addedBenchmarks
        val removedBenchmarks = report.removedBenchmarks
        if (failedBenchmarks.isEmpty()) {
            append("All benchmarks passed!")
        }
        if (!failedBenchmarks.isEmpty() || !addedBenchmarks.isEmpty() || !removedBenchmarks.isEmpty()) {
            printBucketInfo(failedBenchmarks, "Failed benchmarks")
            printBucketInfo(addedBenchmarks, "Added benchmarks")
            printBucketInfo(removedBenchmarks, "Removed benchmarks")
        }
        append("Total becnhmarks number: ${report.benchmarksNumber}")
        append()
    }

    fun renderPerformanceSummary(regressions: Map<String, ScoreChange>,
                                          improvements: Map<String, ScoreChange>) {
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
        if (bucket != null) {
            // There are changes in performance.
            // Output changed benchmarks.
            for ((name, change) in bucket) {
                append(formatColumn(name, true) +
                        formatColumn(fullSet[name]!!.first!!.toString()) +
                        formatColumn(fullSet[name]!!.second!!.toString()) +
                        formatColumn(change.first.toString() + " %") +
                        formatColumn(change.second.toString()))
            }
        } else {
            // Output all values without performance changes.
            val placeholder = "-"
            for ((name, value) in fullSet) {
                append(formatColumn(name, true) +
                        formatColumn(value.first?.toString() ?: placeholder) +
                        formatColumn(value.second?.toString() ?: placeholder) +
                        formatColumn(placeholder) +
                        formatColumn(placeholder))
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

    fun renderPerformanceDetails(report: SummaryBenchmarksReport, onlyChanges: Boolean = false) {
        append("Performance details")
        append(headerSeparator)

        if (onlyChanges) {
            if (report.regressions.isEmpty() && report.improvements.isEmpty()) {
                append("All becnhmarks are stable.")
            }
        }

        val tableWidth = printPerformanceTableHeader()
        // Print geometric mean.
        val geoMeanChangeMap = report.geoMeanScoreChange?.
                let { mapOf(report.geoMeanBenchmark.first!!.meanBenchmark.name to report.geoMeanScoreChange!!) }
        printBenchmarksDetails(
                mutableMapOf(report.geoMeanBenchmark.first!!.meanBenchmark.name to report.geoMeanBenchmark),
                geoMeanChangeMap)
        printTableLineSeparator(tableWidth)
        printBenchmarksDetails(report.mergedReport, report.regressions)
        printBenchmarksDetails(report.mergedReport, report.improvements)
        if (!onlyChanges) {
            // Print all remaining results.
            printBenchmarksDetails(report.mergedReport.filter { it.key !in report.regressions.keys &&
                    it.key !in report.improvements.keys })
        }
    }
}
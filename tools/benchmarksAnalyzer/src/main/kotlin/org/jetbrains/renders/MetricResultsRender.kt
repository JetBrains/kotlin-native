/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.BenchmarkResult

// Report render to text format.
class MetricResultsRender: Render() {
    override val name: String
        get() = "metrics"

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        val results = report.mergedReport.map { entry ->
            buildString {
                val metric = entry.value.first!!.meanBenchmark.metric
                append("{ \"benchmarkName\": \"${entry.key.removeSuffix(metric.suffix)}\",")
                append("\"metric\": \"${metric}\",")
                append("\"value\": \"${entry.value.first!!.meanBenchmark.score}\" }")
            }
        }.joinToString(", ")
        return "[ $results ]"

    }
}
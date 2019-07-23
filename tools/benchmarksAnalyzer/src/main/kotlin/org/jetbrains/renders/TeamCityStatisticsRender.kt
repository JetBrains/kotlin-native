/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.BenchmarkResult

// Report render to text format.
class TeamCityStatisticsRender: Render() {
    override val name: String
        get() = "teamcity"

    private var content = StringBuilder()

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        val currentDurations = report.currentBenchmarksDuration

        content.append("##teamcity[testSuiteStarted name='Benchmarks']\n")

        // For current benchmarks print score as TeamCity Test Metadata
        report.currentMeanVarianceBenchmarks.forEach { benchmark ->
            renderBenchmark(benchmark.meanBenchmark, currentDurations[benchmark.meanBenchmark.name]!!)
            renderSummaryBecnhmarkValue(benchmark.meanBenchmark, "Mean")
            renderSummaryBecnhmarkValue(benchmark.varianceBenchmark, "Variance")
        }
        content.append("##teamcity[testSuiteFinished name='Benchmarks']\n")

        // Report geometric mean as build statistic value
        renderGeometricMean(report.geoMeanBenchmark.first!!)

        return content.toString()
    }

    private fun renderSummaryBecnhmarkValue(benchmark: BenchmarkResult, metric: String) =
            content.append("##teamcity[testMetadata testName='${benchmark.name}' name='$metric'" +
                    " type='number' value='${benchmark.score}']\n")

    // Produce benchmark as test in TeamCity
    private fun renderBenchmark(benchmark: BenchmarkResult , duration: Double) {
        content.append("##teamcity[testStarted name='${benchmark.name}']\n")
        if (benchmark.status == BenchmarkResult.Status.FAILED) {
            content.append("##teamcity[testFailed name='${benchmark.name}']\n")
        }
        // test_duration_in_milliseconds is set for TeamCity
        content.append("##teamcity[testFinished name='${benchmark.name}' duration='${(duration / 1000).toInt()}']\n")
    }

    private fun renderGeometricMean(geoMeanBenchmark: MeanVarianceBenchmark) {
        content.append("##teamcity[buildStatisticValue key='Geometric mean' value='${geoMeanBenchmark.meanBenchmark.score}']\n")
        content.append("##teamcity[buildStatisticValue key='Geometric mean variance' value='${geoMeanBenchmark.varianceBenchmark.score}']\n")
    }
}
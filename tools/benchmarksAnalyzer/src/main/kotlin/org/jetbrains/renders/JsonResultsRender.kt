/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.*

// Report render to text format.
class JsonResultsRender: Render() {
    override val name: String
        get() = "json"

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        val setsInfo = report.benchmarksSetsInfo.first.groupBy { it.name }
        val sets = report.benchmarksParents.first.map { (name, benchmarks) ->
            BenchmarksSet(setsInfo[name]!!.first(),
                benchmarks.map { report.mergedReport[it]!!.first!! }) }
        val resultReport = BenchmarksReport(report.environments.first, sets, report.compilers.first)
        return resultReport.toJson()
    }
}
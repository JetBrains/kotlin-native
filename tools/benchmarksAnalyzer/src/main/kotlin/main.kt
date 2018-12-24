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

import org.jetbrains.benchmarksAnalyzer.readFile
import org.jetbrains.benchmarksAnalyzer.SummaryBenchmarksReport
import org.jetbrains.report.BenchmarksReport
import org.jetbrains.report.json.JsonTreeParser

fun printError(message: String) {
    val usageDesc = "Usage: benchComparator main_report report_to_compare_to [-o output_file] [-eps eps_value] [-short]\n"
    error("${usageDesc}${message}")
}

fun main(args: Array<String>) {
    var mainReport: String? = null
    var compareToReport: String? = null
    var outputFile: String? = null
    var eps: Double = 0.5
    var shortReport = false

    // Parse args
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-o" -> {
                if (i + 1 >= args.size) {
                    printError("Output file name is expected after -o flag")
                }
                outputFile = args[i+1]
                i = i + 1
            }
            "-eps" -> {
                if (i + 1 >= args.size) {
                    printError("Value of meaningful performance changes is expected after -eps flag")
                }
                eps = args[i+1].toDouble()
                i = i + 1
            }
            "-short" -> {
                shortReport = true
            }
            else -> {
                if (mainReport == null) {
                    mainReport = args[i]
                } else if (compareToReport == null) {
                    compareToReport = args[i]
                } else {
                    printError("Too many arguments.")
                }
            }
        }
        i++
    }

    if (mainReport == null) {
        printError("At least one file with benchmarks results should be provided!")
    }

    // Read contents of file.
    val mainBenchsResults = readFile(mainReport!!)
    val mainReportElement = JsonTreeParser.parse(mainBenchsResults)
    val mainBenchsReport = BenchmarksReport.create(mainReportElement)
    var compareToBenchsReport: BenchmarksReport? = null
    if (compareToReport != null) {
        val compareToResults = readFile(compareToReport)
        val compareToReportElement = JsonTreeParser.parse(compareToResults)
        compareToBenchsReport = BenchmarksReport.create(compareToReportElement)
    }

    // Generate comparasion report
    val summaryReport = SummaryBenchmarksReport(mainBenchsReport, compareToBenchsReport, eps)
    summaryReport.print(summaryReport.getTextRender(), shortReport, outputFile)
}
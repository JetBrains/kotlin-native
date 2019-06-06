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

import org.jetbrains.analyzer.sendGetRequest
import org.jetbrains.analyzer.readFile
import org.jetbrains.analyzer.SummaryBenchmarksReport
import org.jetbrains.kliopt.*
import org.jetbrains.renders.*
import org.jetbrains.report.BenchmarksReport
import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.json.JsonTreeParser

abstract class Connector {
    abstract val connectorPrefix: String

    fun isCompatible(fileName: String) =
            fileName.startsWith(connectorPrefix)

    abstract fun getFileContent(fileLocation: String, user: String? = null): String
}

object BintrayConnector : Connector() {
    override val connectorPrefix = "bintray:"
    val bintrayUrl = "https://dl.bintray.com/content/lepilkinaelena/KotlinNativePerformance"

    override fun getFileContent(fileLocation: String, user: String?): String {
        val fileParametersSize = 3
        val fileDescription = fileLocation.substringAfter(connectorPrefix)
        val fileParameters = fileDescription.split(':', limit = fileParametersSize)

        // Right link to bintray file.
        if (fileParameters.size == 1) {
            val accessFileUrl = "$bintrayUrl/${fileParameters[0]}"
            return sendGetRequest(accessFileUrl, followLocation = true)
        }
        // Used builds description format.
        if (fileParameters.size != fileParametersSize) {
            error("To get file from bintray, please, specify, build number from TeamCity and target" +
                    " in format bintray:build_number:target:filename")
        }
        val (buildNumber, target, fileName) = fileParameters
        val accessFileUrl = "$bintrayUrl/$target/$buildNumber/$fileName"
        return sendGetRequest(accessFileUrl, followLocation = true)
    }
}

object TeamCityConnector: Connector() {
    override val connectorPrefix = "teamcity:"
    val teamCityUrl = "http://buildserver.labs.intellij.net"

    override fun getFileContent(fileLocation: String, user: String?): String {
        val fileDescription = fileLocation.substringAfter(connectorPrefix)
        val buildLocator = fileDescription.substringBeforeLast(':')
        val fileName = fileDescription.substringAfterLast(':')
        if (fileDescription == fileLocation ||
                fileDescription == buildLocator || fileName == fileDescription) {
            error("To get file from TeamCity, please, specify, build locator and filename on TeamCity" +
                    " in format teamcity:build_locator:filename")
        }
        val accessFileUrl = "$teamCityUrl/app/rest/builds/$buildLocator/artifacts/content/$fileName"
        val userName = user?.substringBefore(':')
        val password = user?.substringAfter(':')
        return sendGetRequest(accessFileUrl, userName, password)
    }
}

fun getFileContent(fileName: String, user: String? = null): String {
    return when {
        BintrayConnector.isCompatible(fileName) -> BintrayConnector.getFileContent(fileName, user)
        TeamCityConnector.isCompatible(fileName) -> TeamCityConnector.getFileContent(fileName, user)
        else -> readFile(fileName)
    }
}

fun getBenchmarkReport(fileName: String, user: String? = null) =
        BenchmarksReport.create(JsonTreeParser.parse(getFileContent(fileName, user)))

fun parseNormalizeResults(results: String): Map<String, Map<String, Double>> {
    val parsedNormalizeResults = mutableMapOf<String, MutableMap<String, Double>>()
    val tokensNumber = 3
    results.lines().forEach {
        if (!it.isEmpty()) {
            val tokens = it.split(",").map { it.trim() }
            if (tokens.size != tokensNumber) {
                error("Data for normalization should include benchmark name, metric name and value. Got $it")
            }
            parsedNormalizeResults.getOrPut(tokens[0], { mutableMapOf<String, Double>() })[tokens[1]] = tokens[2].toDouble()
        }
    }
    return parsedNormalizeResults
}

// Prints text summary by users request.
/*fun summaryAction(argParser: ArgParser) {
    val benchsReport = SummaryBenchmarksReport(getBenchmarkReport(argParser.get<String>("mainReport")!!, argParser.get<String>("user")))
    val results = mutableListOf<String>()
    val executionNormalize = argParser.get<String>("exec-normalize")?.let {
        parseNormalizeResults(getFileContent(it))
    }
    val compileNormalize = argParser.get<String>("compile-normalize")?.let {
        parseNormalizeResults(getFileContent(it))
    }
    val codesizeNormalize = argParser.get<String>("codesize-normalize")?.let {
        parseNormalizeResults(getFileContent(it))
    }
    results.apply {
        add(benchsReport.failedBenchmarks.size.toString())
        argParser.getAll<String>("exec-samples")?. let {
            val filter = if (it.first() == "all") null else it
            add(benchsReport.getResultsByMetric(BenchmarkResult.Metric.EXECUTION_TIME,
                    argParser.get<String>("exec")!! == "geomean", filter, executionNormalize).joinToString(";"))
        }
        argParser.getAll<String>("compile-samples")?. let {
            val filter = if (it.first() == "all") null else it
            add(benchsReport.getResultsByMetric(BenchmarkResult.Metric.COMPILE_TIME,
                    argParser.get<String>("compile")!! == "geomean", filter, compileNormalize).joinToString(";"))
        }
        argParser.getAll<String>("codesize-samples")?. let {
            val filter = if (it.first() == "all") null else it
            add(benchsReport.getResultsByMetric(BenchmarkResult.Metric.CODE_SIZE,
                    argParser.get<String>("codesize")!! == "geomean", filter, codesizeNormalize).joinToString(";"))
        }
    }
    println(results.joinToString())
}*/

fun main(args: Array<String>) {

    /*val actions = mapOf( "summary" to Action(
            ::summaryAction,
            ArgParser(
                    listOf(
                        OptionDescriptor(ArgType.Choice(listOf("samples", "geomean")), "exec",
                                description = "Execution time way of calculation", defaultValue = "geomean"),
                        OptionDescriptor(ArgType.String(), "exec-samples",
                                description = "Samples used for execution time metric (value 'all' allows use all samples)",
                                delimiter = ","),
                        OptionDescriptor(ArgType.String(), "exec-normalize",
                                description = "File with golden results which should be used for normalization"),
                        OptionDescriptor(ArgType.Choice(listOf("samples", "geomean")), "compile",
                                description = "Compile time way of calculation", defaultValue = "geomean"),
                        OptionDescriptor(ArgType.String(), "compile-samples",
                                description = "Samples used for compile time metric (value 'all' allows use all samples)",
                                delimiter = ","),
                        OptionDescriptor(ArgType.String(), "compile-normalize",
                                description = "File with golden results which should be used for normalization"),
                        OptionDescriptor(ArgType.Choice(listOf("samples", "geomean")), "codesize",
                                description = "Code size way of calculation", defaultValue = "geomean"),
                        OptionDescriptor(ArgType.String(), "codesize-samples",
                                description = "Samples used for code size metric (value 'all' allows use all samples)",
                                delimiter = ","),
                        OptionDescriptor(ArgType.String(), "codesize-normalize",
                                description = "File with golden results which should be used for normalization"),
                        OptionDescriptor(ArgType.String(), "user", "u", "User access information for authorization")
                ), listOf(ArgDescriptor(ArgType.String(), "mainReport", "Main report for analysis"))
            )
        )
    )*/

    // Parse args.
    val argParser = ArgParser("benchmarksAnalyzer")
    val mainReportName by argParser.argument(ArgType.String(), "mainReport", "Main report for analysis")
    val compareToReportName by argParser.argument(ArgType.String(), "compareToReport", "Report to compare to", isRequired = false)

    val output by argParser.option(ArgType.String(), "output", "o", "Output file")
    val epsValue by argParser.option(ArgType.Double(), "eps", "e", "Meaningful performance changes", "1.0")
    val useShortForm by argParser.option(ArgType.Boolean(), "short", "s", "Show short version of report", "false")
    val renders by argParser.options(ArgType.Choice(listOf("text", "html", "teamcity", "statistics", "metrics")),
        "renders", "r", "Renders for showing information", "text", isMultiple = true)
    val user by argParser.option(ArgType.String(), "user", "u", "User access information for authorization")

    if (argParser.parse(args)) {
        // Read contents of file.
        val mainBenchsReport = getBenchmarkReport(mainReportName!!, user)
        var compareToBenchsReport = compareToReportName?.let {
            getBenchmarkReport(it, user)
        }

        // Generate comparasion report.
        val summaryReport = SummaryBenchmarksReport(mainBenchsReport,
                compareToBenchsReport,
                epsValue!!)

        var outputFile = output
        renders?.forEach {
            Render.getRenderByName(it).print(summaryReport, useShortForm!!, outputFile)
            outputFile = null
        }
    }
}
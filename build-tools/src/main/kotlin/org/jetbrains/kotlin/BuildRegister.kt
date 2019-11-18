/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import org.jetbrains.report.json.*

import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties

typealias performanceAdditionalResult = Triple<String, String, String>
/**
 * Task to produce regressions report and send it to slack. Requires a report with current benchmarks result
 * and path to analyzer tool
 *
 * @property currentBenchmarksReportFile  path to file with becnhmarks result
 * @property analyzer path to analyzer tool
 * @property bundleSize size of build
 * @property onlyBranch register only builds for branch
 */
open class BuildRegister : DefaultTask() {
    @Input
    lateinit var currentBenchmarksReportFile: String
    @Input
    lateinit var analyzer: String

    var onlyBranch: String? = null

    var bundleSize: Int? = null

    val buildInfoTokens: Int = 4
    val additionalInfoTokens: Int = 3
    val compileTimeSamplesNumber: Int = 2
    val buildNumberTokens: Int = 3
    val performanceServer = "https://kotlin-native-perf-summary.labs.jb.gg"

    private fun sendPostRequest(url: String, body: String) : String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return connection.apply {
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            requestMethod = "POST"
            doOutput = true
            val outputWriter = OutputStreamWriter(outputStream)
            outputWriter.write(body)
            outputWriter.flush()
        }.let {
            if (it.responseCode == 200) it.inputStream else it.errorStream
        }.let { streamToRead ->
            BufferedReader(InputStreamReader(streamToRead)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                it.close()
                response.toString()
            }
        }
    }

    fun getAdditionalInfo(analyzer: String, reportFile: String, alwaysExists: Boolean, benchmarkName: String,
                          showedName: String = benchmarkName) :
            performanceAdditionalResult? {
        val output = arrayOf(analyzer, "summary", "--compile", "samples",
                "--compile-samples", benchmarkName, "--codesize-samples", benchmarkName,
                "--codesize-normalize", "artifactory:builds/goldenResults.csv", reportFile)
                .runCommand()


        val buildInfoParts = output.split(',')
        if (buildInfoParts.size != additionalInfoTokens) {
            val message = "Problems with getting summary information using $analyzer and $reportFile. $output"
            if (!alwaysExists) {
                println(message)
                return null
            }
            error(message)
        }
        val (failures, compileTime, codeSize) = buildInfoParts.map { it.trim() }
        val codeSizeInfo = "$showedName-$codeSize"
        val compileTimeInfo = "$showedName-$compileTime"
        return performanceAdditionalResult(failures, compileTimeInfo, codeSizeInfo)
    }

    @TaskAction
    fun run() {
        // Get TeamCity properties.
        val teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE") ?:
            error("Can't load teamcity config!")

        val buildProperties = Properties()
        buildProperties.load(FileInputStream(teamcityConfig))
        val buildId = buildProperties.getProperty("teamcity.build.id")
        val teamCityUser = buildProperties.getProperty("teamcity.auth.userId")
        val teamCityPassword = buildProperties.getProperty("teamcity.auth.password")
        val buildNumber = buildProperties.getProperty("build.number")
        val apiKey = buildProperties.getProperty("artifactory.apikey")

        // Get branch.
        val currentBuild = getBuild("id:$buildId", teamCityUser, teamCityPassword)
        val branch = getBuildProperty(currentBuild,"branchName")

        val target = System.getProperty("os.name").replace("\\s".toRegex(), "")

        // Get summary information.
        val output = arrayOf("$analyzer", "summary", "--exec-samples", "all", "--compile", "samples",
                "--compile-samples", "HelloWorld,Videoplayer", "--codesize-samples", "all",
                "--exec-normalize", "artifactory:builds/goldenResults.csv",
                "--codesize-normalize", "artifactory:builds/goldenResults.csv", "$currentBenchmarksReportFile")
                .runCommand()

        // Postprocess information.
        val buildInfoParts = output.split(',')
        if (buildInfoParts.size != buildInfoTokens) {
            error("Problems with getting summary information using $analyzer and $currentBenchmarksReportFile. $output")
        }

        val (failures, executionTime, compileTime, codeSize) = buildInfoParts.map { it.trim() }
        var failuresNumber = failures.toInt()
        // Add legends.
        val geometricMean = "Geometric Mean-"
        val executionTimeInfo = "$geometricMean$executionTime"
        var codeSizeInfo = "$geometricMean$codeSize"
        val compileTimeSamples = compileTime.split(';')
        if (compileTimeSamples.size != compileTimeSamplesNumber) {
            error("Problems with getting compile time samples value. Expected at least $compileTimeSamplesNumber samples, got ${compileTimeSamples.size}")
        }
        val (helloWorldCompile, videoplayerCompile) = compileTimeSamples
        var compileTimeInfo = "HelloWorld-$helloWorldCompile;Videoplayer-$videoplayerCompile"

        // Collect framework run details.
        if (target == "MacOSX") {
            val frameworkResults = getAdditionalInfo(analyzer, currentBenchmarksReportFile, true,
                    "FrameworkBenchmarksAnalyzer")
            frameworkResults?.let {
                val (_, frameworkCompileTime, frameworkCodeSize) = it
                codeSizeInfo += ";$frameworkCodeSize"
                compileTimeInfo += ";$frameworkCompileTime"
            }

            val spaceResults = getAdditionalInfo(analyzer,
                    "artifactory:$buildNumber:$target:spaceFrameworkReport.json", false,
                    "circlet_iosX64", "SpaceFramework_iosX64")
            spaceResults?.let {
                val (failures, frameworkCompileTime, frameworkCodeSize) = it
                failuresNumber += failures.toInt()
                codeSizeInfo += ";$frameworkCodeSize"
                compileTimeInfo += ";$frameworkCompileTime"
            }
        }

        if (target == "Linux") {
            val coroutinesResults = getAdditionalInfo(analyzer,
                    "artifactory:$buildNumber:$target:externalReport.json", false,
                    "kotlinx.coroutines")
            coroutinesResults?.let {
                val (failures, libraryCompileTime, libraryCodeSize) = it
                failuresNumber += failures.toInt()
                codeSizeInfo += ";$libraryCodeSize"
                compileTimeInfo += ";$libraryCompileTime"
            }
        }

        val buildNumberParts = buildNumber.split("-")
        if (buildNumberParts.size != buildNumberTokens) {
            error("Wrong format of build number $buildNumber.")
        }
        val (_, buildType, _) = buildNumberParts

        // Send post request to register build.
        val requestBody = buildString {
            append("{\"buildId\":\"$buildId\",")
            append("\"teamCityUser\":\"$teamCityUser\",")
            append("\"teamCityPassword\":\"$teamCityPassword\",")
            append("\"artifactoryApiKey\":\"$apiKey\",")
            append("\"target\": \"$target\",")
            append("\"buildType\": \"$buildType\",")
            append("\"failuresNumber\": $failuresNumber,")
            append("\"executionTime\": \"$executionTimeInfo\",")
            append("\"compileTime\": \"$compileTimeInfo\",")
            append("\"codeSize\": \"$codeSizeInfo\",")
            append("\"bundleSize\": ${bundleSize?.let {"\"$bundleSize\""} ?: bundleSize}}")
        }
        if (onlyBranch == null || onlyBranch == branch) {
            println(sendPostRequest("$performanceServer/register", requestBody))
        } else {
            println("Skipping registration. Current branch $branch, need registration for $onlyBranch!")
        }

    }
}
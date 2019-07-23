/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.w3c.xhr.*
import kotlin.js.json
import kotlin.js.Date
import kotlin.js.Promise
import org.jetbrains.report.json.*
import org.jetbrains.influxdb.*
import org.jetbrains.build.Build
import org.jetbrains.analyzer.*
import org.jetbrains.report.*

const val teamCityUrl = "https://buildserver.labs.intellij.net/app/rest"
const val artifactoryUrl = "https://repo.labs.intellij.net/kotlin-native-benchmarks"
const val buildsFileName = "buildsSummary.csv"
const val goldenResultsFileName = "goldenResults.csv"
const val artifactoryBuildsDirectory = "builds"
const val buildsInfoPartsNumber = 11

operator fun <K, V> Map<K, V>?.get(key: K) = this?.get(key)

fun getArtifactoryHeader(artifactoryApiKey: String) = Pair("X-JFrog-Art-Api", artifactoryApiKey)

// Local cache for saving information about builds got from Artifactory.
data class GoldenResult(val benchmarkName: String, val metric: String, val value: Double)
data class GoldenResultsInfo(val apiKey: String, val goldenResults: Array<GoldenResult>)

// Build information provided from request.
data class TCBuildInfo(val buildNumber: String, val branch: String, val startTime: String,
                       val finishTime: String)

data class BuildRegister(val buildId: String, val teamCityUser: String, val teamCityPassword: String,
                    val bundleSize: String?) {
    companion object {
        fun create(json: String): BuildRegister {
            val requestDetails = JSON.parse<BuildRegister>(json)
            // Parse method doesn't create real instance with all methods. So create it by hands.
            return BuildRegister(requestDetails.buildId, requestDetails.teamCityUser, requestDetails.teamCityPassword,
                    requestDetails.bundleSize)
        }
    }

    private val teamCityBuildUrl: String by lazy { "$teamCityUrl/builds/id:$buildId" }

    val changesListUrl: String by lazy {
        "$teamCityUrl/changes/?locator=build:id:$buildId"
    }

    private val fileWithResults = "nativeReport.json"

    val teamCityArtifactsUrl: String by lazy { "$teamCityUrl/builds/id:$buildId/artifacts/content/$fileWithResults" }

    fun sendTeamCityRequest(url: String, json: Boolean = false) = sendRequest(RequestMethod.GET, url, teamCityUser,
            teamCityPassword, json)

    private fun format(timeValue: Int): String =
            if (timeValue < 10) "0$timeValue" else "$timeValue"

    fun getBuildInformation(): Promise<TCBuildInfo> {
        return Promise.all(arrayOf(sendTeamCityRequest("$teamCityBuildUrl/number"),
                sendTeamCityRequest("$teamCityBuildUrl/branchName"),
                sendTeamCityRequest("$teamCityBuildUrl/startDate"))).then { results ->
            val (buildNumber, branch, startTime) = results
            val currentTime = Date()
            val timeZone = currentTime.getTimezoneOffset() / -60    // Convert to hours.
            // Get finish time as current time, because buid on TeamCity isn't finished.
            val finishTime = "${format(currentTime.getUTCFullYear())}" +
                    "${format(currentTime.getUTCMonth() + 1)}" +
                    "${format(currentTime.getUTCDate())}" +
                    "T${format(currentTime.getUTCHours())}" +
                    "${format(currentTime.getUTCMinutes())}" +
                    "${format(currentTime.getUTCSeconds())}" +
                    "${if (timeZone > 0) "+" else "-"}${format(timeZone)}${format(0)}"
            TCBuildInfo(buildNumber, branch, startTime, finishTime)
        }
    }
}

fun checkBuildType(currentType: String, targetType: String): Boolean {
    val releasesBuildTypes = listOf("release", "eap", "rc1", "rc2")
    return if (targetType == "release") currentType in releasesBuildTypes else currentType == targetType
}

// Get builds numbers in right order.
fun orderedBuildNumbers(buildNumbers: List<String>, type: String) = buildNumbers.filter { checkBuildType(it, type) }
        .sortedWith(compareBy ( { it.substringBefore(".").toInt() },
                { it.substringAfter(".").substringBefore("-").toDouble() },
                { it.substringAfterLast("-").toInt() }))

// Get builds numbers from DB which have needed parameters.
fun getBuildsForParameters(branch: String?, target: dynamic): Promise<List<String>> {
    val measurement = BenchmarkMeasurement()
    val selectExpr = branch?.let {
        (measurement.tag("environment.machine.os") eq target) and
                /*(measurement.field("build.number") match ".+-${request.params.type}-.+") and*/
                (measurement.field("build.branch") eq FieldType.InfluxString(branch))
    } ?: (measurement.tag("environment.machine.os") eq target)

    return InfluxDBConnector.select(measurement.distinct("build.number"),
            measurement.field("build.number")
                    .select(selectExpr)).then { dbResponse ->
        dbResponse.toString().replace("\\[|\\]| ".toRegex(), "").split(",")
    }
    return tokens
}

// Routing of requests to current server.
fun router() {
    val express = require("express")
    val router = express.Router()

    // Register build on Artifactory.
    router.post("/register", { request, response ->
        val maxCommitsNumber = 5
        val register = BuildRegister.create(JSON.stringify(request.body))

        // Get information from TeamCity.
        register.getBuildInformation().then { buildInfo ->
            register.sendTeamCityRequest(register.changesListUrl, true).then { changes ->

                val commitsList = CommitsList(JsonTreeParser.parse(changes))
                // Get artifact.
                register.sendTeamCityRequest(register.teamCityArtifactsUrl).then { resultsContent ->
                    val results = BenchmarkMeasurement.create(JsonTreeParser.parse(resultsContent),
                            BuildInfo(buildInfo.buildNumber, buildInfo.startTime, buildInfo.finishTime,
                                    commitsList, buildInfo.branch)).toMutableList()
                    if (results.isNotEmpty() && register.bundleSize != null) {
                        // Add bundle size.
                        val bundleSizeBenchmark = results[0].copy()
                        bundleSizeBenchmark.benchmarkName = "Kotlin/Native"
                        bundleSizeBenchmark.benchmarkStatus = FieldType.InfluxString("PASSED")
                        bundleSizeBenchmark.benchmarkScore = FieldType.InfluxFloat(register.bundleSize.toDouble())
                        bundleSizeBenchmark.benchmarkMetric = "BUNDLE_SIZE"
                        bundleSizeBenchmark.benchmarkRuntime = FieldType.InfluxFloat(0.0)
                        bundleSizeBenchmark.benchmarkRepeat = FieldType.InfluxInt(1)
                        bundleSizeBenchmark.benchmarkWarmup = FieldType.InfluxInt(0)
                        results.add(bundleSizeBenchmark)
                    }
                    // Save results in database.
                    Promise.all(InfluxDBConnector.insert(results)).then { _ ->
                        response.sendStatus(200)
                    }.catch {
                        response.sendStatus(400)
                    }
                }
            }
        }
    })

    // Register golden results to normalize on Artifactory.
    router.post("/registerGolden", { request, response ->
        val goldenResultsInfo = JSON.parse<GoldenResultsInfo>(JSON.stringify(request.body))
        val resultPoints = goldenResultsInfo.goldenResults.map {
            GoldenResultMeasurement(it.benchmarkName, it.metric, it.value)
        }
        Promise.all(InfluxDBConnector.insert(resultPoints)).then { _ ->
            response.sendStatus(200)
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get builds description with additional information.
    router.get("/buildsDesc/:target/:type", { request, response ->
        val measurement = BenchmarkMeasurement()
        val target = request.params.target.toString().replace('_', ' ').asDynamic()
        var branch: String? = null
        if (request.query != undefined) {
            if (request.query.branch != undefined) {
                branch = request.query.branch
            }
        }
        getBuildsForParameters(branch, target).then { results ->
            val filteredBuildNumbers = orderedBuildNumbers((results as List<String>), "${request.params.type}")
            val responseLists = filteredBuildNumbers.map {
                // Select needed measurements.
                measurement.select(measurement.all(), (measurement.tag("environment.machine.os") eq target) and
                        (measurement.field("build.number") eq FieldType.InfluxString(it))).then { dbResponse ->
                    val measurements = (dbResponse as List<BenchmarkMeasurement>)
                    val report = measurements.toReport()
                    val failuresNumber = report?.let { report ->
                        val summaryReport = SummaryBenchmarksReport(report)
                        summaryReport.failedBenchmarks.size
                    } ?: 0
                    val buildDescription = measurements.firstOrNull()?.let {
                        Build(it.buildNumber!!.value, it.buildStartTime!!.value, it.buildEndTime!!.value, it.buildBranch!!.value,
                                it.buildCommits!!.value, request.params.type, failuresNumber)
                    }
                    buildDescription
                }.catch {
                    response.sendStatus(400)
                }
            }

            Promise.all(responseLists.toTypedArray()).then { resultList ->
                response.json(resultList)
            }.catch {
                response.sendStatus(400)
            }
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get values of current metric.
    router.get("/metricValue/:target/:type/:metric", { request, response ->
        val measurement = BenchmarkMeasurement()
        val metric = request.params.metric
        val target = request.params.target.toString().replace('_', ' ').asDynamic()
        var samples: List<String>? = null
        var agregation = "geomean"
        var normalize = false
        var branch: String? = null

        // Parse parameters from request if it exists.
        if (request.query != undefined) {
            if (request.query.samples != undefined) {
                samples = request.query.samples.toString().split(",").map { it.trim() }
            }
            if (request.query.agr != undefined) {
                agregation = request.query.agr.toString()
            }
            if (request.query.normalize != undefined) {
                normalize = true
            }
            if (request.query.branch != undefined) {
                branch = request.query.branch
            }
        }

        // Get golden results to normalize data.
        val golden = GoldenResultMeasurement()
        val goldenResults = golden.select(golden.all()).then { results ->
            val parsedNormalizeResults = mutableMapOf<String, MutableMap<String, Double>>()
            (results as List<GoldenResultMeasurement>).forEach {
                parsedNormalizeResults.getOrPut(it.benchmarkName!!,
                        { mutableMapOf<String, Double>() })[it.benchmarkMetric!!] = it.benchmarkScore!!.value
            }
            parsedNormalizeResults
        }.catch {
            response.sendStatus(400)
        }

        // Get builds numbers.
        val buildsNumbers = getBuildsForParameters(branch, target)

        Promise.all(arrayOf(buildsNumbers, goldenResults)).then { results ->
            val (buildsNumbers, goldenResults) = results
            val filteredBuildNumbers = orderedBuildNumbers((buildsNumbers as List<String>), "${request.params.type}")
            val responseLists = filteredBuildNumbers.map {
                // Get points for this build.
                measurement.select(measurement.all(), (measurement.tag("environment.machine.os") eq target) and
                        (measurement.field("build.number") eq FieldType.InfluxString(it))).then { dbResponse ->
                    val report = (dbResponse as List<BenchmarkMeasurement>).toReport()
                    report?.let { report ->
                        val dataForNormalization = if (normalize)
                            { goldenResults as Map<String, Map<String, Double>> } else null
                        val summaryReport = SummaryBenchmarksReport(report)
                        val result = if (samples != null && samples.contains("all")) {
                            // Case of quering for all benchmarks and for some other separately.
                            val changedSamples = samples.toMutableList()
                            changedSamples.remove("all")
                            summaryReport.getResultsByMetric(
                                    BenchmarkResult.metricFromString(metric) ?: BenchmarkResult.Metric.EXECUTION_TIME,
                                    agregation == "geomean", null, dataForNormalization) + if (changedSamples.isNotEmpty()) {
                                summaryReport.getResultsByMetric(
                                    BenchmarkResult.metricFromString(metric) ?: BenchmarkResult.Metric.EXECUTION_TIME,
                                    agregation == "geomean", changedSamples, dataForNormalization)
                            } else listOf()
                        } else summaryReport.getResultsByMetric(
                                BenchmarkResult.metricFromString(metric) ?: BenchmarkResult.Metric.EXECUTION_TIME,
                                agregation == "geomean", samples, dataForNormalization)
                        it to result
                    }
                }.catch {
                    response.sendStatus(400)
                }
            }

            Promise.all(responseLists.toTypedArray()).then { resultList ->
                val results = resultList as Array<Pair<String, List<Double?>>>
                val unzippedLists = mutableListOf<Collection<Any?>>()
                if (results.isNotEmpty()) {
                    // Get list of all buildNumbers.
                    unzippedLists.add(results.map { it.first })
                    for (i in 0 until results[0].second.size) {
                        unzippedLists.add(results.map { it.second[i] }.toList())
                    }
                }
                response.json(unzippedLists)
            }.catch {
                response.sendStatus(400)
            }
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get branches for [target].
    router.get("/branches/:target", { request, response ->
        val measurement = BenchmarkMeasurement()
        val target = request.params.target.toString().replace('_', ' ').asDynamic()
        InfluxDBConnector.select(measurement.distinct("build.branch"),
                measurement.field("build.branch")
                        .select(measurement.tag("environment.machine.os") eq target)).then { dbResponse ->
            response.json(dbResponse)
        }
    })

    // Get build numbers for [target].
    router.get("/buildsNumbers/:target", { request, response ->
        val measurement = BenchmarkMeasurement()
        val target = request.params.target.toString().replace('_', ' ').asDynamic()

        InfluxDBConnector.select(measurement.distinct("build.number"),
                measurement.field("build.number")
                        .select(measurement.tag("environment.machine.os") eq target)).then { dbResponse ->
            response.json(dbResponse)
        }
    })

    router.get("/delete/:target", { request, response ->
        TODO()
    })

    // Main page.
    router.get("/", { _, response ->
        response.render("index")
    })

    return router
}
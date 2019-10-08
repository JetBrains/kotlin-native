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
import kotlin.coroutines.*

const val teamCityUrl = "https://buildserver.labs.intellij.net/app/rest"
const val artifactoryUrl = "https://repo.labs.intellij.net/kotlin-native-benchmarks"

operator fun <K, V> Map<K, V>?.get(key: K) = this?.get(key)

fun getArtifactoryHeader(artifactoryApiKey: String) = Pair("X-JFrog-Art-Api", artifactoryApiKey)

// Local cache for saving information about builds got from Artifactory.
data class GoldenResult(val benchmarkName: String, val metric: String, val value: Double)
data class GoldenResultsInfo(val apiKey: String, val goldenResults: Array<GoldenResult>)

// Build information provided from request.
data class TCBuildInfo(val buildNumber: String, val branch: String, val startTime: String,
                       val finishTime: String)

data class BuildRegister(val buildId: String, val teamCityUser: String, val teamCityPassword: String,
                    val bundleSize: String?, val fileWithResult: String) {
    companion object {
        fun create(json: String): BuildRegister {
            val requestDetails = JSON.parse<BuildRegister>(json)
            // Parse method doesn't create real instance with all methods. So create it by hands.
            return BuildRegister(requestDetails.buildId, requestDetails.teamCityUser, requestDetails.teamCityPassword,
                    requestDetails.bundleSize, requestDetails.fileWithResult)
        }
    }

    private val teamCityBuildUrl: String by lazy { "$teamCityUrl/builds/id:$buildId" }

    val changesListUrl: String by lazy {
        "$teamCityUrl/changes/?locator=build:id:$buildId"
    }

    val teamCityArtifactsUrl: String by lazy { "$teamCityUrl/builds/id:$buildId/artifacts/content/$fileWithResult" }

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
fun getBuildsForParameters(branch: String?, target: dynamic, connector: InfluxDBConnector): Promise<List<String>> {
    val measurement = BenchmarkMeasurement(connector)
    val selectExpr = branch?.let {
        (measurement.tag("environment.machine.os") eq target) and
                /*(measurement.field("build.number") match ".+-${request.params.type}-.+") and*/
                (measurement.field("build.branch") eq FieldType.InfluxString(branch))
    } ?: (measurement.tag("environment.machine.os") eq target)

    return connector.select(measurement.distinct("build.number") from measurement.field("build.number")
                    .select(selectExpr)).then { dbResponse ->
        dbResponse.toString().replace("\\[|\\]| ".toRegex(), "").split(",")
    }
}

// Routing of requests to current server.
fun router() {
    val express = require("express")
    val router = express.Router()
    val connector = InfluxDBConnector("https://biff-9a16f218.influxcloud.net", "kotlin_native",
            user = "elena_lepikina", password = "KMFBsyhrae6gLrCZ4Tmq")

    // Register build on Artifactory.
    router.post("/register", { request, response ->
        val register = BuildRegister.create(JSON.stringify(request.body))

        // Get information from TeamCity.
        register.getBuildInformation().then { buildInfo ->
            register.sendTeamCityRequest(register.changesListUrl, true).then { changes ->

                val commitsList = CommitsList(JsonTreeParser.parse(changes))
                // Get artifact.
                register.sendTeamCityRequest(register.teamCityArtifactsUrl).then { resultsContent ->
                    val results = BenchmarkMeasurement.create(JsonTreeParser.parse(resultsContent), connector,
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
                    Promise.all(connector.insert(results)).then { _ ->
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
            GoldenResultMeasurement(it.benchmarkName, it.metric, it.value, connector)
        }
        Promise.all(connector.insert(resultPoints)).then { _ ->
            response.sendStatus(200)
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get builds description with additional information.
    router.get("/buildsDesc/:target/:type", { request, response ->
        val measurement = BenchmarkMeasurement(connector)
        val target = request.params.target.toString().replace('_', ' ')
        var branch: String? = null
        if (request.query != undefined) {
            if (request.query.branch != undefined) {
                branch = request.query.branch
            }
        }
        getBuildsForParameters(branch, target, connector).then { results ->
            val filteredBuildNumbers = orderedBuildNumbers(results, "${request.params.type}")
            val responseLists = filteredBuildNumbers.map {
                // Select needed measurements.
                measurement.select(measurement.all() where ((measurement.tag("environment.machine.os") eq target)  and
                        (measurement.field("build.number") eq FieldType.InfluxString(it)) as Condition<String>)).then { measurements ->
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
        val measurement = BenchmarkMeasurement(connector)
        val metric = request.params.metric
        val target = request.params.target.toString().replace('_', ' ')
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
        val golden = GoldenResultMeasurement(connector = connector)
        val goldenResults = golden.select(golden.all()).then { results ->
            val parsedNormalizeResults = mutableMapOf<String, MutableMap<String, Double>>()
            results.forEach {
                parsedNormalizeResults.getOrPut(it.benchmarkName!!,
                        { mutableMapOf<String, Double>() })[it.benchmarkMetric!!] = it.benchmarkScore!!.value
            }
            parsedNormalizeResults
        }.catch {
            response.sendStatus(400)
        }

        // Get builds numbers.
        val buildsNumbers = getBuildsForParameters(branch, target, connector)

        Promise.all(arrayOf(buildsNumbers, goldenResults)).then { results ->
            val (buildsNumbers, goldenResults) = results
            val filteredBuildNumbers = orderedBuildNumbers(buildsNumbers, "${request.params.type}")
            val responseLists = filteredBuildNumbers.map {
                // Get points for this build.
                measurement.select(measurement.all() where ((measurement.tag("environment.machine.os") eq target) and
                        (measurement.field("build.number") eq it))).then { dbResponse ->
                    val report = dbResponse.toReport()
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
        val measurement = BenchmarkMeasurement(connector)
        val target = request.params.target.toString().replace('_', ' ')
        connector.select(measurement.distinct("build.branch") from measurement.field("build.branch")
                        .select( measurement.tag("environment.machine.os") eq target)).then { dbResponse ->
            response.json(dbResponse)
        }
    })

    // Get build numbers for [target].
    router.get("/buildsNumbers/:target", { request, response ->
        val measurement = BenchmarkMeasurement(connector)
        val target = request.params.target.toString().replace('_', ' ')

        connector.select(measurement.distinct("build.number") from measurement.field("build.number")
                        .select(measurement.tag("environment.machine.os") eq target)).then { dbResponse ->
            response.json(dbResponse)
        }
    })

    router.get("/convert/:target", { request, response ->
        val target = request.params.target.toString()
        var buildNumber: String? = null
        if (request.query != undefined) {
            if (request.query.buildNumber != undefined) {
                buildNumber = request.query.buildNumber
            }
        }
        getBuildsInfoFromBintray(target).then { buildInfo ->
            launch {
                val buildsDescription = buildInfo.lines().drop(1)
                var shouldConvert = buildNumber?.let { false } ?: true
                buildsDescription.forEach {
                    if (!it.isEmpty()) {
                        val currentBuildNumber = it.substringBefore(',')
                        if (!"\\d+(\\.\\d+)+-\\w+-\\d+".toRegex().matches(currentBuildNumber)) {
                            error("Build number $currentBuildNumber differs from expected format. File with data for " +
                                    "target $target could be corrupted.")
                        }
                        if (!shouldConvert && buildNumber != null && buildNumber == currentBuildNumber) {
                            shouldConvert = true
                        }
                        if (shouldConvert) {
                            // Save data from Bintray into database.
                            val bintrayUrl = "https://dl.bintray.com/content/lepilkinaelena/KotlinNativePerformance"
                            val fileName = "nativeReport.json"
                            val accessFileUrl = "$bintrayUrl/$target/$currentBuildNumber/$fileName"
                            val infoParts = it.split(", ")
                            try {
                                val jsonReport = sendRequest(RequestMethod.GET, accessFileUrl).await()
                                val results = BenchmarkMeasurement.create(JsonTreeParser.parse(jsonReport), connector,
                                        BuildInfo(currentBuildNumber, infoParts[1], infoParts[2],
                                                CommitsList.parse(infoParts[4]), infoParts[3])).toMutableList()

                                val bundleSize = if (infoParts[10] != "-") infoParts[10] else null
                                // Save bundle size if exists.
                                if (results.isNotEmpty() && bundleSize != null) {
                                    // Add bundle size.
                                    val bundleSizeBenchmark = results[0].copy()
                                    bundleSizeBenchmark.benchmarkName = "Kotlin/Native"
                                    bundleSizeBenchmark.benchmarkStatus = FieldType.InfluxString("PASSED")
                                    bundleSizeBenchmark.benchmarkScore = FieldType.InfluxFloat(bundleSize.toDouble())
                                    bundleSizeBenchmark.benchmarkMetric = "BUNDLE_SIZE"
                                    bundleSizeBenchmark.benchmarkRuntime = FieldType.InfluxFloat(0.0)
                                    bundleSizeBenchmark.benchmarkRepeat = FieldType.InfluxInt(1)
                                    bundleSizeBenchmark.benchmarkWarmup = FieldType.InfluxInt(0)
                                    results.add(bundleSizeBenchmark)
                                }

                                // Change compiler flags.
                                results.forEach {
                                    val options = getOptsForBenchmark(it.benchmarkName!!)
                                    it.kotlinBackendFlags = options
                                }
                                // Save results in database.
                                Promise.all(connector.insert(results)).then { _ ->
                                    println("Success insert")
                                }.catch { errorResponse ->
                                    println("Failed to insert data for build")
                                    println(errorResponse)
                                }
                            } catch (e: Exception) {
                                println(e.message)
                            }
                        }
                    }
                }
            }
        }.catch {
            response.sendStatus(400)
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

fun getOptsForBenchmark(benchmarkName: String): List<String> {
    val optimizedBenchmarks = listOf("Cinterop", "Numerical", "ObjCInterop", "Ring", "swiftInterop")
    val debuggableBenchmark = listOf("FrameworkBenchmarksAnalyzer", "HelloWorld", "Videoplayer")

    optimizedBenchmarks.forEach {
        if (benchmarkName.contains(it, true)) return listOf("-opt")
    }

    debuggableBenchmark.forEach {
        if (benchmarkName.contains(it, true)) return listOf("-g")
    }

    return listOf<String>()
}

fun getBuildsInfoFromBintray(target: String): Promise<String> {
    val downloadBintrayUrl = "https://dl.bintray.com/content/lepilkinaelena/KotlinNativePerformance"
    val buildsFileName = "buildsSummary.csv"
    return sendRequest(RequestMethod.GET, "$downloadBintrayUrl/$target/$buildsFileName")
}

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { cont ->
    then({ cont.resume(it) }, { cont.resumeWithException(it) })
}

fun launch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) =
    block.startCoroutine(Continuation(context) { result ->
        result.onFailure { exception ->
            throw exception
        }
    })
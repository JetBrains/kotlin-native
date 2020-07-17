/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.w3c.xhr.*
import kotlin.js.json
import kotlin.js.Date
import kotlin.js.Promise
import org.jetbrains.report.json.*
import org.jetbrains.elastic.*
import org.jetbrains.build.Build
import org.jetbrains.analyzer.*
import org.jetbrains.report.*
import kotlin.coroutines.*

const val teamCityUrl = "https://buildserver.labs.intellij.net/app/rest"
const val artifactoryUrl = "https://repo.labs.intellij.net/kotlin-native-benchmarks"

operator fun <K, V> Map<K, V>?.get(key: K) = this?.get(key)

fun getArtifactoryHeader(artifactoryApiKey: String) = Pair("X-JFrog-Art-Api", artifactoryApiKey)

fun convertToNewFormat(data: JsonObject): List<Any> {
    val env = Environment.create(data.getRequiredField("env"))
    val benchmarksObj = data.getRequiredField("benchmarks")
    val compilerDescription = data.getRequiredField("kotlin")
    val compiler = Compiler.create(compilerDescription)
    val backend = (compilerDescription as JsonObject).getRequiredField("backend")
    val flagsArray = (backend as JsonObject).getOptionalField("flags")
    var flags: List<String> = emptyList()
    if (flagsArray != null && flagsArray is JsonArray) {
        flags = flagsArray.jsonArray.map { (it as JsonLiteral).unquoted() }
    }
    val benchmarksList = BenchmarksSet.parseBenchmarksArray(benchmarksObj)

    return listOf(env, compiler, benchmarksList, flags)
}

fun convert(json: String, buildNumber: String): BenchmarksReport {
    val data = JsonTreeParser.parse(json)
    val reports = if (data is JsonArray) {
        data.map { convertToNewFormat(data as JsonObject) }
    } else listOf(convertToNewFormat(data as JsonObject))
    val knownFlags = mapOf(
            "Cinterop" to listOf("-opt"),
            "FrameworkBenchmarksAnalyzer" to listOf("-g"),
            "HelloWorld" to listOf("-g"),
            "Numerical" to listOf("-opt"),
            "ObjCInterop" to listOf("-opt"),
            "Ring" to listOf("-opt"),
            "swiftInterop" to listOf("-opt"),
            "Videoplayer" to listOf("-g")
    )

    val fullReport = reports.map { elements ->
        val benchmarks = (elements[2] as List<BenchmarkResult>).groupBy { it.name.substringBefore('.').substringBefore(':') }
        val parsedFlags = elements[3] as List<String>
        val benchmarksSets = benchmarks.map { (setName, results) ->
            val flags = if (parsedFlags[0] == "-opt") knownFlags[setName]!! else parsedFlags
            BenchmarksSet(BenchmarksSet.BenchmarksSetInfo(setName, flags), results)
        }
        BenchmarksReport(elements[0] as Environment, benchmarksSets, elements[1] as Compiler)
    }.reduce{ acc, it -> acc + it }
    fullReport.buildNumber = buildNumber
    return fullReport
}

// Local cache for saving information about builds got from Artifactory.
data class GoldenResult(val benchmarkName: String, val metric: String, val value: Double)
data class GoldenResultsInfo(val goldenResults: Array<GoldenResult>)

fun GoldenResultsInfo.toBenchmarksReport(): BenchmarksReport {
    val benchmarksSamples = goldenResults.map{ BenchmarkResult (it.benchmarkName, BenchmarkResult.Status.PASSED,
            it.value, BenchmarkResult.metricFromString(it.metric)!!, it.value, 1, 0) }
    val compiler = Compiler(Compiler.Backend(Compiler.BackendType.NATIVE, "golden"), "golden")
    val environment = Environment(Environment.Machine("golden", "golden"), Environment.JDKInstance("golden", "golden"))
    return BenchmarksReport(environment,
            listOf(BenchmarksSet(BenchmarksSet.BenchmarksSetInfo("golden", listOf()), benchmarksSamples)), compiler)
}

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
fun orderedBuildNumbers(buildNumbers: List<String>, type: String) = buildNumbers.filter { checkBuildType(it.substringAfter("-").substringBeforeLast("-"), type) }
        .sortedWith(compareBy ( { it.substringBefore(".").toInt() },
                { it.substringAfter(".").substringBefore("-").toDouble() },
                { it.substringAfterLast("-").toInt() }))

// Get builds numbers from DB which have needed parameters.
fun getBaseBuildInfo(target: String, benchmarksIndex: ElasticSearchIndex): Promise<Map<String, String>> {
    // Search in benchmarks index.
    val queryDescription = """
            {
                "_source": ["buildNumber", "_id"],
                "size": 1000,
                "query": {
                    "nested": {
                        "path" : "env",
                        "query" : {
                            "nested" : {
                                "path" : "env.machine",
                                "query" : {
                                    "bool": {"must": [{"match": { "env.machine.os": "$target" }}]}
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

    return benchmarksIndex.search(queryDescription, listOf("hits.hits._id", "hits.hits._source")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        dbResponse.getObjectOrNull("hits")?.getArrayOrNull("hits")?.
                map { (it as JsonObject).getPrimitive("_id").content to
                        (it as JsonObject).getObject("_source").getPrimitive("buildNumber").content }?.
                        toMap()
                ?: error("Wrong response:\n$responseString")
    }
}

fun getBuildsInfo(type: String?, branch: String?, documentIds: Iterable<String>, buildInfoIndex: ElasticSearchIndex): Promise<List<BuildInfo>> {
    val queryDescription = """
            {   "size": 1000,
                "query": {
                    "bool": {
                        "must": [ 
                            { "terms" : { "_id" : [${documentIds.map{ "\"$it\""}.joinToString()}] } }
                            ${type?.let{ """,
                            { "regexp": { "buildNumber": { "value": "${if (it == "release") ".*eap.*|.*release.*|.*rc.*" else ".*dev.*"}" } } }
                            """}?:""} 
                            ${branch?.let{ """,
                            {"match": { "branch": "$it" }}
                            """}?:""}
                        ]
                    }
                }
            }
        """.trimIndent()
    return buildInfoIndex.search(queryDescription, listOf("hits.hits._source")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        dbResponse.getObjectOrNull("hits")?.getArrayOrNull("hits")?.
                map { BuildInfo.create((it as JsonObject).getObject("_source")) }
                ?: error("Wrong response:\n$responseString")
    }
}

fun getGeometricMean(metricName: String, benchmarksIndex: ElasticSearchIndex, target: String? = null,
                     buildNumbers: Iterable<String>? = null): Promise<List<Pair<String, List<Double>>>> {
    val queryDescription = """
{
    "_source": false,
    ${target?.let {"""
    "query": {
        "bool": {
            "must": [
                {
                    "nested": {
                        "path" : "env",
                        "query" : {
                            "nested" : {
                                "path" : "env.machine",
                                "query" : {
                                    "bool": {"must": [{"match": { "env.machine.os": "$target" }}]}
                                }
                            }
                        }
                    }
                }
            ]
        }
    }, """} ?: ""}
    ${buildNumbers?.let{"""
    "aggs" : {
        "builds": {
            "filters" : { 
                "filters": { 
                    ${buildNumbers.map {"\"$it\": { \"match\" : { \"buildNumber\" : \"$it\" }}"}.joinToString(",\n")}
                }
            },"""} ?: ""}
            "aggs" : {
                "benchs" : {
                    "nested" : {
                        "path" : "benchmarksSets"
                    },
                    "aggs" : {
                        "metric_build" : {
                            "nested" : {
                                "path" : "benchmarksSets.benchmarks"
                            },
                            "aggs" : {
                                "metric_samples": {
                                    "filters" : { 
                                        "filters": { "samples": { "match": { "benchmarksSets.benchmarks.metric": "$metricName" } } }
                                    },
                                    "aggs" : {
                                        "sum_log_x": {
                                            "sum": {
                                                "field" : "benchmarksSets.benchmarks.score",
                                                "script" : {
                                                    "source": "Math.log(_value)"
                                                }
                                            }
                                        },
                                        "geom_mean": {
                                            "bucket_script": {
                                                "buckets_path": {
                                                    "sum_log_x": "sum_log_x",
                                                    "x_cnt": "_count"
                                                },
                                                "script": "Math.exp(params.sum_log_x/params.x_cnt)"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
               ${buildNumbers?.let{""" }
            }"""} ?: ""}
        }
    }
}
"""
    return benchmarksIndex.search(queryDescription, listOf("aggregations")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        println("AAAAAA")
        var aggregations = dbResponse.getObjectOrNull("aggregations") ?: error("Wrong response:\n$responseString")
        println("BBBBBB")
        buildNumbers?.let {
            val buckets = aggregations.getObjectOrNull("builds")?.getObjectOrNull("buckets")
                    ?: error("Wrong response:\n$responseString")
            buildNumbers.map {
                it to listOf(buckets.getObject(it).getObject("benchs").getObject("metric_build").
                        getObject("metric_samples").getObject("buckets").getObject("samples").
                        getObject("geom_mean").getPrimitive("value").double)
            }
        } ?: listOf("golden" to listOf(aggregations.getObject("benchs").getObject("metric_build").getObject("metric_samples").
                getObject("buckets").getObject("samples").getObject("geom_mean").getPrimitive("value").double))
    }
}

fun distinctValues(field: String, index: ElasticSearchIndex): Promise<List<String>> {
    val queryDescription = """
            {
              "aggs": {
                    "unique": {"terms": {"field": "$field", "size": 1000}}
                }
            }
        """.trimIndent()
    return index.search(queryDescription, listOf("aggregations.unique.buckets")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        dbResponse.getObjectOrNull("aggregations")?.getObjectOrNull("unique")?.
                getArrayOrNull("buckets")?.map { (it as JsonObject).getPrimitiveOrNull("key")?.content }?.filterNotNull()
                ?: error("Wrong response:\n$responseString")
    }
}

// Routing of requests to current server.
fun router() {
    val express = require("express")
    val router = express.Router()
    val process = require("child_process")
    val fs = require("fs")
    val connector = ElasticSearchConnector("http://localhost")
    val benchmarksIndex = BenchmarksIndex(connector)
    val goldenIndex = GoldenResultsIndex(connector)
    val buildInfoIndex = BuildInfoIndex(connector)
    val buildsCache = HashMap<Triple<String, String?, String?>, BuildInfo>()

    router.get("/createMapping") { request, response ->
        benchmarksIndex.createMapping().then { _ ->
            response.sendStatus(200)
        }.catch { _ ->
            response.sendStatus(400)
        }
        buildInfoIndex.createMapping().then { _ ->
            response.sendStatus(200)
        }.catch { _ ->
            response.sendStatus(400)
        }
    }
    // Register build on Artifactory.
    router.post("/register") { request, response ->
        val register = BuildRegister.create(JSON.stringify(request.body))

        // Get information from TeamCity.
        register.getBuildInformation().then { buildInfo ->
            register.sendTeamCityRequest(register.changesListUrl, true).then { changes ->
                val commitsList = CommitsList(JsonTreeParser.parse(changes))
                // Get artifact.
                register.sendTeamCityRequest(register.teamCityArtifactsUrl).then { resultsContent ->
                    val buildInfoInstance = BuildInfo(buildInfo.buildNumber, buildInfo.startTime, buildInfo.finishTime,
                                    commitsList, buildInfo.branch)
                    var benchmarksReport = BenchmarksReport.create(JsonTreeParser.parse(resultsContent))
                    benchmarksReport.buildNumber = buildInfo.buildNumber
                    if (register.bundleSize != null) {
                        // Add bundle size.
                        val bundleSizeBenchmark = BenchmarkResult("Kotlin/Native", BenchmarkResult.Status.PASSED, register.bundleSize.toDouble(),
                                BenchmarkResult.Metric.BUNDLE_SIZE, 0.0, 1, 0)
                        val bundleSet = BenchmarksSet(BenchmarksSet.BenchmarksSetInfo("Kotlin/Native", listOf()),
                                listOf(bundleSizeBenchmark))
                        benchmarksReport = BenchmarksReport(benchmarksReport.env,
                                benchmarksReport.benchmarksSets + bundleSet, benchmarksReport.compiler)
                    }
                    val summaryReport = SummaryBenchmarksReport(benchmarksReport)
                    // Save results in database.
                    benchmarksIndex.insert(summaryReport.toBenchmarksReport()).then { _ ->
                        buildInfoIndex.insert(buildInfoInstance).then { _ ->
                            response.sendStatus(200)
                        }.catch {
                            response.sendStatus(400)
                        }
                    }.catch {
                        response.sendStatus(400)
                    }
                }
            }
        }
    }

    // Register golden results to normalize on Artifactory.
    router.post("/registerGolden", { request, response ->
        val goldenResultsInfo: GoldenResultsInfo = JSON.parse<GoldenResultsInfo>(JSON.stringify(request.body))
        val goldenReport = goldenResultsInfo.toBenchmarksReport()
        goldenIndex.insert(goldenReport).then { _ ->
            response.sendStatus(200)
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get builds description with additional information.
    router.get("/buildsDesc/:target", { request, response ->
        val target = request.params.target.toString().replace('_', ' ')

        var branch: String? = null
        var type: String? = null
        if (request.query != undefined) {
            if (request.query.branch != undefined) {
                branch = request.query.branch
            }
            if (request.query.type != undefined) {
                type = request.query.type
            }
        }

        getBaseBuildInfo(target, benchmarksIndex).then { idsMap ->
            getBuildsInfo(type, branch, idsMap.keys, buildInfoIndex).then { buildsInfo ->
                response.json(buildsInfo)
            }.catch {
                response.sendStatus(400)
            }
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get values of current metric.
    router.get("/metricValue/:target/:metric", { request, response ->
        val metric = request.params.metric
        val target = request.params.target.toString().replace('_', ' ')
        var samples: List<String>? = null
        var aggregation = "geomean"
        var normalize = false
        var branch: String? = null
        var type: String? = null

        // Parse parameters from request if it exists.
        if (request.query != undefined) {
            if (request.query.samples != undefined) {
                samples = request.query.samples.toString().split(",").map { it.trim() }
            }
            if (request.query.agr != undefined) {
                aggregation = request.query.agr.toString()
            }
            if (request.query.normalize != undefined) {
                normalize = true
            }
            if (request.query.branch != undefined) {
                branch = request.query.branch
            }
            if (request.query.type != undefined) {
                type = request.query.type
            }
        }

        getBaseBuildInfo(target, benchmarksIndex).then { idsMap ->
            getBuildsInfo(type, branch, idsMap.keys, buildInfoIndex).then { buildsInfo ->
                val buildNumbers = buildsInfo.map { it.buildNumber }
                if (aggregation == "geomean") {
                    // Get geometric mean for samples.
                    getGeometricMean(metric, benchmarksIndex, target, buildNumbers).then { geoMeansValues ->
                        if (normalize) {
                            getGeometricMean(metric, goldenIndex).then { golden ->
                                val goldenValue = golden[0].second[0]
                                val results = geoMeansValues.map { it.first to it.second[0]/goldenValue}
                                response.json(results)
                            }
                        } else {
                            response.json(geoMeansValues)
                        }
                    }.catch {
                        response.sendStatus(400)
                    }
                } else {

                }

            }.catch {
                response.sendStatus(400)
            }
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get branches for [target].
    router.get("/branches", { request, response ->
        distinctValues("branch.keyword", buildInfoIndex).then { results ->
            response.json(results.toString())
        }.catch { errorMessage ->
            error(errorMessage.message ?: "Failed getting branches list.")
            response.sendStatus(400)
        }
    })

    // Get build numbers for [target].
    router.get("/buildsNumbers/:target", { request, response ->
        distinctValues("buildNumber", buildInfoIndex).then { results ->
            response.json(results.toString())
        }.catch { errorMessage ->
            error(errorMessage.message ?: "Failed getting branches list.")
            response.sendStatus(400)
        }
    })

    // Replace from Artifactory to DB.
    // TODO Bintray -> Artifactory.
    router.get("/convert/:target", { request, response ->
        val target = request.params.target.toString()
        var buildNumber: String? = null
        if (request.query != undefined) {
            if (request.query.buildNumber != undefined) {
                buildNumber = request.query.buildNumber
            }
        }
        getBuildsInfoFromArtifactory(target).then { buildInfo ->
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
                            val artifactoryUrl = "https://repo.labs.intellij.net/kotlin-native-benchmarks"
                            val fileName = "nativeReport.json"
                            val accessFileUrl = "$artifactoryUrl/$target/$currentBuildNumber/$fileName"
                            val infoParts = it.split(", ")
                            if (infoParts[3] == "master" || "eap" in currentBuildNumber || "release" in currentBuildNumber) {
                                try {
                                    val jsonReport = sendRequest(RequestMethod.GET, accessFileUrl).await()
                                    var report = convert(jsonReport, currentBuildNumber)
                                    println(currentBuildNumber)
                                    val buildInfoRecord = BuildInfo(currentBuildNumber, infoParts[1], infoParts[2],
                                            CommitsList.parse(infoParts[4]), infoParts[3])

                                    val bundleSize = if (infoParts[10] != "-") infoParts[10] else null
                                    if (bundleSize != null) {
                                        // Add bundle size.
                                        val bundleSizeBenchmark = BenchmarkResult("Kotlin/Native",
                                                BenchmarkResult.Status.PASSED, bundleSize.toDouble(),
                                                BenchmarkResult.Metric.BUNDLE_SIZE, 0.0, 1, 0)
                                        val bundleSet = BenchmarksSet(BenchmarksSet.BenchmarksSetInfo("Kotlin/Native", listOf()),
                                                listOf(bundleSizeBenchmark))
                                        report = BenchmarksReport(report.env,
                                                report.benchmarksSets + bundleSet, report.compiler)
                                    }
                                    val summaryReport = SummaryBenchmarksReport(report).toBenchmarksReport()
                                    summaryReport.buildNumber = currentBuildNumber
                                    //println(buildInfoRecord.toJson())
                                    //println("AAAAAAAAAAA")
                                    //println(summaryJson)
                                    // Save results in database.
                                    benchmarksIndex.insert(summaryReport).then { _ ->
                                        buildInfoIndex.insert(buildInfoRecord).then { _ ->
                                            println("Success insert")
                                        }.catch { errorResponse ->
                                            println("Failed to insert data for build")
                                            println(errorResponse.message)
                                        }
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
            }
            response.sendStatus(200)
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

fun getBuildsInfoFromArtifactory(target: String): Promise<String> {
    val artifactoryUrl = "https://repo.labs.intellij.net/kotlin-native-benchmarks"
    val buildsFileName = "buildsSummary.csv"
    val artifactoryBuildsDirectory = "builds"
    return sendRequest(RequestMethod.GET, "$artifactoryUrl/$artifactoryBuildsDirectory/$target/$buildsFileName")
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
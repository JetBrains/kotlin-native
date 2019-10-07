/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.influxdb

import org.jetbrains.report.*
import org.jetbrains.report.json.*
import kotlin.js.Date               // TODO - migrate to multiplatform.

data class Commit(val revision: String, val developer: String) {
    override fun toString() = "$revision by $developer"
    companion object {
        fun parse(description: String) = if (description != "...") {
            description.split(" by ").let {
                val (currentRevision, currentDeveloper) = it
                Commit(currentRevision, currentRevision)
            }
        } else {
            Commit("unknown", "unknown")
        }
    }
}

// List of commits.
class CommitsList: ConvertedFromJson {

    val commits: List<Commit>

    constructor(data: JsonElement) {
        if (data !is JsonObject) {
            error("Commits description is expected to be a JSON object!")
        }
        val changesElement = data.getOptionalField("change")
        commits = changesElement?.let {
            if (changesElement !is JsonArray) {
                error("Change field is expected to be an array. Please, check source.")
            }
            changesElement.jsonArray.map {
                with(it as JsonObject) {
                    Commit(elementToString(getRequiredField("version"), "version"),
                            elementToString(getRequiredField("username"), "username")
                    )
                }
            }
        } ?: listOf<Commit>()
    }

    private constructor(_commits: List<Commit>) {
        commits = _commits
    }

    override fun toString(): String =
        commits.toString()
    companion object {
        fun parse(description: String) = CommitsList(description.split(";").filter { it.isNotEmpty() }.map {
            Commit.parse(it)
        })
    }
}

data class BuildInfo(val number: String, val startTime: String, val endTime: String, val commitsList: CommitsList,
                     val branch: String)

// Database instance describing golden result value.
class GoldenResultMeasurement(name: String = "", metric: String = "", score: Double = 0.0, connector: InfluxDBConnector):
        Measurement<GoldenResultMeasurement>("goldenResults", connector) {
    var benchmarkName by Tag<String>("benchmark.name")
    var benchmarkScore by Field<FieldType.InfluxFloat>("benchmark.score")
    var benchmarkMetric by Tag<String>("benchmark.metric")

    init {
        benchmarkName = name
        benchmarkScore = FieldType.InfluxFloat(score)
        benchmarkMetric = metric
    }

    override fun fromInfluxJson(data: JsonElement): List<GoldenResultMeasurement> {
        val points = mutableListOf<GoldenResultMeasurement>()
        var columnsIndexes: Map<String, Int>
        if (data is JsonObject) {
            val results = data.getRequiredField("results") as JsonArray
            results.map {
                if (it is JsonObject) {
                    val series = it.getRequiredField("series") as JsonArray
                    series.map {
                        if (it is JsonObject) {
                            val columns = it.getRequiredField("columns") as JsonArray
                            columnsIndexes = columns.mapIndexed{ index, it ->
                                (it as JsonLiteral).unquoted() to index
                            }.toMap()
                            val valuesArrays = it.getRequiredField("values") as JsonArray
                            valuesArrays.forEach {
                                val values = it as JsonArray
                                val name = (values[columnsIndexes["benchmark.name"]!!] as JsonLiteral).unquoted()
                                val score = (values[columnsIndexes["benchmark.score"]!!] as JsonLiteral).double
                                val metric = (values[columnsIndexes["benchmark.metric"]!!] as JsonLiteral).unquoted()
                                points.add(GoldenResultMeasurement(name, metric, score, connector))
                            }
                        }
                    }
                }
            }
        }

        return points
    }
}

// Convert benchmarks measurements to standard benchmark report.
fun List<BenchmarkMeasurement>.toReport(): BenchmarksReport? {
    return this.firstOrNull()?. let { firstPoint ->
        // Create common part of report.
        val machine = Environment.Machine(firstPoint.envMachineCpu!!, firstPoint.envMachineOs!!)
        val jdk = Environment.JDKInstance(firstPoint.envJDKVersion!!.value, firstPoint.envJDKVendor!!.value)
        val environment = Environment(machine, jdk)
        val backend = Compiler.Backend(Compiler.backendTypeFromString(firstPoint.kotlinBackendType!!)!!,
                firstPoint.kotlinBackendVersion!!.value, firstPoint.kotlinBackendFlags!!)
        val compiler = Compiler(backend, firstPoint.kotlinVersion!!.value)
        val benchmarksList = map {
            val metric = BenchmarkResult.metricFromString(it.benchmarkMetric!!)!!
            BenchmarkResult(it.benchmarkName!! + metric.suffix, BenchmarkResult.statusFromString(it.benchmarkStatus!!.value)!!,
                    it.benchmarkScore!!.value, metric,
                    it.benchmarkRuntime!!.value, it.benchmarkRepeat!!.value, it.benchmarkWarmup!!.value)
        }
        BenchmarksReport(environment, benchmarksList, compiler)
    }
}

// Database instance describing benchmark.
class BenchmarkMeasurement(connector: InfluxDBConnector) : Measurement<BenchmarkMeasurement>("benchmarks", connector) {
    fun initBuildInfo(buildInfo: BuildInfo) {
        buildNumber = FieldType.InfluxString(buildInfo.number)
        buildBranch = FieldType.InfluxString(buildInfo.branch)
        buildCommits = FieldType.InfluxString(buildInfo.commitsList)
        buildStartTime = FieldType.InfluxString(buildInfo.startTime)
        buildEndTime = FieldType.InfluxString(buildInfo.endTime)
    }

    fun copy(): BenchmarkMeasurement {
        val point = BenchmarkMeasurement(connector)
        point.envMachineCpu = envMachineCpu
        point.envMachineOs = envMachineOs
        point.envJDKVendor = envJDKVendor
        point.envJDKVersion = envJDKVersion

        point.kotlinBackendType = kotlinBackendType
        point.kotlinBackendVersion = kotlinBackendVersion
        point.kotlinBackendFlags = kotlinBackendFlags
        point.kotlinVersion = kotlinVersion

        point.benchmarkName = benchmarkName
        point.benchmarkStatus = benchmarkStatus
        point.benchmarkScore = benchmarkScore
        point.benchmarkMetric = benchmarkMetric
        point.benchmarkRuntime = benchmarkRuntime
        point.benchmarkRepeat = benchmarkRepeat
        point.benchmarkWarmup = benchmarkWarmup
        point.timestamp = Date.now().toLong() * 1000 + benchmarkRepeat!!.value
        point.buildNumber = buildNumber
        point.buildBranch = buildBranch
        point.buildCommits = buildCommits
        point.buildStartTime = buildStartTime
        point.buildEndTime = buildEndTime
        return point
    }

    companion object: EntityFromJsonFactory<List<BenchmarkMeasurement>> {
        private lateinit var connector: InfluxDBConnector
        // Convert from standard benchmarks report to measurements instances.
        fun fromReport(benchmarksReport: BenchmarksReport, buildInfo: BuildInfo?, connector: InfluxDBConnector):
                List<BenchmarkMeasurement> {
            val points = mutableListOf<BenchmarkMeasurement>()
            benchmarksReport.benchmarks.forEach { (name, results) ->
                results.forEach {
                    val point = BenchmarkMeasurement(connector)
                    point.envMachineCpu = benchmarksReport.env.machine.cpu
                    point.envMachineOs = benchmarksReport.env.machine.os
                    point.envJDKVendor = FieldType.InfluxString(benchmarksReport.env.jdk.vendor)
                    point.envJDKVersion = FieldType.InfluxString(benchmarksReport.env.jdk.version)

                    point.kotlinBackendType = benchmarksReport.compiler.backend.type.toString()
                    point.kotlinBackendVersion = FieldType.InfluxString(benchmarksReport.compiler.backend.version)
                    point.kotlinBackendFlags = benchmarksReport.compiler.backend.flags
                    point.kotlinVersion = FieldType.InfluxString(benchmarksReport.compiler.kotlinVersion)

                    point.benchmarkName = name
                    point.benchmarkStatus = FieldType.InfluxString(it.status)
                    point.benchmarkScore = FieldType.InfluxFloat(it.score)
                    point.benchmarkMetric = it.metric.value
                    point.benchmarkRuntime = FieldType.InfluxFloat(it.runtimeInUs)
                    point.benchmarkRepeat = FieldType.InfluxInt(it.repeat)
                    point.benchmarkWarmup = FieldType.InfluxInt(it.warmup)
                    buildInfo?.let {
                        point.initBuildInfo(buildInfo)
                    }
                    points.add(point)
                }
            }
            return points
        }

        fun create(data: JsonElement, connector: InfluxDBConnector, buildInfo: BuildInfo? = null):
                List<BenchmarkMeasurement> {
            this.connector = connector
            val results = create(data)
            buildInfo?.let {
                results.forEach {
                    it.initBuildInfo(buildInfo)
                }
            }
            return results
        }

        // Create measurements from json format of standard benchmarks report.
        override fun create(data: JsonElement): List<BenchmarkMeasurement> {
            val points = mutableListOf<BenchmarkMeasurement>()
            if (data is JsonObject) {
                val env = data.getRequiredField("env") as JsonObject
                val machine = env.getRequiredField("machine") as JsonObject
                val cpu = elementToString(machine.getRequiredField("cpu"), "cpu")
                val os = elementToString(machine.getRequiredField("os"), "os")

                val jdk = env.getRequiredField("jdk") as JsonObject
                val jdkVersion = elementToString(jdk.getRequiredField("version"), "version")
                val jdkVendor = elementToString(jdk.getRequiredField("vendor"), "vendor")
                val benchmarksObj = data.getRequiredField("benchmarks")
                val compiler = data.getRequiredField("kotlin") as JsonObject
                val backend = compiler.getRequiredField("backend") as JsonObject
                val typeElement = backend.getRequiredField("type") as JsonLiteral
                val type = typeElement.unquoted()
                val version = elementToString(backend.getRequiredField("version"), "version")
                val flagsArray = backend.getOptionalField("flags")
                var flags: List<String> = emptyList()
                if (flagsArray != null && flagsArray is JsonArray) {
                    flags = flagsArray.jsonArray.map { it.toString() }
                }
                val kotlinVersion = elementToString(compiler.getRequiredField("kotlinVersion"), "kotlinVersion")
                if (benchmarksObj is JsonArray) {
                    benchmarksObj.jsonArray.forEach {
                        if (it is JsonObject) {
                            val name = elementToString(it.getRequiredField("name"), "name")
                            val metricElement = it.getOptionalField("metric")
                            val metric = if (metricElement != null && metricElement is JsonLiteral)
                                metricElement.unquoted()
                            else "EXECUTION_TIME"
                            val statusElement = it.getRequiredField("status")
                            if (statusElement is JsonLiteral) {
                                val status = statusElement.unquoted()
                                val score = elementToDouble(it.getRequiredField("score"), "score")
                                val runtimeInUs = elementToDouble(it.getRequiredField("runtimeInUs"), "runtimeInUs")
                                val repeat = elementToInt(it.getRequiredField("repeat"), "repeat")
                                val warmup = elementToInt(it.getRequiredField("warmup"), "warmup")

                                val point = BenchmarkMeasurement(connector)
                                point.envMachineCpu = cpu
                                point.envMachineOs = os
                                point.envJDKVendor = FieldType.InfluxString(jdkVendor)
                                point.envJDKVersion = FieldType.InfluxString(jdkVersion)

                                point.kotlinBackendType = type
                                point.kotlinBackendVersion = FieldType.InfluxString(version)
                                point.kotlinBackendFlags = flags
                                point.kotlinVersion = FieldType.InfluxString(kotlinVersion)

                                point.benchmarkName = name
                                point.benchmarkStatus = FieldType.InfluxString(status)
                                point.benchmarkScore = FieldType.InfluxFloat(score)
                                point.benchmarkMetric = metric
                                point.benchmarkRuntime = FieldType.InfluxFloat(runtimeInUs)
                                point.benchmarkRepeat = FieldType.InfluxInt(repeat)
                                point.benchmarkWarmup = FieldType.InfluxInt(warmup)
                                point.timestamp = Date.now().toLong() * 1000 + repeat // TODO multiplatform. Hack to make unique timestamp.
                                points.add(point)
                            } else {
                                error("Status should be string literal.")
                            }
                        } else {
                            error("Benchmark entity is expected to be an object. Please, check origin files.")
                        }
                    }
                } else {
                    error("Benchmarks field is expected to be an array. Please, check origin files.")
                }
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
            return points
        }
    }

    override fun fromInfluxJson(data: JsonElement): List<BenchmarkMeasurement> {
        val points = mutableListOf<BenchmarkMeasurement>()
        var columnsIndexes: Map<String, Int>
        if (data is JsonObject) {
            val results = data.getRequiredField("results") as JsonArray
            results.map {
                if (it is JsonObject) {
                    val series = it.getRequiredField("series") as JsonArray
                    series.map {
                        if (it is JsonObject) {
                            val columns = it.getRequiredField("columns") as JsonArray
                            columnsIndexes = columns.mapIndexed{ index, it ->
                                (it as JsonLiteral).unquoted() to index
                            }.toMap()
                            val valuesArrays = it.getRequiredField("values") as JsonArray
                            valuesArrays.forEach {
                                val point = BenchmarkMeasurement(connector)
                                val values = it as JsonArray
                                point.envMachineCpu = (values[columnsIndexes["environment.machine.cpu"]!!] as JsonLiteral)
                                        .unquoted()
                                point.envMachineOs = (values[columnsIndexes["environment.machine.os"]!!] as JsonLiteral)
                                        .unquoted()
                                point.envJDKVendor = FieldType.InfluxString(
                                        (values[columnsIndexes["environment.jdk.vendor"]!!] as JsonLiteral).unquoted())
                                point.envJDKVersion = FieldType.InfluxString(
                                        (values[columnsIndexes["environment.jdk.version"]!!] as JsonLiteral).unquoted())

                                point.kotlinBackendType = (values[columnsIndexes["kotlin.backend.type"]!!] as JsonLiteral)
                                        .unquoted()
                                point.kotlinBackendVersion = FieldType.InfluxString(
                                        (values[columnsIndexes["kotlin.backend.version"]!!] as JsonLiteral).unquoted())
                                point.kotlinBackendFlags = (values[columnsIndexes["environment.machine.os"]!!] as JsonLiteral)
                                        .unquoted().replace("[", "").replace("]", "")
                                        .split(',')
                                point.kotlinVersion = FieldType.InfluxString(
                                        (values[columnsIndexes["kotlin.kotlinVersion"]!!] as JsonLiteral).unquoted())

                                point.benchmarkName = (values[columnsIndexes["benchmark.name"]!!] as JsonLiteral).unquoted()
                                point.benchmarkStatus = FieldType.InfluxString(
                                        (values[columnsIndexes["benchmark.status"]!!] as JsonLiteral).unquoted())
                                point.benchmarkScore = FieldType.InfluxFloat(
                                        (values[columnsIndexes["benchmark.score"]!!] as JsonLiteral).double)
                                point.benchmarkMetric = (values[columnsIndexes["benchmark.metric"]!!] as JsonLiteral).unquoted()
                                point.benchmarkRuntime = FieldType.InfluxFloat(
                                        (values[columnsIndexes["benchmark.runtimeInUs"]!!] as JsonLiteral).double)

                                point.benchmarkRepeat = FieldType.InfluxInt(
                                        (values[columnsIndexes["benchmark.repeat"]!!] as JsonLiteral).int)
                                point.benchmarkWarmup = FieldType.InfluxInt(
                                        (values[columnsIndexes["benchmark.warmup"]!!] as JsonLiteral).int)
                                point.buildNumber = FieldType.InfluxString(values[columnsIndexes["build.number"]!!] as JsonLiteral)
                                point.buildBranch = FieldType.InfluxString(values[columnsIndexes["build.branch"]!!] as JsonLiteral)
                                point.buildCommits = FieldType.InfluxString(values[columnsIndexes["build.commits"]!!] as JsonLiteral)
                                point.buildStartTime = FieldType.InfluxString(values[columnsIndexes["build.startTime"]!!] as JsonLiteral)
                                point.buildEndTime = FieldType.InfluxString(values[columnsIndexes["build.endTime"]!!] as JsonLiteral)
                                points.add(point)
                            }
                        }
                    }
                }
            }
        }
        return points
    }
    
    // Environment.
    // Machine.
    var envMachineCpu by Tag<String>("environment.machine.cpu")
    var envMachineOs by Tag<String>("environment.machine.os")
    // JDK.
    var envJDKVersion by Field<FieldType.InfluxString>("environment.jdk.version")
    var envJDKVendor by Field<FieldType.InfluxString>("environment.jdk.vendor")

    // Kotlin information.
    // Backend.
    var kotlinBackendType by Tag<String>("kotlin.backend.type")
    var kotlinBackendVersion by Field<FieldType.InfluxString>("kotlin.backend.version")
    var kotlinBackendFlags by Tag<List<String>>("kotlin.backend.flags")
    var kotlinVersion by Field<FieldType.InfluxString>("kotlin.kotlinVersion")

    // Benchmark data.
    var benchmarkName by Tag<String>("benchmark.name")
    var benchmarkStatus by Field<FieldType.InfluxString>("benchmark.status")
    var benchmarkScore by Field<FieldType.InfluxFloat>("benchmark.score")
    var benchmarkMetric by Tag<String>("benchmark.metric")
    var benchmarkRuntime by Field<FieldType.InfluxFloat>("benchmark.runtimeInUs")
    var benchmarkRepeat by Field<FieldType.InfluxInt>("benchmark.repeat")
    var benchmarkWarmup by Field<FieldType.InfluxInt>("benchmark.warmup")

    // Build information (from CI).
    var buildNumber by Field<FieldType.InfluxString>("build.number")
    var buildStartTime by Field<FieldType.InfluxString>("build.startTime")
    var buildEndTime by Field<FieldType.InfluxString>("build.endTime")
    var buildCommits by Field<FieldType.InfluxString>("build.commits")
    var buildBranch by Field<FieldType.InfluxString>("build.branch")
}
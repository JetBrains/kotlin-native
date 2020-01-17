/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.report

import org.jetbrains.report.json.*

interface JsonSerializable {
    fun serializeFields(): String
    fun toJson(): String {
        return """
        {
            ${serializeFields()}
        }
        """
    }

    // Convert iterable objects arrays, lists to json.
    fun <T> arrayToJson(data: Iterable<T>): String {
        return data.joinToString(prefix = "[", postfix = "]") {
            if (it is JsonSerializable) it.toJson() else "\"$it\""
        }
    }
}

interface EntityFromJsonFactory<T>: ConvertedFromJson {
    fun create(data: JsonElement): T
}

// Class for benchmarks report with all information of run.
class BenchmarksReport(val env: Environment, val benchmarksSets: List<BenchmarksSet>, val compiler: Compiler):
        JsonSerializable {

    companion object: EntityFromJsonFactory<BenchmarksReport> {
        override fun create(data: JsonElement): BenchmarksReport {
            if (data is JsonObject) {
                val env = Environment.create(data.getRequiredField("env"))
                val benchmarksObj = data.getRequiredField("benchmarksSets")
                val compiler = Compiler.create(data.getRequiredField("kotlin"))
                val buildNumberField = data.getOptionalField("buildNumber")
                val benchmarksSetsList = parseBenchmarksSets(benchmarksObj)
                val report = BenchmarksReport(env, benchmarksSetsList, compiler)
                buildNumberField?.let { report.buildNumber = (it as JsonLiteral).unquoted() }
                return report
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
        }

        // Parse array with benchmarks sets to list.
        fun parseBenchmarksSets(data: JsonElement): List<BenchmarksSet> {
            if (data is JsonArray) {
                return data.jsonArray.map { BenchmarksSet.create(it as JsonObject) }
            } else {
                error("benchmarksSets field is expected to be an array. Please, check origin files.")
            }
        }
    }

    val benchmarks = benchmarksSets.map { it.benchmarks }.reduce { acc, map -> acc + map }

    var buildNumber: String? = null

    override fun serializeFields(): String {
        val buildNumberField = buildNumber?.let { """,
            "buildNumber": "$buildNumber"
        """} ?: ""
        return """
            "env": ${env.toJson()},
            "kotlin": ${compiler.toJson()},
            "benchmarksSets": ${arrayToJson(benchmarksSets)}$buildNumberField
        """
    }

    // Concatenate benchmarks report if they have same environment and compiler.
    operator fun plus(other: BenchmarksReport): BenchmarksReport {
        if (compiler != other.compiler || env != other.env) {
            error ("It's impossible to concat reports from different machines!")
        }
        val mergedBenchmarks = benchmarksSets.toMutableList()
        other.benchmarksSets.forEach {addedSet ->
            benchmarksSets.forEachIndexed { index, it ->
                // Merge same benchmarks sets and add new ones.
                if (addedSet.setInfo.hasSameOriginWith(it.setInfo)) {
                    mergedBenchmarks[index] = it + addedSet
                } else {
                    mergedBenchmarks.add(addedSet)
                }
            }
        }
        return BenchmarksReport(env, mergedBenchmarks, compiler)
    }
}

class BenchmarksSet(val setInfo: BenchmarksSetInfo, benchmarksList: List<BenchmarkResult>): JsonSerializable {

    companion object: EntityFromJsonFactory<BenchmarksSet> {
        override fun create(data: JsonElement): BenchmarksSet {
            if (data is JsonObject) {
                val name = BenchmarksSet.elementToString(data.getRequiredField("name"), "name")
                val benchmarksObj = data.getRequiredField("benchmarks")
                val flagsArray = data.getOptionalField("compilerFlags")
                var flags: List<String> = emptyList()
                if (flagsArray != null && flagsArray is JsonArray) {
                    flags = flagsArray.jsonArray.map { (it as JsonLiteral).unquoted() }
                }
                val benchmarksList = parseBenchmarksArray(benchmarksObj)
                return BenchmarksSet(BenchmarksSetInfo(name, flags), benchmarksList)
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
        }

        // Parse array with benchmarks to list
        fun parseBenchmarksArray(data: JsonElement): List<BenchmarkResult> {
            if (data is JsonArray) {
                return data.jsonArray.map { BenchmarkResult.create(it as JsonObject) }
            } else {
                error("Benchmarks field is expected to be an array. Please, check origin files.")
            }
        }

        // Made a map of becnhmarks with name as key from list.
        private fun structBenchmarks(benchmarksList: List<BenchmarkResult>) =
                benchmarksList.groupBy{ it.name }
    }

    data class BenchmarksSetInfo(val name: String, val compilerFlags: List<String>) {
        fun hasSameOriginWith(other: BenchmarksSetInfo) =
                other.name == name && other.compilerFlags.size == compilerFlags.size &&
                        (other.compilerFlags - compilerFlags).isEmpty()
    }

    val benchmarks: Map<String, List<BenchmarkResult>> = structBenchmarks(benchmarksList)

    override fun serializeFields(): String {
        val result = """
            "name": "${setInfo.name}",
            "benchmarks": ${arrayToJson(benchmarks.flatMap{it.value})}
        """
        // Don't print flags field if there is no one.
        if (setInfo.compilerFlags.isEmpty()) {
            return result
        }
        else {
            return """
                    $result,
                "compilerFlags": ${arrayToJson(setInfo.compilerFlags)}
                """
        }
    }

    // Concatenate benchmarks sets if they have same name and flags.
    operator fun plus(other: BenchmarksSet): BenchmarksSet {
        if (!setInfo.hasSameOriginWith(other.setInfo)) {
            error ("It's impossible to concat benchmarks sets with different names and compiler flags!")
        }
        val mergedBenchmarks = HashMap<String, List<BenchmarkResult>>(benchmarks)
        mergedBenchmarks.putAll(other.benchmarks)
        return BenchmarksSet(setInfo, mergedBenchmarks.flatMap{it.value})
    }
}

// Class for kotlin compiler
data class Compiler(val backend: Backend, val kotlinVersion: String): JsonSerializable {

    enum class BackendType(val type: String) {
        JVM("jvm"),
        NATIVE("native")
    }

    companion object: EntityFromJsonFactory<Compiler> {
        override fun create(data: JsonElement): Compiler {
            if (data is JsonObject) {
                val backend = Backend.create(data.getRequiredField("backend"))
                val kotlinVersion = elementToString(data.getRequiredField("kotlinVersion"), "kotlinVersion")

                return Compiler(backend, kotlinVersion)
            } else {
                error("Kotlin entity is expected to be an object. Please, check origin files.")
            }
        }

        fun backendTypeFromString(s: String): BackendType? = BackendType.values().find { it.type == s }
    }

    // Class for compiler backend
    data class Backend(val type: BackendType, val version: String): JsonSerializable {
        companion object: EntityFromJsonFactory<Backend> {
            override fun create(data: JsonElement): Backend {
                if (data is JsonObject) {
                    val typeElement = data.getRequiredField("type")
                    if (typeElement is JsonLiteral) {
                        val type = backendTypeFromString(typeElement.unquoted()) ?: error("Backend type should be 'jvm' or 'native'")
                        val version = elementToString(data.getRequiredField("version"), "version")

                        return Backend(type, version)
                    } else {
                        error("Backend type should be string literal.")
                    }
                } else {
                    error("Backend entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun serializeFields(): String {
            return """
                "type": "${type.type}",
                "version": "${version}"
                """
        }
    }

    override fun serializeFields(): String {
        return """
            "backend": ${backend.toJson()},
            "kotlinVersion": "${kotlinVersion}"
        """
    }
}

// Class for description of environment of benchmarks run
data class Environment(val machine: Machine, val jdk: JDKInstance): JsonSerializable {

    companion object: EntityFromJsonFactory<Environment> {
        override fun create(data: JsonElement): Environment {
            if (data is JsonObject) {
                val machine = Machine.create(data.getRequiredField("machine"))
                val jdk = JDKInstance.create(data.getRequiredField("jdk"))

                return Environment(machine, jdk)
            } else {
                error("Environment entity is expected to be an object. Please, check origin files.")
            }
        }
    }

    // Class for description of machine used for benchmarks run.
    data class Machine(val cpu: String, val os: String): JsonSerializable {
        companion object: EntityFromJsonFactory<Machine> {
            override fun create(data: JsonElement): Machine {
                if (data is JsonObject) {
                    val cpu = elementToString(data.getRequiredField("cpu"), "cpu")
                    val os = elementToString(data.getRequiredField("os"), "os")

                    return Machine(cpu, os)
                } else {
                    error("Machine entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun serializeFields(): String {
            return """
                "cpu": "$cpu",
                "os": "$os"
            """
        }
    }

    // Class for description of jdk used for benchmarks run.
    data class JDKInstance(val version: String, val vendor: String): JsonSerializable {
        companion object: EntityFromJsonFactory<JDKInstance> {
            override fun create(data: JsonElement): JDKInstance {
                if (data is JsonObject) {
                    val version = elementToString(data.getRequiredField("version"), "version")
                    val vendor = elementToString(data.getRequiredField("vendor"), "vendor")

                    return JDKInstance(version, vendor)
                } else {
                    error("JDK entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun serializeFields(): String {
            return """
                "version": "$version",
                "vendor": "$vendor"
            """
        }
    }

    override fun serializeFields(): String {
        return """
                "machine": ${machine.toJson()},
                "jdk": ${jdk.toJson()}
            """
    }
}

open class BenchmarkResult(val name: String, val status: Status,
                      val score: Double, val metric: Metric, val runtimeInUs: Double,
                      val repeat: Int, val warmup: Int): JsonSerializable {

    enum class Metric(val suffix: String, val value: String) {
        EXECUTION_TIME("", "EXECUTION_TIME"),
        CODE_SIZE(".codeSize", "CODE_SIZE"),
        COMPILE_TIME(".compileTime", "COMPILE_TIME"),
        BUNDLE_SIZE(".bundleSize", "BUNDLE_SIZE")
    }

    constructor(name: String, score: Double) : this(name, Status.PASSED, score, Metric.EXECUTION_TIME, 0.0, 0, 0)

    companion object: EntityFromJsonFactory<BenchmarkResult> {

        override fun create(data: JsonElement): BenchmarkResult {
            if (data is JsonObject) {
                var name = elementToString(data.getRequiredField("name"), "name")
                val metricElement = data.getOptionalField("metric")
                val metric = if (metricElement != null && metricElement is JsonLiteral)
                                metricFromString(metricElement.unquoted()) ?: Metric.EXECUTION_TIME
                            else Metric.EXECUTION_TIME
                name += metric.suffix
                val statusElement = data.getRequiredField("status")
                if (statusElement is JsonLiteral) {
                    val status = statusFromString(statusElement.unquoted())
                            ?: error("Status should be PASSED or FAILED")

                    val score = elementToDouble(data.getRequiredField("score"), "score")
                    val runtimeInUs = elementToDouble(data.getRequiredField("runtimeInUs"), "runtimeInUs")
                    val repeat = elementToInt(data.getRequiredField("repeat"), "repeat")
                    val warmup = elementToInt(data.getRequiredField("warmup"), "warmup")

                    return BenchmarkResult(name, status, score, metric, runtimeInUs, repeat, warmup)
                } else {
                    error("Status should be string literal.")
                }
            } else {
                error("Benchmark entity is expected to be an object. Please, check origin files.")
            }
        }

        fun statusFromString(s: String): Status? = Status.values().find { it.value == s }
        fun metricFromString(s: String): Metric? = Metric.values().find { it.value == s }
    }

    enum class Status(val value: String) {
        PASSED("PASSED"),
        FAILED("FAILED")
    }

    override fun serializeFields(): String {
        return """
            "name": "${name.removeSuffix(metric.suffix)}",
            "status": "${status.value}",
            "score": ${score},
            "metric": "${metric.value}",
            "runtimeInUs": ${runtimeInUs},
            "repeat": ${repeat},
            "warmup": ${warmup}
        """
    }

    val shortName: String
        get() = name.removeSuffix(metric.suffix)
}
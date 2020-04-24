/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.elastic

import org.jetbrains.report.*
import org.jetbrains.report.json.*
import org.jetbrains.analyzer.MeanVarianceBenchmark
import kotlin.js.Promise     // TODO - migrate to multiplatform.

data class Commit(val revision: String, val developer: String): JsonSerializable {
    override fun toString() = "$revision by $developer"

    override fun serializeFields() = """
        "revision": "$revision",
        "developer": "$developer"
    """

    companion object: EntityFromJsonFactory<Commit> {
        fun parse(description: String) = if (description != "...") {
            description.split(" by ").let {
                val (currentRevision, currentDeveloper) = it
                Commit(currentRevision, currentDeveloper)
            }
        } else {
            Commit("unknown", "unknown")
        }

        override fun create(data: JsonElement): Commit {
            if (data is JsonObject) {
                val revision = elementToString(data.getRequiredField("revision"), "revision")
                val developer = elementToString(data.getRequiredField("developer"), "developer")
                return Commit(revision, developer)
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
        }
    }
}

// List of commits.
class CommitsList: ConvertedFromJson, JsonSerializable {

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

    constructor(_commits: List<Commit>) {
        commits = _commits
    }

    override fun toString(): String =
        commits.toString()
    companion object {
        fun parse(description: String) = CommitsList(description.split(";").filter { it.isNotEmpty() }.map {
            Commit.parse(it)
        })
    }

    override fun serializeFields() = """
        "commits": ${arrayToJson(commits)}
    """
}

data class BuildInfo(val buildNumber: String, val startTime: String, val endTime: String, val commitsList: CommitsList,
                     val branch: String): JsonSerializable {
    override fun serializeFields() = """
        "buildNumber": "$buildNumber",
        "startTime": "$startTime",
        "endTime": "$endTime",
        ${commitsList.serializeFields()},
        "branch": "$branch"
    """

    companion object: EntityFromJsonFactory<BuildInfo> {
        override fun create(data: JsonElement): BuildInfo {
            if (data is JsonObject) {
                val buildNumber = elementToString(data.getRequiredField("buildNumber"), "buildNumber")
                val startTime = elementToString(data.getRequiredField("startTime"), "startTime")
                val endTime = elementToString(data.getRequiredField("endTime"), "endTime")
                val branch = elementToString(data.getRequiredField("branch"), "branch")
                val commitsList = data.getRequiredField("commits")
                val commits = if (commitsList is JsonArray) {
                    commitsList.jsonArray.map { Commit.create(it as JsonObject) }
                } else {
                    error("benchmarksSets field is expected to be an array. Please, check origin files.")
                }
                return BuildInfo(buildNumber, startTime, endTime, CommitsList(commits), branch)
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
        }
    }
}

enum class ElasticSearchType {
    TEXT, KEYWORD, DATE, LONG, DOUBLE, BOOLEAN, OBJECT, NESTED
}

abstract class ElasticSearchIndex(val indexName: String, val connector: ElasticSearchConnector) {
    var nextId = 0L
    val url = "${connector.url}/$indexName"
    // Insert data.
    fun insert(data: JsonSerializable): Promise<String> {
        val description = data.toJson()
        val writeUrl = "$url/_doc/$nextId?pretty"
        nextId++
        return connector.request(RequestMethod.POST, writeUrl, body = description)
    }

    // Make request.
    // TODO - replace to DSL
    fun search(requestJson: String, filterPathes: List<String> = emptyList()): Promise<String> {
        val url = "${connector.url}/$indexName/_search?pretty${if (filterPathes.isNotEmpty()) 
            "&filter_path=" + filterPathes.joinToString(",") else ""}"
        return connector.request(RequestMethod.POST, url, body = requestJson)
    }

    init {
        val queryBody = """{
               "_source": ["_id"],"size": 1,
               "query": {
                  "match_all": {}
               }
            }"""
        search(queryBody, listOf("hits.total.value")).then { responseString ->
            val response = JsonTreeParser.parse(responseString).jsonObject
            val value = response.getObjectOrNull("hits")?.getObjectOrNull("total")?.getPrimitiveOrNull("value")?.content
                    ?: error("Error response from ElasticSearch:\n$responseString")
            nextId = value.toLong()
            println(nextId)
        }.catch { errorMessage ->
            error(errorMessage.message ?: "Failed getting next id for index $indexName")
        }
    }

    /*fun distinct(fieldName) {
        val description = """
            {
             "size":0,
             "aggs" : {
              "uniq_${fieldName}" : {
               "terms" : { "field" : "${fieldName}" }
               }
              }
            }
        """.trimIndent()
        connector.request(RequestMethod.GET, writeUrl, body = description).then { response ->
            println(response)
        }
    }*/

    abstract val mapping: Map<String, ElasticSearchType>

    val mappingDescription: String
        get() = """
            {
              "mappings": {
                "properties": {
                ${mapping.map { (property, type) ->
                    "\"${property}\": { \"type\": \"${type.name.toLowerCase()}\"${if (type == ElasticSearchType.DATE) "," +
                            "\"format\": \"basic_date_time_no_millis\"" else ""} }"}.joinToString()}
                }
              }
            }
        """.trimIndent()

    fun createMapping() =
        connector.request(RequestMethod.PUT, url, body = mappingDescription)
}

class BenchmarksIndex(connector: ElasticSearchConnector): ElasticSearchIndex("benchmarks", connector) {
    override val mapping: Map<String, ElasticSearchType>
        get() = mapOf("buildNumber" to ElasticSearchType.KEYWORD,
                "benchmarksSets" to ElasticSearchType.NESTED,
                "env" to ElasticSearchType.NESTED,
                "kotlin" to ElasticSearchType.NESTED)
}

class GoldenResultsIndex(connector: ElasticSearchConnector): ElasticSearchIndex("golden", connector) {
    override val mapping: Map<String, ElasticSearchType>
        get() = mapOf("buildNumber" to ElasticSearchType.KEYWORD,
                "benchmarksSets" to ElasticSearchType.NESTED,
                "env" to ElasticSearchType.NESTED,
                "kotlin" to ElasticSearchType.NESTED)
}

class BuildInfoIndex(connector: ElasticSearchConnector): ElasticSearchIndex("builds", connector) {
    override val mapping: Map<String, ElasticSearchType>
        get() = mapOf("buildNumber" to ElasticSearchType.KEYWORD,
                "startTime" to ElasticSearchType.DATE,
                "endTime" to ElasticSearchType.DATE,
                "commits" to ElasticSearchType.NESTED)
}

class NormalizedMeanVarianceBenchmark(name: String, status: BenchmarkResult.Status, score: Double, metric: BenchmarkResult.Metric,
                            runtimeInUs: Double, repeat: Int, warmup: Int, variance: Double, val normalizedScore: Double) :
        MeanVarianceBenchmark(name, status, score, metric, runtimeInUs, repeat, warmup, variance) {

    override fun serializeFields(): String {
        return """
            ${super.serializeFields()},
            "normalizedScore": $normalizedScore
            """
    }
}
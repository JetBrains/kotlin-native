/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

import org.w3c.xhr.*
import kotlin.js.json
import org.jetbrains.report.json.*
import org.jetbrains.build.Build

const val teamCityUrl = "https://buildserver.labs.intellij.net/app/rest"
const val downloadBintrayUrl = "https://dl.bintray.com/content/lepilkinaelena/KotlinNativePerformance"
const val uploadBintrayUrl = "https://api.bintray.com/content/lepilkinaelena/KotlinNativePerformance"
const val buildsFileName = "buildsSummary.csv"
const val bintrayPackage = "builds"
const val buildsInfoPartsNumber = 11

operator fun <K, V> Map<K, V>?.get(key: K) = this?.get(key)

// Local cache for saving information about builds got from Bintray.
object LocalCache {
    private val knownTargets = listOf("Linux", "MacOSX", "Windows10")
    private val buildsInfo = mutableMapOf<String, MutableMap<String, String>>()

    fun clean(onlyTarget: String? = null) {
        onlyTarget?.let {
            buildsInfo[onlyTarget]?.clear()
        } ?: buildsInfo.clear()
    }

    fun fill(onlyTarget: String? = null) {
        onlyTarget?.let {
            val buildsDescription = getBuildsInfoFromBintray(onlyTarget).lines().drop(1)
            buildsInfo[onlyTarget] = mutableMapOf<String, String>()
            buildsDescription.forEach {
                if (!it.isEmpty()) {
                    val buildNumber = it.substringBefore(',')
                    if (!"\\d+(\\.\\d+)+-\\w+-\\d+".toRegex().matches(buildNumber)) {
                        error("Build number $buildNumber differs from expected format. File with data for " +
                                "target $onlyTarget could be corrupted.")
                    }
                    buildsInfo[onlyTarget]!![buildNumber] = it
                }
            }
        } ?: run {
            knownTargets.forEach {
                fill(it)
            }
        }
    }

    fun buildExists(target: String, buildNumber: String) =
            buildsInfo[target][buildNumber]?.let { true } ?: false

    private fun getBuilds(target: String, buildNumber: String? = null) =
            buildsInfo[target]?.let { buildsList ->
                buildNumber?.let {
                    // Check if interesting build id is in cache.
                    buildsList[it]?.let { buildsList.values }
                } ?: buildsList.values
            }

    operator fun get(target: String, buildId: String? = null): Collection<String> {
        val builds = getBuilds(target, buildId)

        if (builds.isNullOrEmpty()) {
            // No suitable builds were found.
            // Refill cache.
            clean(target)
            fill(target)
            return getBuilds(target, buildId) ?: listOf<String>()
        }

        return builds
    }
}

// Build information provided from request.
data class BuildInfo(val buildNumber: String, val branch: String, val startTime: String,
                     val finishTime: String)

data class BuildRegister(val buildId: String, val teamCityUser: String, val teamCityPassword: String,
                    val bintrayUser: String, val bintrayPassword: String,
                    val target: String, val buildType: String, val failuresNumber: Int,
                    val executionTime: String, val compileTime: String, val codeSize: String,
                    val bundleSize: String?) {
    companion object {
        fun create(json: String): BuildRegister {
            val requestDetails = JSON.parse<BuildRegister>(json)
            // Parse method doesn't create real instance with all methods. So create it by hands.
            return BuildRegister(requestDetails.buildId, requestDetails.teamCityUser, requestDetails.teamCityPassword,
                    requestDetails.bintrayUser, requestDetails.bintrayPassword, requestDetails.target,
                    requestDetails.buildType, requestDetails.failuresNumber, requestDetails.executionTime, requestDetails.compileTime,
                    requestDetails.codeSize, requestDetails.bundleSize)
        }
    }

    private val teamCityBuildUrl: String by lazy { "$teamCityUrl/builds/id:$buildId" }

    val changesListUrl: String by lazy {
        "$teamCityUrl/changes/?locator=build:id:$buildId"
    }

    private fun sendTeamCityRequest(url: String) = sendGetRequest(url, teamCityUser, teamCityPassword)

    fun getBuildInformation(): BuildInfo {
        val buildNumber = sendTeamCityRequest("$teamCityBuildUrl/number")
        val branch = sendTeamCityRequest("$teamCityBuildUrl/branchName")
        val startTime = sendTeamCityRequest("$teamCityBuildUrl/startDate")
        val finishTime = sendTeamCityRequest("$teamCityBuildUrl/finishDate")
        return BuildInfo(buildNumber, branch, startTime, finishTime)
    }
}

data class Commit(val revision: String, val developer: String)

// List of commits.
class CommitsList(data: JsonElement): ConvertedFromJson {

    val commits: List<Commit>

    init {
        if (data !is JsonObject) {
            error("Commits description is expected to be a json object!")
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
}

fun getBuildsInfoFromBintray(target: String) =
        sendGetRequest("$downloadBintrayUrl/$target/$buildsFileName")

// Parse  and postprocess result of response with build description.
fun prepareBuildsResponse(builds: Collection<String>, type: String): List<Build> {
    val buildsObjects = mutableListOf<Build>()
    builds.forEach {
        val tokens = it.split(",").map { it.trim() }
        if (tokens.size != buildsInfoPartsNumber) {
            error("Build description $it doesn't contain all necessary information. " +
                    "File with data could be corrupted.")
        }
        if (tokens[5] == type || type == "day") {
            buildsObjects.add(Build(tokens[0], tokens[1], tokens[2], tokens[3],
                    tokens[4], tokens[5], tokens[6].toInt(), tokens[7], tokens[8], tokens[9],
                    if (tokens[10] == "-") null else tokens[10]))
        }
    }
    return buildsObjects
}

// Routing of requests to current server.
fun router() {
    val express = require("express")
    val router = express.Router()

    // Register build on Bintray.
    router.post("/register", { request, response ->
        val register = BuildRegister.create(JSON.stringify(request.body))

        // Get information from TeamCity.
        val buildInfo = register.getBuildInformation()
        val changes = sendGetRequest(register.changesListUrl, register.teamCityUser,
                register.teamCityPassword, true)
        val commitsList = CommitsList(JsonTreeParser.parse(changes))
        val commitsDescription = buildString {
            commitsList.commits.forEach {
                append("${it.revision} by ${it.developer};")
            }
        }

        // Get summary file from Bintray.
        var buildsDescription = getBuildsInfoFromBintray(register.target)
        // Add information about new build.
        //var buildsDescription = "build, start time, finish time, branch, commits, type, failuresNumber, execution time, compile time, code size, bundle size\n"
        buildsDescription += "${buildInfo.buildNumber}, ${buildInfo.startTime}, ${buildInfo.finishTime}, " +
                "${buildInfo.branch}, $commitsDescription, ${register.buildType}, ${register.failuresNumber}, " +
                "${register.executionTime}, ${register.compileTime}, ${register.codeSize}, " +
                "${register?.bundleSize ?: "-"}\n"

        // Upload new version of file.
        val uploadUrl = "$uploadBintrayUrl/$bintrayPackage/latest/${register.target}/${buildsFileName}?publish=1&override=1"
        sendUploadRequest(uploadUrl, buildsDescription, register.bintrayUser, register.bintrayPassword)

        // Send response.
        response.sendStatus(200)
    })

    // Get list of builds.
    router.get("/builds/:target/:type/:id", { request, response ->
        val builds = LocalCache[request.params.target, request.params.id]
        response.json(prepareBuildsResponse(builds, request.params.type))
    })

    router.get("/builds/:target/:type", { request, response ->
        val builds = LocalCache[request.params.target]
        response.json(prepareBuildsResponse(builds, request.params.type))
    })

    router.get("/clean", { request, response ->
        LocalCache.clean()
        response.sendStatus(200)
    })

    router.get("/fill", { request, response ->
        LocalCache.fill()
        response.sendStatus(200)
    })

    // Main page.
    router.get("/", { request, response ->
        response.render("index")
    })

    return router
}

fun getAuth(user: String, password: String): String {
    val buffer = js("Buffer").from(user + ":" + password)
    val based64String = buffer.toString("base64")
    return "Basic " + based64String
}

fun sendGetRequest(url: String, user: String? = null, password: String? = null, jsonContentType: Boolean = false) : String {
    val request = require("sync-request")
    val headers = mutableListOf<Pair<String, String>>()
    if (user != null && password != null) {
        headers.add("Authorization" to getAuth(user, password))
    }
    if (jsonContentType) {
        headers.add("Accept" to "application/json")
    }
    val response = request("GET", url,
            json(
                "headers" to json(*(headers.toTypedArray()))
            )
    )
    if (response.statusCode != 200) {
        error("Error during getting response from $url\n" +
                "${response.getBody()}")
    }

    return response.getBody().toString()
}

fun sendUploadRequest(url: String, fileContent: String, user: String? = null, password: String? = null) {
    val request = require("sync-request")
    val headers = mutableListOf<Pair<String, String>>("Content-type" to "text/plain")
    if (user != null && password != null) {
        headers.add("Authorization" to getAuth(user, password))
    }
    val response = request("PUT", url,
            json(
                    "headers" to json(*(headers.toTypedArray())),
                    "body" to fileContent
            )
    )
    if (response.statusCode != 201) {
        error("Error during uploading to $url\n" +
                "${response}")
    }
}
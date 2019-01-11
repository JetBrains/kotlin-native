/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory

import org.jetbrains.report.json.*

import java.io.FileInputStream
import java.io.IOException
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.Properties

fun String.runCommand(workingDir: File = File("."),
                      timeoutAmount: Long = 60,
                      timeoutUnit: TimeUnit = TimeUnit.SECONDS): String? {
    return try {
        ProcessBuilder(*this.split("\\s".toRegex()).toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start().apply {
                    waitFor(timeoutAmount, timeoutUnit)
                }.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

class CommitsList(data: JsonElement): ConvertedFromJson {
    val commits: List<Triple<String, String, String>>
    init {
        if (data is JsonObject) {
            val changesElement = getRequiredField(data, "change")
            if (changesElement is JsonArray) {
                commits = arrayToList(changesElement.jsonArray, { index ->
                    if (getObjectOrNull(index) != null)
                        Triple(elementToString(getRequiredField((this.getObjectOrNull(index) as JsonObject), "version"), "version"),
                                elementToString(getRequiredField((this.getObjectOrNull(index) as JsonObject), "username"), "username"),
                                elementToString(getRequiredField((this.getObjectOrNull(index) as JsonObject), "webUrl"), "webUrl")
                        )
                    else null
                })
            } else {
                error("Change field is expected to be an array. Please, check source.")
            }
        } else {
            error("Commits description is expected to be a json object!")
        }
    }
}

/**
 * Task to produce regressions report and send it to slack. Requires a report with current benchmarks result
 * and path to analyzer tool
 *
 * @property currentBenchmarksReportFile  path to file with becnhmarks result
 * @property analyzer path to analyzer tool
 * @property fileNameForPreviousResults name of file where should be saved benchmarks results from previous build
 */
open class RegressionsReporter : DefaultTask() {

    val teamCityUrl = "http://buildserver.labs.intellij.net"
    val defaultBranch = "master"
    val slackUsers = mapOf("elena.lepilkina" to "elena.lepilkina")

    @Input
    lateinit var currentBenchmarksReportFile: String

    @Input
    var fileNameForPreviousResults = "previousReport.json"

    @Input
    lateinit var analyzer: String

    override fun configure(config: Closure<*>): Task {
        super.configure(config)
        return this
    }

    private fun tabUrl(buildId: String, buildTypeId: String, tab: String) =
            "$teamCityUrl/viewLog.html?buildId=$buildId&buildTypeId=$buildTypeId&tab=$tab"

    private fun testReportUrl(buildId: String, buildTypeId: String) =
            tabUrl(buildId, buildTypeId, "testsInfo")

    private fun buildsUrl(buildLocator: String) =
            "$teamCityUrl/app/rest/builds/?locator=$buildLocator"

    private fun artifactContentUrl(buildLocator: String, artifactPath: String) =
            "$teamCityUrl/app/rest/builds/$buildLocator/artifacts/content/$artifactPath"

    private fun previousBuildLocator(buildTypeId: String, branchName: String) =
            "buildType:id:$buildTypeId,branch:name:$branchName,status:SUCCESS,state:finished,count:1"

    private fun changesListUrl(buildId: String) =
            "$teamCityUrl/app/rest/changes/?locator=id:$buildId"


    private fun sendGet(url: String, username: String, password: String ) : String {
        val connection = URL(url).openConnection() as HttpURLConnection
        val auth = Base64.getEncoder().encode((username + ":" + password).toByteArray()).toString(Charsets.UTF_8)
        connection.addRequestProperty("Authorization", "Basic $auth")
        connection.setRequestProperty("Accept", "application/json");
        connection.connect()
        return connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
    }

    @TaskAction
    fun run() {
        // Get TeamCity properties.
        val teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE") ?:
            error("Can't load teamcity config!")
        val buildProperties = Properties()
        buildProperties.load(FileInputStream(teamcityConfig))

        val buildId = buildProperties.getProperty("teamcity.build.id")
        val buildTypeId = buildProperties.getProperty("teamcity.buildType.id")
        val user = buildProperties.getProperty("teamcity.auth.userId")
        val password = buildProperties.getProperty("teamcity.auth.password")
        val branch = buildProperties.getProperty("vcsroot.branch")

        val testReportUrl = testReportUrl(buildId, buildTypeId)

        // Get previous builds on branch.
        val builds = try {
            println("Build get request: ${buildsUrl("buildType:id:$buildTypeId,branch:name:$branch,status:SUCCESS,state:finished,count:1")}")
            sendGet(buildsUrl("buildType:id:$buildTypeId,branch:name:$branch,status:SUCCESS,state:finished,count:1"),
                    user, password)
        } catch (t: Throwable) {
            error("Try to get builds! TeamCity is unreachable!")
        }
        val previousBuildsExist = (JsonTreeParser.parse(builds) as JsonObject).getPrimitive("count").int != 0

        // Get changes description.
        val changes = try {
            sendGet(changesListUrl(buildId), user, password)
        } catch (t: Throwable) {
            error("Try to get commits! TeamCity is unreachable!")
        }
        val changesList = CommitsList(JsonTreeParser.parse(changes))
        val changesInfo = "Changes:\n" + buildString {
            changesList.commits.forEach { (version, user, url) ->
                append("    Change $version by @$user\n (details: $url)")
            }
        }

        // If branch differs from default compare to master if it's first build, otherwise to previous build on branch.
        val compareToBranch = if (previousBuildsExist) { branch } else { defaultBranch }

        // Get benchmarks results from last build on branch.
        val response = try {
            sendGet(artifactContentUrl(previousBuildLocator(buildTypeId, compareToBranch), currentBenchmarksReportFile),
                    user, password)
        } catch (t: Throwable) {
            error("No build to compare to!")
        }

        // Get compare to build.
        val compareToBuild = try {
            sendGet(buildsUrl(previousBuildLocator(buildTypeId, compareToBranch)), user, password)
        } catch (t: Throwable) {
            error("Try to get build! TeamCity is unreachable!")
        }

        val compareToBuildLink = with(JsonTreeParser.parse(builds) as JsonObject) {
            if (getPrimitive("count").int != 0) {
                error("No build to compare to!")
            }
            getArray("build").getObject(0).getPrimitive("webUrl").toString()
        }

        File(fileNameForPreviousResults).printWriter().use { out ->
            out.println(response)
        }

        // Generate comparasion report.
        val report = "$analyzer -s $currentBenchmarksReportFile $fileNameForPreviousResults".runCommand()

        // Send to channel or user directly.
        val session = SlackSessionFactory.createWebSocketSlackSession(buildProperties.getProperty("konan-reporter-token"))
        session.connect()
        val channel = if (branch == defaultBranch) {
            session.findChannelByName(buildProperties.getProperty("konan-channel-name"))
        } else {
            val developers = changesList.commits.filter{ (_, user, _) -> user in slackUsers }. map { (_, user, _) ->
                session.findUserByUserName(slackUsers[user])
            }.toTypedArray()
            val reply = session.openMultipartyDirectMessageChannel(*developers)
            reply.getReply().getSlackChannel()
        }
        session.sendMessage(channel, "${changesInfo}\nCompare to build:$compareToBuildLink\n" +
                "${report}\nBenchmarks statistics:$testReportUrl")
        session.disconnect()
    }
}
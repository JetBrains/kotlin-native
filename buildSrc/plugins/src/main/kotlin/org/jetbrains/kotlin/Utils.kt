package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager

import java.io.FileInputStream
import java.io.IOException
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import org.jetbrains.report.json.*

fun Project.platformManager() = findProperty("platformManager") as PlatformManager
fun Project.testTarget() = findProperty("target") as KonanTarget

/**
 * Ad-hoc signing of the specified path
 */
fun codesign(project: Project, path: String) {
    check(HostManager.hostIsMac, { "Apple specific code signing" })
    val (stdOut, stdErr, exitCode) = runProcess(executor = localExecutor(project), executable = "/usr/bin/codesign",
            args = listOf("--verbose", "-s", "-", path))
    check(exitCode == 0, {
        """
            |Codesign failed with exitCode: $exitCode
            |stdout: $stdOut
            |stderr: $stdErr
            """.trimMargin()
    })
}

// Run command line from string.
fun Array<String>.runCommand(workingDir: File = File("."),
                             timeoutAmount: Long = 60,
                             timeoutUnit: TimeUnit = TimeUnit.SECONDS): String {
    return try {
        ProcessBuilder(*this)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start().apply {
                    waitFor(timeoutAmount, timeoutUnit)
                }.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        error("Couldn't run command $this")
    }
}

fun String.splitCommaSeparatedOption(optionName: String) =
        split("\\s*,\\s*".toRegex()).map {
            if (it.isNotEmpty()) listOf(optionName, it) else listOf(null)
        }.flatten().filterNotNull()

data class Commit(val revision: String, val developer: String, val webUrlWithDescription: String)

val teamCityUrl = "http://buildserver.labs.intellij.net"

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
                            elementToString(getRequiredField("username"), "username"),
                            elementToString(getRequiredField("webUrl"), "webUrl")
                    )
                }
            }
        } ?: listOf<Commit>()
    }
}

fun buildsUrl(buildLocator: String) =
        "$teamCityUrl/app/rest/builds/?locator=$buildLocator"

fun getBuild(buildLocator: String, user: String, password: String) =
        try {
            sendGetRequest(buildsUrl(buildLocator), user, password)
        } catch (t: Throwable) {
            error("Try to get build! TeamCity is unreachable!")
        }

fun sendGetRequest(url: String, username: String? = null, password: String? = null) : String {
    val connection = URL(url).openConnection() as HttpURLConnection
    if (username != null && password != null) {
        val auth = Base64.getEncoder().encode((username + ":" + password).toByteArray()).toString(Charsets.UTF_8)
        connection.addRequestProperty("Authorization", "Basic $auth")
    }
    connection.setRequestProperty("Accept", "application/json");
    connection.connect()
    return connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
}

fun getBuildProperty(buildJsonDescription: String, property: String) =
        with(JsonTreeParser.parse(buildJsonDescription) as JsonObject) {
            if (getPrimitive("count").int == 0) {
                error("No build information on TeamCity for $buildJsonDescription!")
            }
            (getArray("build").getObject(0).getPrimitive(property) as JsonLiteral).unquoted()
        }

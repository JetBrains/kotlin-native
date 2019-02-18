package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import java.io.File

fun Project.platformManager() = findProperty("platformManager") as PlatformManager
fun Project.testTarget() = findProperty("target") as KonanTarget

val Project.verboseTest
    get() = hasProperty("test_verbose")

val Project.testOutputLocal
    get() = (findProperty("testOutputLocal") as File).toString()

/**
 * Ad-hoc signing of the specified path
 */
fun codesign(project: Project, path: String) {
    check(HostManager.hostIsMac) { "Apple specific code signing" }
    val (stdOut, stdErr, exitCode) = runProcess(executor = localExecutor(project), executable = "/usr/bin/codesign",
            args = listOf("--verbose", "-s", "-", path))
    check(exitCode == 0) { """
        |Codesign failed with exitCode: $exitCode
        |stdout: $stdOut
        |stderr: $stdErr
        """.trimMargin()
    }
}
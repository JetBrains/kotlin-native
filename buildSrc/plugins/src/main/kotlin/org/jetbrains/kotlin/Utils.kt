package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager

fun Project.platformManager() = findProperty("platformManager") as PlatformManager
fun Project.testTarget() = findProperty("target") as KonanTarget

fun Project.knProject(): Project {
    val rootProject = project.rootProject
    val knProject = rootProject.findProject(":kotlin-native")
    if (knProject != null) {
        return knProject
    } else return rootProject
}

/**
 * Ad-hoc signing of the specified path
 */
fun codesign(project: Project, path: String) {
    check(HostManager.hostIsMac, { "Apple specific code signing" } )
    val (stdOut, stdErr, exitCode) = runProcess(executor = localExecutor(project), executable = "/usr/bin/codesign",
            args = listOf("--verbose", "-s", "-", path))
    check(exitCode == 0, { """
            |Codesign failed with exitCode: $exitCode
            |stdout: $stdOut
            |stderr: $stdErr
            """.trimMargin()
    })
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import java.io.File

//region Project properties.

val Project.platformManager
    get() = findProperty("platformManager") as PlatformManager

val Project.testTarget
    get() = findProperty("target") as KonanTarget

val Project.verboseTest
    get() = hasProperty("test_verbose")

val Project.testOutputLocal
    get() = (findProperty("testOutputLocal") as File).toString()

val Project.testOutputStdlib
    get() = (findProperty("testOutputStdlib") as File).toString()

val Project.testOutputFramework
    get() = (findProperty("testOutputFramework") as File).toString()

@Suppress("UNCHECKED_CAST")
val Project.globalTestArgs: List<String>
    get() = with(findProperty("globalTestArgs")) {
            if (this is Array<*>) this.toList() as List<String>
            else this as List<String>
    }

//endregion

/**
 * Ad-hoc signing of the specified path.
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

/**
 * Creates a list of file paths to be compiled from the given list with regard to exclude list.
 */
fun Project.getFilesToCompile(compile: List<String>, exclude: List<String>): List<String> {
    // convert exclude list to paths
    val excludeFiles = exclude.map { project.file(it).absolutePath }.toList()

    // create list of tests to compile
    return compile.flatMap { f ->
        project.file(f)
                .walk()
                .filter { it.isFile && it.name.endsWith(".kt") && !excludeFiles.contains(it.absolutePath) }
                .map(File::getAbsolutePath)
                .asIterable()
    }
}

//region Task dependency.

fun Project.setDistDependencyFor(taskName: String) {
    project.setDistDependencyFor(project.tasks.getByName(taskName))
}

fun Project.setDistDependencyFor(t: Task) {
    val rootTasks = project.rootProject.tasks
    // We don't build the compiler if a custom dist path is specified.
    if (project.findProperty("useCustomDist") != null) {
        t.dependsOn(rootTasks.getByName("dist"))
        val target = project.testTarget
        if (target != HostManager.host) {
            // if a test_target property is set then tests should depend on a crossDist
            // otherwise runtime components would not be build for a target.
            t.dependsOn(rootTasks.getByName("${target.name}CrossDist"))
        }
    }
}

//endregion
/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task

import org.jetbrains.kotlin.konan.target.HostManager

import java.io.File

fun Project.createStdlibTest(name: String, configure: (KonanGTestRunner) -> Unit = {}): KonanGTestRunner =
        project.tasks.create("execute${name.capitalize()}", KonanGTestRunner::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            configure(this)

            // Configure test task.
            val testOutput = (project.findProperty("testOutputStdlib") as? File)?.toString()
                    ?: throw RuntimeException("Output directory testOutputStdlib is not set")
            val target = project.testTarget()
            executable = "$testOutput/${target.name}/$name.${target.family.exeSuffix}"
            useFilter = false
            testLogger = RunnerLogger.GTEST

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
            finalizedBy("resultsTask")
        }

fun Project.setDistDependencyFor(taskName: String) {
    project.setDistDependencyFor(project.tasks.getByName(taskName))
}

fun Project.setDistDependencyFor(t: Task) {
    val rootTasks = project.rootProject.tasks
    // We don't build the compiler if a custom dist path is specified.
    if (project.findProperty("useCustomDist") != null) {
        t.dependsOn(rootTasks.getByName("dist"))
        val target = project.testTarget()
        if (target != HostManager.host) {
            // if a test_target property is set then tests should depend on a crossDist
            // otherwise runtime components would not be build for a target.
            t.dependsOn(rootTasks.getByName("${target.name}CrossDist"))
        }
    }
}

fun Project.createStandaloneTest(name: String, config: Closure<*>): KonanStandaloneTestRunner =
        project.tasks.create("execute${name.capitalize()}", KonanStandaloneTestRunner::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)

            // Configure test task.
            val testOutput = (project.findProperty("testOutputLocal") as? File)?.toString()
                    ?: throw RuntimeException("Output directory testOutputLocal is not set")
            val target = project.testTarget()
            if (enableKonanAssertions) {
                if (flags != null) flags!!.add("-ea")
                else flags = mutableListOf("-ea")
            }
            executable = "$testOutput/${target.name}/$name.${target.family.exeSuffix}"

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
        }
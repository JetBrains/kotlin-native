/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Project

// TODO: join this

fun Project.createStdlibTest(name: String, configure: (KonanGTestRunner) -> Unit = {}): KonanGTestRunner =
        project.tasks.create("execute${name.capitalize()}", KonanGTestRunner::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            configure(this)

            // Configure test task.
            val testOutput = project.testOutputStdlib
            val target = project.testTarget
            executable = "$testOutput/$name/${target.name}/$name.${target.family.exeSuffix}"
            useFilter = false
            testLogger = KonanTestRunner.Logger.GTEST

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
            finalizedBy("resultsTask")
        }

fun Project.createStandaloneTest(name: String, config: Closure<*>): KonanStandaloneTestRunner =
        project.tasks.create("execute${name.capitalize()}", KonanStandaloneTestRunner::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)

            // Configure test task.
            val testOutput = project.testOutputLocal
            val target = project.testTarget
            executable = "$testOutput/$name/${target.name}/$name.${target.family.exeSuffix}"

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
        }

fun Project.createInteropTest(name: String, config: Closure<*>): KonanInteropTestRunner =
        project.tasks.create("execute${name.capitalize()}", KonanInteropTestRunner::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)

            // Configure test task.
            val testOutput = project.testOutputLocal
            val target = project.testTarget
            executable = "$testOutput/$name/${target.name}/$name.${target.family.exeSuffix}"

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
        }

fun Project.createLinkTest(name: String, config: Closure<*>): KonanLinkTestRunner =
        project.tasks.create("execute${name.capitalize()}", KonanLinkTestRunner::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)

            // Configure test task.
            val testOutput = project.testOutputLocal
            val target = project.testTarget
            executable = "$testOutput/$name/${target.name}/$name.${target.family.exeSuffix}"

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
        }

fun Project.createDynamicTest(name: String, config: Closure<*>): KonanDynamicTestRunner =
        project.tasks.create(name, KonanDynamicTestRunner::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)

            // Configure test task.
            val testOutput = project.testOutputLocal
            val target = project.testTarget
            executable = "$testOutput/$name/${target.name}/$name.${target.family.exeSuffix}"

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
        }

inline fun <reified T: KonanTestRunner> Project.createTest(name: String, config: Closure<*>): T =
        project.tasks.create(name, T::class.java).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)

            // Configure test task.
            val testOutput = project.testOutputLocal
            val target = project.testTarget
            executable = "$testOutput/$name/${target.name}/$name.${target.family.exeSuffix}"

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}${target.name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
        }
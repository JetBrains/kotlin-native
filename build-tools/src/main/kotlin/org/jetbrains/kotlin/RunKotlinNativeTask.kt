/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

open class RunKotlinNativeTask @Inject constructor(
        private val runTask: AbstractExecTask<*>, private val linkTaskName: String
) : DefaultTask() {
    @Input
    var buildType = "RELEASE"
    @Input
    @Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    var filter: String = ""
    @Input
    @Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    var filterRegex: String = ""
    @Input
    lateinit var outputFileName: String

    override fun configure(configureClosure: Closure<Any>): Task {
        val task = super.configure(configureClosure)
        this.dependsOn += linkTaskName
        this.finalizedBy("konanJsonReport")
        return task
    }

    fun depends(taskName: String) {
        this.dependsOn += taskName
    }

    @TaskAction
    fun run() {
        var output = ByteArrayOutputStream()
        project.exec {
            it.executable = runTask.executable
            it.args("list")
            it.standardOutput = output
        }
        val benchmarks = output.toString().lines()
        val filterArgs = filter.splitCommaSeparatedOption("-f")
        val filterRegexArgs = filterRegex.splitCommaSeparatedOption("-fr")
        val regexes = filterRegexArgs.map { it.toRegex() }
        val benchmarksToRun = if (filterArgs.isNotEmpty() || regexes.isNotEmpty()) {
            benchmarks.filter { benchmark -> benchmark in filterArgs || regexes.any { it.matches(benchmark) } }
        } else benchmarks.filter { !it.isEmpty() }
        val results = benchmarksToRun.map { benchmark ->
            output = ByteArrayOutputStream()
            project.exec {
                it.commandLine = runTask.commandLine
                it.args("-f", benchmark)
                it.standardOutput = output
            }
            output.toString().removePrefix("[").removeSuffix("]")
        }

        File(outputFileName).printWriter().use { out ->
            out.println("[${results.joinToString(",")}]")
        }

    }

    internal fun emptyConfigureClosure() = object : Closure<Any>(this) {
        override fun call(): RunKotlinNativeTask {
            return this@RunKotlinNativeTask
        }
    }

    fun configureClosure(fileName: String) = object : Closure<Any>(this) {
        override fun call(): RunKotlinNativeTask {
            val task = this@RunKotlinNativeTask
            task.outputFileName = fileName
            return task
        }
    }
}

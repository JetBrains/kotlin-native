/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFiles
import java.io.File

/**
 * Task to find out collisions before producing fat jar.
 *
 * @property configurations added to fat jar configurations
 * @property ignoredFiles excluded from analysis files
 * @property resolvingRules rules to resolve files
 * @property resolvingRulesWithRegexes rules to resolve files described with regexes
 * @property librariesWithIgnoredClassCollisions libraries which collision in class files are ignored
 */
open class CollisionDetector : DefaultTask() {
    @InputFiles
    var configurations = listOf<Configuration>()
    val ignoredFiles = mutableListOf<String>()
    val resolvingRules = mutableMapOf<String, String>()
    val resolvingRulesWithRegexes = mutableMapOf<Regex, String>()
    val resolvedConflicts = mutableMapOf<String, File>()
    val librariesWithIgnoredClassCollisions = mutableListOf<String>()

    // Key - filename, value - jar file contained it.
    private val filesInfo = mutableMapOf<String, String>()

    @TaskAction
    fun run() {
        configurations.forEach { configuration ->
            configuration.files.forEach { processedFile ->
                if (processedFile.name.endsWith(".jar")) {
                    project.zipTree("${processedFile.absolutePath}").matching{ it.exclude(ignoredFiles) }.forEach {
                        val outputPath = it.absolutePath.substringAfter(processedFile.name).substringAfter("/")
                        val rule = resolvingRules.getOrElse(outputPath) {
                            var foundValue: String? = null
                            resolvingRulesWithRegexes.forEach {
                                if (it.key.matches(outputPath)) {
                                    foundValue = it.value
                                }
                            }
                            foundValue
                        }

                        if (rule != null && processedFile.name.startsWith(rule)) {
                            resolvedConflicts[outputPath] = processedFile
                        }

                        if (outputPath in filesInfo.keys) {
                            // Skip class files from ignored libraries if version of libraries had collision are the same.
                            var ignoreJar = false
                            val versionRegex = "\\d+\\.\\d+(\\.\\d+)?-\\w+-\\d+".toRegex()
                            val currentVersion = versionRegex.find(processedFile.name)?.groupValues?.get(0)
                            val collisionLibVersion = versionRegex.find(filesInfo[outputPath]!!)?.groupValues?.get(0)
                            if (outputPath.endsWith(".class") && currentVersion == collisionLibVersion) {
                                librariesWithIgnoredClassCollisions.forEach {
                                    if (processedFile.name.startsWith(it)) {
                                        ignoreJar = true
                                    }
                                }
                            }
                            if (rule == null && !ignoreJar)
                                error("Collision is detected. File $outputPath is found in ${filesInfo[outputPath]} and ${processedFile.name}")
                        } else {
                            filesInfo[outputPath] = processedFile.name
                        }
                    }
                }
            }
        }
    }
}
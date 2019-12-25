/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

import java.io.File
import javax.inject.Inject

open class CompileToBitcode @Inject constructor(@InputDirectory val srcRoot: File,
                                                val folderName: String,
                                                val target: String) : DefaultTask() {
    enum class Language {
        C, CPP
    }

    val compilerArgs = mutableListOf<String>()
    val linkerArgs = mutableListOf<String>()
    val excludeFiles = mutableListOf<String>()
    var srcDir = File(srcRoot, "cpp")
    var headersDir = File(srcRoot, "headers")
    var skipLinkagePhase = false
    var excludedTargets = mutableListOf<String>()
    var language = Language.CPP

    private val targetDir by lazy { File(project.buildDir, target) }

    private val objDir by lazy { File(targetDir, folderName) }

    private val KonanTarget.isMINGW
        get() = this.family == Family.MINGW

    @OutputFile
    val outFile = File(targetDir, "${folderName}.bc")

    @TaskAction
    fun compile() {
        if (target in excludedTargets) return
        objDir.mkdirs()
        val plugin = project.convention.getPlugin(ExecClang::class.java)
        val commonFlags = listOf("-c", "-emit-llvm", "-I$headersDir")
        val (executable, defaultFlags, srcFilesPatterns) =
                when (language) {
                    Language.C -> Triple("clang",
                            // Used flags provided by original build of allocator C code.
                            commonFlags + listOf("-std=gnu11", "-O3", "-Wall", "-Wextra", "-Wno-unknown-pragmas",
                                    "-ftls-model=initial-exec"),
                            listOf("**/*.c"))
                    Language.CPP -> Triple("clang++",
                            commonFlags + listOfNotNull("-std=c++14", "-Werror", "-O2",
                                    "-fPIC".takeIf { !HostManager().targetByName(target).isMINGW }),
                            listOf("**/*.cpp", "**/*.mm"))
                }

        plugin.execKonanClang(target, Action {
            it.workingDir = objDir
            it.executable = executable
            it.args = defaultFlags + compilerArgs +
                    project.fileTree(srcDir) {
                        it.include(srcFilesPatterns)
                        it.exclude(excludeFiles)
                    }.files.map { it.absolutePath }
        })

        if (!skipLinkagePhase) {
            project.exec {
                val llvmDir = project.findProperty("llvmDir")
                it.executable = "$llvmDir/bin/llvm-link"
                it.args = listOf("-o", outFile.absolutePath) + linkerArgs +
                        project.fileTree(objDir) {
                            it.include("**/*.bc")
                        }.files.map { it.absolutePath }
            }
        }
    }
}

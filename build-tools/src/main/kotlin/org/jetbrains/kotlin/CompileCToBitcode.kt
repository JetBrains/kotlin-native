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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

import java.io.File
import javax.inject.Inject

open class CompileCToBitcode @Inject constructor(@InputDirectory val srcRoot: File,
                                                 val folderName: String,
                                                 val target: String) : DefaultTask() {
    val compilerArgs = mutableListOf<String>()
    val linkerArgs = mutableListOf<String>()
    val excludeFiles = mutableListOf<String>()
    val srcDir = File(srcRoot, "c")
    val headersDir = File(srcDir, "include")
    var skipLinkagePhase = false
    var excludedTargets = mutableListOf<String>()

    private val targetDir by lazy { File(project.buildDir, target) }

    private val objDir by lazy { File(targetDir, folderName) }

    @OutputFile
    val outFile = File(targetDir, "${folderName}.bc")

    @TaskAction
    fun compile() {
        if (target in excludedTargets) return
        objDir.mkdirs()
        val plugin = project.convention.getPlugin(ExecClang::class.java)
        plugin.execKonanClang(target, Action {
            it.workingDir = objDir
            it.executable = "clang"
            val picFlags = if (HostManager().targetByName(target).family != Family.MINGW) listOf("-fPIC", "-DPIC") else emptyList()
            it.args = listOf("-std=gnu11", "-O3", "-c", "-emit-llvm", "-I$headersDir", "-Wall",
                    "-Wextra", "-Wshorten-64-to-32", "-Wsign-compare", "-Wundef", "-Wno-format-zero-length",
                    "-funroll-loops", "-D_REENTRANT") + picFlags + compilerArgs +
                    project.fileTree(srcDir) {
                        it.include("**/*.c")
                        it.exclude(excludeFiles)
                    }.files.map{ it.absolutePath }
        })

        if (skipLinkagePhase) {
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

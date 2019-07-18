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
import org.gradle.process.ExecSpec

import java.io.File
import javax.inject.Inject

open class CompileCToBitcode @Inject constructor(val folderName: String, val target: String,  @InputDirectory val srcRoot: File, val headersDir: File,
                                                 val excludeFiles: List<String>) : DefaultTask() {

    val compilerArgs = mutableListOf<String>()
    val linkerArgs = mutableListOf<String>()

    private val srcDir = File(srcRoot, "c")

    private val targetDir = File(project.buildDir, target)

    private val objDir = File(targetDir, folderName)


    @OutputFile
    val outFile = File(targetDir, "${folderName}.bc")

    fun addCompilerArgs(vararg args: String) {
        compilerArgs.addAll(args)
    }

    fun addLinkerArgs(vararg args: String) {
        linkerArgs.addAll(args)
    }

    @TaskAction
    fun compile() {
        objDir.mkdirs()
        val plugin = project.convention.getPlugin(ExecClang::class.java)
        plugin.execKonanClang(this.target, Action<ExecSpec> {
            it.workingDir = objDir
            it.executable = "clang"
            it.args = listOf("-std=gnu11", "-O3", "-I$headersDir", "-c", "-emit-llvm", "-Wall", "-Wextra", "-Wno-unknown-pragmas", "-ftls-model=initial-exec") + compilerArgs +
                    project.fileTree(srcDir) {
                it.include("**/*.c")
                    it.exclude(excludeFiles)}.files.map{ it.absolutePath }
            println(it.commandLine)
        })

        /*project.exec {
            val llvmDir = project.findProperty("llvmDir")
            it.executable = "$llvmDir/bin/llvm-link"
            it.args = listOf("-o", outFile.absolutePath) + linkerArgs
            project.fileTree(objDir) {
                it.include("*.bc")
            }.files.map { it.absolutePath }

            println(it.commandLine)
        }*/
    }
}

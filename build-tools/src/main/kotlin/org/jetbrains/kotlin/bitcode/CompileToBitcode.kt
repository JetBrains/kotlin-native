/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

open class CompileToBitcode @Inject constructor(
        val srcRoot: File,
        val folderName: String,
        val target: String,
        val outputGroup: String
) : DefaultTask() {

    enum class Language {
        C, CPP
    }

    // Compiler args are part of compilerFlags so we don't register them as an input.
    val compilerArgs = mutableListOf<String>()
    @Input
    val linkerArgs = mutableListOf<String>()
    var excludeFiles: List<String> = listOf(
            "**/*Test.cpp",
            "**/*Test.mm",
    )
    var includeFiles: List<String> = listOf(
            "**/*.cpp",
            "**/*.mm"
    )

    // Source files and headers are registered as inputs by the `inputFiles` and `headers` properties.
    var srcDirs: FileCollection = project.files(srcRoot.resolve("cpp"))
    var headersDirs: FileCollection = project.files(srcRoot.resolve("headers"))

    @Input
    var skipLinkagePhase = false

    @Input
    var language = Language.CPP

    private val targetDir by lazy { project.buildDir.resolve("bitcode/$outputGroup/$target") }

    val objDir by lazy { File(targetDir, folderName) }

    private val KonanTarget.isMINGW
        get() = this.family == Family.MINGW

    val executable
        get() = when (language) {
            Language.C -> "clang"
            Language.CPP -> "clang++"
        }

    @get:Input
    val compilerFlags: List<String>
        get() {
            val commonFlags = listOf("-c", "-emit-llvm", "-MMD") + headersDirs.map { "-I$it" }
            val languageFlags = when (language) {
                Language.C ->
                    // Used flags provided by original build of allocator C code.
                    listOf("-std=gnu11", "-O3", "-Wall", "-Wextra", "-Werror")
                Language.CPP ->
                    listOfNotNull("-std=c++14", "-Werror", "-O2",
                            "-Wall", "-Wextra",
                            "-Wno-unused-parameter",  // False positives with polymorphic functions.
                            "-Wno-unused-function",  // TODO: Enable this warning when we have C++ runtime tests.
                            "-fPIC".takeIf { !HostManager().targetByName(target).isMINGW })
            }
            return commonFlags + languageFlags + compilerArgs
        }

    @get:SkipWhenEmpty
    @get:InputFiles
    val inputFiles: Iterable<File>
        get() {
            return srcDirs.flatMap { srcDir ->
                project.fileTree(srcDir) {
                    it.include(includeFiles)
                    it.exclude(excludeFiles)
                }.files
            }
        }

    @get:OutputFiles
    protected val dependencyFiles: Iterable<File>
        get() = inputFiles.map {
            objDir.resolve("${it.nameWithoutExtension}.d")
        }

    // TODO: When building for the very first time this will be empty (no dependency files are generated), but for
    // the second this will be non-empty and make the task dirty. For subsequent runs everything runs correctly.
    @get:InputFiles
    protected val headers: Iterable<File>
        get() {
            val headers = mutableSetOf<File>()
            val parseMakefileDependency = { file: File, input: String ->
                class Parser {
                    var pos = 0

                    val eof: Boolean
                        get() = pos >= input.length

                    fun skipPrefix(prefix: String) {
                        if (input.subSequence(pos, pos + prefix.length) != prefix)
                            error("Invalid dependency output")
                        pos += prefix.length
                    }

                    fun isSpace(pos: Int) =
                        when (input[pos]) {
                            ' ', '\n', '\r', '\t' -> true
                            else -> false
                        }

                    fun isEscape(pos: Int) =
                        when (input[pos]) {
                            '\\' -> true
                            else -> false
                        }

                    fun skipSpaces() {
                        while (!eof && (isSpace(pos) || (isEscape(pos) && pos + 1 < input.length && isSpace(pos + 1)))) {
                            pos++
                        }
                    }

                    fun readFileName(): String {
                        val builder = StringBuilder()
                        while (!eof && !isSpace(pos)) {
                            if (!isEscape(pos)) {
                                builder.append(input[pos])
                            } else {
                                ++pos
                                if (!eof) {
                                    builder.append(input[pos])
                                }
                            }
                            ++pos
                        }
                        return builder.toString()
                    }
                }
                val p = Parser()
                p.skipPrefix("${file.nameWithoutExtension}.o:")
                p.skipSpaces()
                while (!p.eof) {
                    val filename = p.readFileName()
                    if (filename != file.absolutePath)
                        headers.add(File(filename))
                    p.skipSpaces()
                }
            }
            for (inputFile in inputFiles) {
                val dependencyFile = objDir.resolve("${inputFile.nameWithoutExtension}.d")
                if (dependencyFile.exists()) {
                    parseMakefileDependency(inputFile, dependencyFile.readText())
                }
            }
            return headers
        }

    @OutputFile
    val outFile = File(targetDir, "${folderName}.bc")

    @TaskAction
    fun compile() {
        objDir.mkdirs()
        val plugin = project.convention.getPlugin(ExecClang::class.java)

        plugin.execKonanClang(target) {
            it.workingDir = objDir
            it.executable = executable
            it.args = compilerFlags + inputFiles.map { it.absolutePath }
        }

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
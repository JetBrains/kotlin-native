/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.konan.file.File

fun generatePlatformLibraries(args: Array<String>) {
    var inputDirectory: File? = null
    var outputDirectory: File? = null
    var target: String? = null
    var index = 0
    var saveTemps = false
    while (index < args.size) {
        when (args[index++]) {
            "-target" ->
                target = args[index++]
            "-output" -> {
                if (outputDirectory != null) throw Error("Only one output directory is allowed")
                outputDirectory = File(args[index++])
            }
            "-input" -> {
                if (inputDirectory != null) throw Error("Only one input directory is allowed")
                inputDirectory = File(args[index++])
            }
            "-save-temps" ->
                saveTemps = true
        }
    }
    if (target == null) throw Error("-target argument is required")
    if (inputDirectory == null) throw Error("-input argument is required")
    if (outputDirectory == null) throw Error("-output argument is required")

    if (!inputDirectory.exists) throw Error("input directory doesn't exist")
    if (!outputDirectory.exists) {
        outputDirectory.mkdirs()
    }

    generatePlatformLibraries(target, inputDirectory, outputDirectory, saveTemps)
}

private class DefFile(val name: String, val depends: MutableList<DefFile>) {
    fun dependsOn(defFile: DefFile): Boolean {
        if (defFile == this) return true
        for (dependency in this.depends) {
            if (defFile.dependsOn(dependency)) return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        return other is DefFile && other.name == name
    }
    override fun toString(): String = "$name: [${depends.joinToString(separator = ", ") { it.name }}]"
}

private fun topoSort(defFiles: List<DefFile>): List<DefFile> {
    // Do DFS toposort.
    val markGray = mutableSetOf<DefFile>()
    val markBlack = mutableSetOf<DefFile>()
    val result = mutableListOf<DefFile>()

    fun visit(def: DefFile) {
        if (markBlack.contains(def)) return
        if (markGray.contains(def)) throw Error("$def is part of cycle")
        markGray += def
        def.depends.forEach {
            visit(it)
        }
        markGray -= def
        markBlack += def
        result += def
    }

    var index = 0
    while (markBlack.size < defFiles.size) {
        visit(defFiles[index++])
    }
    return result
}

private fun generatePlatformLibraries(target: String, inputDirectory: File, outputDirectory: File, saveTemps: Boolean) {
    println("generate platform libraries from $inputDirectory to $outputDirectory for $target")
    // Build dependencies graph.
    val defFiles = mutableMapOf<String, DefFile>()
    val dependsRegex = Regex("^depends = (.*)")
    inputDirectory.listFilesOrEmpty.filter { it.extension == "def" }.forEach { file ->
        val name = file.name.split(".").also { assert(it.size == 2) }[0]
        val def = defFiles.getOrPut(name) {
            DefFile(name, mutableListOf())
        }
        file.forEachLine { line ->
            val match = dependsRegex.matchEntire(line)
            if (match != null) {
                match.groupValues[1].split(" ").forEach { dependency ->
                    def.depends.add(defFiles.getOrPut(dependency) {
                        DefFile(dependency, mutableListOf())
                    })
                }
            }
        }
    }
    val sorted = topoSort(defFiles.values.toList())
    // Now run interop tool on toposorted dependencies.
    sorted.forEach { def ->
        val file = File("$inputDirectory/${def.name}.def")
        val args = arrayOf("-o", "$outputDirectory/${def.name}.klib",
                "-target", target,
                "-def", file.absolutePath,
                "-compiler-option", "-fmodules-cache-path=$outputDirectory/clangModulesCache",
                "-no-default-libs", "-no-endorsed-libs", "-Xpurge-user-libs",
                *def.depends.flatMap { listOf("-l", "$outputDirectory/${it.name}.klib") }.toTypedArray())
        println("Processing ${def.name}...")
        K2Native.mainNoExit(invokeInterop("native", args))
        if (!saveTemps) {
            File("$outputDirectory/${def.name}.klib-build").deleteRecursively()
        }
    }
}
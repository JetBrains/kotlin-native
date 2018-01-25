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
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.file.*

typealias StaticLibrary = String

internal fun produceObjectFiles(context: Context, bitcodeFiles: List<BitcodeFile>) =
        CompilationStage(context).produceObjectFiles(bitcodeFiles)

internal fun produceStaticLibrary(context: Context, bitcodeFiles: List<BitcodeFile>) =
        CompilationStage(context).produceStaticLibrary(bitcodeFiles)

private class CompilationStage(val context: Context) {

    val config = context.config.configuration
    val target = context.config.targetManager.target

    private val distribution = context.config.distribution
    private val platform = determinePlatformFlags(target, distribution)
    private val optimize = config.get(KonanConfigKeys.OPTIMIZATION) ?: false
    private val debug = config.get(KonanConfigKeys.DEBUG) ?: false
    private val dynamic = context.config.produce == CompilerOutputKind.DYNAMIC ||
            context.config.produce == CompilerOutputKind.FRAMEWORK

    fun produceObjectFiles(bitcodeFiles: List<BitcodeFile>): List<ObjectFile> =
            listOf(when {
                target == KonanTarget.WASM32 -> bitcodeToWasm(bitcodeFiles)
                bitcodeFiles.size == 1 -> llc(opt(bitcodeFiles[0]))
                else -> llc(opt(link(bitcodeFiles)))
            })

    fun produceStaticLibrary(bitcodeFiles: List<BitcodeFile>): StaticLibrary =
            bitcodeFiles.map {
                when (target) {
                    KonanTarget.WASM32 -> bitcodeToWasm(bitcodeFiles)
                    else -> llc(opt(it))
                }
            }.let { llvmAr(it) }

    private fun link(files: List<BitcodeFile>): BitcodeFile {
        val linked = temporary("linked", ".o")
        val args = listOf(*files.toTypedArray(), "-o", linked)
        hostLlvmTool("llvm-link", args)
        return linked
    }

    private fun llvmAr(files: List<ObjectFile>): StaticLibrary {
        val output = temporaryName("lib", ".a")
        val args = listOf("rcs", output, *files.toTypedArray())
        hostLlvmTool("llvm-ar", args)
        return output
    }

    private fun llc(file: BitcodeFile): ObjectFile {
        val compiled = temporary("compiled", ".o")
        val flags = when {
            optimize -> platform.llvmLtoOptFlags
            debug    -> platform.llvmDebugOptFlags
            else     -> platform.llvmLtoNooptFlags
        }
        val args = listOf(file, *flags.toTypedArray(), "-filetype=obj", "-o", compiled)
        hostLlvmTool("llc", args)
        return compiled
    }

    // TODO: use properties
    private fun opt(file: BitcodeFile): BitcodeFile {
        val optimized = temporary("optimized", ".bc")
        val flags = when {
            optimize    -> "-O3"
            debug       -> "-O0"
            else        -> "-O1"
        }
        val args = listOf(file, flags, "-o", optimized)
        hostLlvmTool("opt", args)
        return optimized
    }

    private fun MutableList<String>.addNonEmpty(elements: List<String>) {
        addAll(elements.filter { !it.isEmpty() })
    }

    private fun runTool(command: List<String>) = runTool(*command.toTypedArray())
    private fun runTool(vararg command: String) =
            Command(*command)
                    .logWith(context::log)
                    .execute()

    private fun llvmLto(files: List<BitcodeFile>): ObjectFile {
        val combined = temporary("combined", ".o")

        val tool = distribution.llvmLto
        val command = mutableListOf(tool, "-o", combined)
        command.addNonEmpty(platform.llvmLtoFlags)
        when {
            optimize -> command.addNonEmpty(platform.llvmLtoOptFlags)
            debug    -> command.addNonEmpty(platform.llvmDebugOptFlags)
            else     -> command.addNonEmpty(platform.llvmLtoNooptFlags)
        }
        command.addNonEmpty(platform.llvmLtoDynamicFlags)
        command.addNonEmpty(files)
        runTool(command)

        return combined
    }

    private fun temporary(name: String, suffix: String): String {
        val temporaryFile = createTempFile(name, suffix)
        temporaryFile.deleteOnExit()
        return temporaryFile.absolutePath
    }

    // TODO: refactor
    private fun temporaryName(name: String, suffix: String): String {
        val tempFile = createTempFile(name, suffix)
        tempFile.delete()
        return tempFile.absolutePath
    }

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.targetToolchain}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, args: List<String>) {
        val absoluteToolName = "${distribution.llvmBin}/$tool"
        val command = listOf(absoluteToolName) + args
        runTool(command)
    }

    private fun bitcodeToWasm(bitcodeFiles: List<BitcodeFile>): String {
        val combinedBc = temporary("combined", ".bc")
        hostLlvmTool("llvm-link", bitcodeFiles + listOf("-o", combinedBc))

        val combinedS = temporary("combined", ".s")
        targetTool("llc", combinedBc, "-o", combinedS)

        val s2wasmFlags = platform.s2wasmFlags.toTypedArray()
        val combinedWast = temporary( "combined", ".wast")
        targetTool("s2wasm", combinedS, "-o", combinedWast, *s2wasmFlags)

        val combinedWasm = temporary( "combined", ".wasm")
        val combinedSmap = temporary( "combined", ".smap")
        targetTool("wasm-as", combinedWast, "-o", combinedWasm, "-g", "-s", combinedSmap)

        return combinedWasm
    }
}
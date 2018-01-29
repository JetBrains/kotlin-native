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

import llvm.LLVMLinkModules2
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.backend.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

internal interface CompilerOutputProducer {
    val context: Context

    fun produce()
}

internal class BitcodeProducer(override val context: Context) : CompilerOutputProducer {
    override fun produce() {
        val llvmModule = context.llvmModule!!
        val output = context.config.outputFile
        LLVMWriteBitcodeToFile(llvmModule, output)
    }
}

// TODO: library extension should be platform dependent
internal class LibraryProducer(override val context: Context) : CompilerOutputProducer {

    private val phaser = PhaseManager(context)
    lateinit var libraryWriter: KonanLibraryWriter

    override fun produce() {
        val llvmModule = context.llvmModule!!
        val config = context.config.configuration
        val libraryName = context.config.moduleId

        // TODO: create static library first then put it into klib
        val bitcode = produceLibrary(context, config, llvmModule)

        phaser.phase(KonanPhase.OBJECT_FILES) {
            // stubs
            context.config.nativeLibraries.forEach {
                addStaticLibrary(it)
            }
            addStaticLibrary(bitcode)
        }
        libraryWriter.commit()
    }

    private fun addStaticLibrary(bitcodeFileName: String) {
        if (!context.isWasm) {
            val stubs = File(File(bitcodeFileName).name + ".a")
            File(produceStaticLibrary(context, listOf(bitcodeFileName))).copyTo(stubs)
            libraryWriter.addIncludedBinary(stubs.name)
        }
    }

    private fun produceLibrary(context: Context, config: CompilerConfiguration, llvmModule: LLVMModuleRef): String {
        val output = context.config.outputName
        val neededLibraries = context.llvm.librariesForLibraryManifest
        val libraryName = context.config.moduleId
        val abiVersion = context.config.currentAbiVersion
        val target = context.config.targetManager.target
        val nopack = config.getBoolean(KonanConfigKeys.NOPACK)
        val manifest = config.get(KonanConfigKeys.MANIFEST_FILE)

        libraryWriter = buildLibrary(
                context.config.nativeLibraries,
                context.config.includeBinaries,
                neededLibraries,
                context.serializedLinkData!!,
                abiVersion,
                target,
                output,
                libraryName,
                llvmModule,
                nopack,
                manifest,
                context.dataFlowGraph)
        return libraryWriter.mainBitcodeFileName
    }
}

internal class ProgramProducer(override val context: Context) : CompilerOutputProducer {

    private val phaser = PhaseManager(context)

    override fun produce() {
        val llvmModule = context.llvmModule!!
        val config = context.config.configuration
        val tempFiles = context.config.tempFiles

        val produce = config.get(KonanConfigKeys.PRODUCE)
                ?: throw IllegalArgumentException("Unknown produce type")

        val bitcodeFiles = if (produce == CompilerOutputKind.DYNAMIC) {
            produceCAdapterBitcode(
                    context.config.clang,
                    tempFiles.cAdapterCppName,
                    tempFiles.cAdapterBitcodeName)
            listOf(tempFiles.cAdapterBitcodeName)
        } else emptyList()

        val programBitcode = produceProgram(context, tempFiles, llvmModule, bitcodeFiles)

        lateinit var objectFiles: List<ObjectFile>
        phaser.phase(KonanPhase.OBJECT_FILES) {
            // on wasm32 we link bitcode for now
            val files = listOf(programBitcode) + if (context.isWasm) {
                context.llvm.librariesToLink.map { it.bitcodePaths }.flatten()
            } else {
                emptyList()
            }
            objectFiles = produceObjectFiles(context, files)
        }
        phaser.phase(KonanPhase.LINK_STAGE) {
            linkObjectFiles(context, objectFiles)
        }
    }

    private fun produceProgram(context: Context, tempFiles: TempFiles, llvmModule: LLVMModuleRef,
                               generatedBitcodeFiles: List<String> = emptyList()): String {
        val nativeLibraries = context.config.nativeLibraries +
                context.config.defaultNativeLibraries +
                generatedBitcodeFiles

        PhaseManager(context).phase(KonanPhase.BITCODE_LINKER) {
            for (library in nativeLibraries) {
                val libraryModule = parseBitcodeFile(library)
                val failed = LLVMLinkModules2(llvmModule, libraryModule)
                if (failed != 0) {
                    throw Error("failed to link $library") // TODO: retrieve error message from LLVM.
                }
            }
        }
        val output = tempFiles.nativeBinaryFileName
        LLVMWriteBitcodeToFile(llvmModule, output)
        return output
    }
}
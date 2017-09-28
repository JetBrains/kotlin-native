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
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.backend.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal fun produceOutput(context: Context) {

    val llvmModule = context.llvmModule!!
    val config = context.config.configuration

    when (config.get(KonanConfigKeys.PRODUCE)) {
        CompilerOutputKind.PROGRAM -> {
            val program = context.config.outputName
            val output = "$program.kt.bc"
            context.bitcodeFileName = output

            PhaseManager(context).phase(KonanPhase.BITCODE_LINKER) {
                for (library in context.config.nativeLibraries) {
                    val libraryModule = parseBitcodeFile(library)
                    val failed = LLVMLinkModules2(llvmModule, libraryModule)
                    if (failed != 0) {
                        throw Error("failed to link $library") // TODO: retrieve error message from LLVM.
                    }
                }
            }

            LLVMWriteBitcodeToFile(llvmModule, output)
        }
        CompilerOutputKind.LIBRARY -> {
            val output = context.config.outputName
            val libraryName = context.config.moduleId
            val neededLibraries 
                = context.config.immediateLibraries.purgeUnneeded()
            val abiVersion = context.config.currentAbiVersion
            val target = context.config.targetManager.target
            val nopack = config.getBoolean(KonanConfigKeys.NOPACK)
            val manifest = config.get(KonanConfigKeys.MANIFEST_FILE)

            val library = buildLibrary(
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
                context.moduleEscapeAnalysisResult.build().toByteArray())

            context.library = library
            context.bitcodeFileName = library.mainBitcodeFileName
        }
        CompilerOutputKind.BITCODE -> {
            val output = context.config.outputFile
            context.bitcodeFileName = output
            LLVMWriteBitcodeToFile(llvmModule, output)
        }
    }
}

internal fun List<KonanLibraryReader>.purgeUnneeded(): List<KonanLibraryReader> {
        return this.map { 
            if (!it.isNeededForLink && it.isDefaultLink) null else it
        }.filterNotNull()
}

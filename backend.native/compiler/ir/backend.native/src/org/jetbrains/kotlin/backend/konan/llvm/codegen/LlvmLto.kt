package org.jetbrains.kotlin.backend.konan.llvm.codegen

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPhase
import org.jetbrains.kotlin.backend.konan.PhaseManager
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.uniqueName
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal fun linkBitcode(mainModule: LLVMModuleRef, toLink: List<LLVMModuleRef?>) {
    for (library in toLink) {
        val failed = LLVMLinkModules2(mainModule, library)
        if (failed != 0) {
            throw Error("failed to link $library") // TODO: retrieve error message from LLVM.
        }
    }
}

// Later we may switch to thin LTO or other compilation mode,
// but for now lets use this name.
internal fun compileWithFatLto(context: Context, phaser: PhaseManager, nativeLibraries: List<String>) {
    val libraries = context.llvm.librariesToLink
    val programModule = context.llvmModule!!
    val runtime = context.llvm.runtime

    // We need to detect stdlib because it will be linked different way.
    fun stdlibPredicate(libraryReader: KonanLibrary) = libraryReader.uniqueName == "stdlib"
    val stdlibPath = libraries.first(::stdlibPredicate).bitcodePaths.first { it.endsWith("program.kt.bc") }
    val stdlibModule = parseBitcodeFile(stdlibPath)

    val otherModules = libraries.filterNot(::stdlibPredicate).flatMap { it.bitcodePaths }

    phaser.phase(KonanPhase.BITCODE_LINKER) {
        linkBitcode(programModule, (nativeLibraries + otherModules).map { parseBitcodeFile(it) })
    }

    phaser.phase(KonanPhase.LLVM_CODEGEN) {

        val target = LLVMGetTarget(runtime.llvmModule)!!.toKString()

        memScoped {
            val configuration = alloc<CodeGenConfig>().apply {

                fileName = context.config.tempFiles.finalObjectFileName.cstr.ptr

                outputKind = OutputKind.OUTPUT_KIND_OBJECT_FILE

                targetTriple = target.cstr.ptr

                optLevel = when {
                    context.shouldOptimize() -> 3
                    context.shouldContainDebugInfo() -> 0
                    else -> 1
                }

                // We should care about output size on embedded targets.
                sizeLevel = when (context.config.target) {
                    KonanTarget.WASM32, is KonanTarget.ZEPHYR -> 2
                    else -> if (context.shouldOptimize()) 0 else 1
                }

                shouldProfile = context.shouldProfilePhases().toByte().toInt()

                relocMode = when (context.config.produce) {
                    CompilerOutputKind.PROGRAM -> LLVMRelocMode.LLVMRelocStatic
                    else -> LLVMRelocMode.LLVMRelocPIC
                }
                shouldPerformLto = context.shouldOptimize().toByte().toInt()

                shouldPreserveDebugInfo = context.shouldContainDebugInfo().toByte().toInt()

                compilingForHost = (HostManager.host == context.config.target).toByte().toInt()
            }

            if (LLVMFatLtoCodegen(
                            LLVMGetModuleContext(context.llvmModule),
                            programModule,
                            runtime.llvmModule,
                            stdlibModule,
                            configuration.readValue()
                    ) != 0) {
                context.reportCompilationError("Fat LTO codegen failed")
            }
        }
    }
}
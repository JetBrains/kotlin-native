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

import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect
import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.*

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

// Use "clang -v -save-temps" to write linkCommand() method 
// for another implementation of this class.
internal abstract class PlatformFlags(val properties: KonanProperties) {
    val llvmLtoNooptFlags = properties.llvmLtoNooptFlags
    val llvmLtoOptFlags = properties.llvmLtoOptFlags
    val llvmLtoFlags = properties.llvmLtoFlags
    val llvmLtoDynamicFlags = properties.llvmLtoDynamicFlags
    val entrySelector = properties.entrySelector
    val linkerOptimizationFlags = properties.linkerOptimizationFlags
    val linkerKonanFlags = properties.linkerKonanFlags
    val linkerNoDebugFlags = properties.linkerNoDebugFlags
    val linkerDynamicFlags = properties.linkerDynamicFlags
    val llvmDebugOptFlags = properties.llvmDebugOptFlags
    val s2wasmFlags = properties.s2wasmFlags
    val targetToolchain = properties.absoluteTargetToolchain
    val targetSysRoot = properties.absoluteTargetSysRoot

    val targetLibffi = properties.libffiDir ?.let { listOf("${properties.absoluteLibffiDir}/lib/libffi.a") } ?: emptyList()

    open val useCompilerDriverAsLinker: Boolean get() = false // TODO: refactor.

    abstract fun linkCommand(objectFiles: List<ObjectFile>,
                             defaultLibraries: List<StaticLibrary>,
                             userStaticLibs: List<StaticLibrary>,
                             executable: ExecutableFile,
                             optimize: Boolean,
                             debug: Boolean,
                             dynamic: Boolean
                             ): Command

    open fun linkCommandSuffix(): List<String> = emptyList()

    protected fun propertyTargetString(name: String)
        = properties.targetString(name)!!
    protected fun propertyTargetList(name: String)
        = properties.targetList(name)

    abstract fun filterStaticLibraries(binaries: List<String>): List<String> 

    open fun linkStaticLibraries(binaries: List<String>): List<String> {
        val libraries = filterStaticLibraries(binaries)
        // Let's just pass them as absolute paths
        return libraries
    }

}

internal open class AndroidPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val prefix = "$targetToolchain/bin/"
    private val clang = "$prefix/clang"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>,
                             defaultLibraries: List<StaticLibrary>,
                             userStaticLibs: List<StaticLibrary>,
                             executable: ExecutableFile,
                             optimize: Boolean,
                             debug: Boolean,
                             dynamic: Boolean
    ): Command {
        // liblog.so must be linked in, as we use its functionality in runtime.
        return Command(clang).apply {
            + "-o"
            + executable
            + "-fPIC"
            + "-shared"
            + "-llog"
            + objectFiles
            if (optimize) + linkerOptimizationFlags
            if (!debug) + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
            + linkerKonanFlags
        }
    }
}

internal open class MacOSBasedPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val linker = "$targetToolchain/usr/bin/ld"
    private val dsymutil = "${distribution.llvmBin}/llvm-dsymutil"
    private val libLTO = distribution.libLTO

    open val osVersionMin by lazy {
        listOf(
                propertyTargetString("osVersionMinFlagLd"),
                properties.osVersionMin!! + ".0")
    }

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>,
                             defaultLibraries: List<StaticLibrary>,
                             userStaticLibs: List<StaticLibrary>,
                             executable: ExecutableFile,
                             optimize: Boolean,
                             debug: Boolean,
                             dynamic: Boolean
    ): Command {
        return object : Command(linker) {} .apply {
            + "-demangle"
            + listOf("-object_path_lto", "temporary.o", "-lto_library", libLTO)
            + listOf("-dynamic", "-arch", propertyTargetString("arch"))
            + osVersionMin
            + listOf("-syslibroot", targetSysRoot, "-o", executable)
            + objectFiles
            + defaultLibraries
            + userStaticLibs.map { listOf("-force_load", it) }.flatten()
            if (optimize) + linkerOptimizationFlags
            if (!debug) + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
            + linkerKonanFlags
            + "-lSystem"
        }
    }

    fun dsymUtilCommand(executable: ExecutableFile, outputDsymBundle: String) =
            object : Command(dsymutilCommand(executable, outputDsymBundle)) {
                override fun runProcess(): Int =
                        executeCommandWithFilter(command)
            }

    // TODO: consider introducing a better filtering directly in Command.
    private fun executeCommandWithFilter(command: List<String>): Int {
        val builder = ProcessBuilder(command)

        // Inherit main process output streams.
        val isDsymUtil = (command[0] == dsymutil)

        builder.redirectOutput(Redirect.INHERIT)
        builder.redirectInput(Redirect.INHERIT)
        if (!isDsymUtil)
            builder.redirectError(Redirect.INHERIT)

        val process = builder.start()
        if (isDsymUtil) {
            /**
             * llvm-lto has option -alias that lets tool to know which symbol we use instead of _main,
             * llvm-dsym doesn't have such a option, so we ignore annoying warning manually.
             */
            val errorStream = process.errorStream
            val outputStream = bufferedReader(errorStream)
            while (true) {
                val line = outputStream.readLine() ?: break
                if (!line.contains("warning: could not find object file symbol for symbol _main"))
                    System.err.println(line)
            }
            outputStream.close()
        }
        val exitCode = process.waitFor()
        return exitCode
    }

    open fun dsymutilCommand(executable: ExecutableFile, outputDsymBundle: String): List<String> =
            listOf(dsymutil, executable, "-o", outputDsymBundle)

    open fun dsymutilDryRunVerboseCommand(executable: ExecutableFile): List<String> =
            listOf(dsymutil, "-dump-debug-map" ,executable)
}

internal open class LinuxBasedPlatform(val distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val llvmLib = distribution.llvmLib
    private val libGcc = "$targetSysRoot/${propertyTargetString("libGcc")}"
    private val linker = "$targetToolchain/bin/ld.gold"
    private val pluginOptimizationFlags = propertyTargetList("pluginOptimizationFlags")
    private val specificLibs
        = propertyTargetList("abiSpecificLibraries").map { "-L${targetSysRoot}/$it" }

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>,
                             defaultLibraries: List<StaticLibrary>,
                             userStaticLibs: List<StaticLibrary>,
                             executable: ExecutableFile,
                             optimize: Boolean,
                             debug: Boolean,
                             dynamic: Boolean
    ): Command {
        val isMips = (distribution.target == KonanTarget.LINUX_MIPS32 ||
                distribution.target == KonanTarget.LINUX_MIPSEL32)
        // TODO: Can we extract more to the konan.properties?
        return Command(linker).apply {
            + "--sysroot=${targetSysRoot}"
//            + "-export-dynamic"
            + "-z"
            + "relro"
            + "--build-id"
            + "--eh-frame-hdr"
            // + "-m"
            // + "elf_x86_64",
            + "-dynamic-linker"
            + propertyTargetString("dynamicLinker")
            + "-o"
            + executable
            + defaultLibraries
            + "--whole-library"
            + userStaticLibs
            + "--no-whole-library"
            if (!dynamic) + "$targetSysRoot/usr/lib64/crt1.o"
            + "$targetSysRoot/usr/lib64/crti.o"
            if (dynamic)
                + "$libGcc/crtbeginS.o"
            else
                + "$libGcc/crtbegin.o"
            + "-L$llvmLib"
            + "-L$libGcc"
            if (!isMips) + "--hash-style=gnu" // MIPS doesn't support hash-style=gnu
            + specificLibs
            + listOf("-L$targetSysRoot/../lib", "-L$targetSysRoot/lib", "-L$targetSysRoot/usr/lib")
            if (optimize) {
                + "-plugin"
                +"$llvmLib/LLVMgold.so"
                + pluginOptimizationFlags
            }
            if (optimize) + linkerOptimizationFlags
            if (!debug) + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
            + objectFiles
            + linkerKonanFlags
            + listOf("-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
                    "-lc", "-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed")
            if (dynamic)
                + "$libGcc/crtendS.o"
            else
                + "$libGcc/crtend.o"
            + "$targetSysRoot/usr/lib64/crtn.o"
        }
    }
}

internal open class MingwPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val linker = "$targetToolchain/bin/clang++"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isWindowsStaticLib || it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>,
                             defaultLibraries: List<StaticLibrary>,
                             userStaticLibs: List<StaticLibrary>,
                             executable: ExecutableFile,
                             optimize: Boolean,
                             debug: Boolean,
                             dynamic: Boolean
    ): Command {
        return Command(linker).apply {
            + listOf("-o", executable)
            + objectFiles
            if (optimize) + linkerOptimizationFlags
            if (!debug)  + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
        }
    }

    override fun linkCommandSuffix() = linkerKonanFlags
}

internal open class WasmPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val clang = "clang"

    override val useCompilerDriverAsLinker: Boolean get() = false

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter{it.isJavaScript}

    override fun linkCommand(objectFiles: List<ObjectFile>,
                             defaultLibraries: List<StaticLibrary>,
                             userStaticLibs: List<StaticLibrary>,
                             executable: ExecutableFile,
                             optimize: Boolean,
                             debug: Boolean,
                             dynamic: Boolean
    ): Command {
        return object: Command("") {
            override fun execute() {
                val src = File(objectFiles.single())
                val dst = File(executable)
                src.recursiveCopyTo(dst)
                javaScriptLink(args, executable)
            }

            private fun javaScriptLink(jsFiles: List<String>, executable: String): String {
                val linkedJavaScript = File("$executable.js")

                val linkerHeader = "var konan = { libraries: [] };\n"
                val linkerFooter = """|if (isBrowser()) {
                                      |   konan.moduleEntry([]);
                                      |} else {
                                      |   konan.moduleEntry(arguments);
                                      |}""".trimMargin()

                linkedJavaScript.writeBytes(linkerHeader.toByteArray());

                jsFiles.forEach {
                    linkedJavaScript.appendBytes(File(it).readBytes())
                }

                linkedJavaScript.appendBytes(linkerFooter.toByteArray());
                return linkedJavaScript.name
            }
        }
    }
}

internal fun determinePlatformFlags(target: KonanTarget, distribution: Distribution) = when (target) {
    KonanTarget.LINUX, KonanTarget.RASPBERRYPI,
    KonanTarget.LINUX_MIPS32, KonanTarget.LINUX_MIPSEL32 ->
        LinuxBasedPlatform(distribution)
    KonanTarget.MACBOOK, KonanTarget.IPHONE, KonanTarget.IPHONE_SIM ->
        MacOSBasedPlatform(distribution)
    KonanTarget.ANDROID_ARM32, KonanTarget.ANDROID_ARM64 ->
        AndroidPlatform(distribution)
    KonanTarget.MINGW ->
        MingwPlatform(distribution)
    KonanTarget.WASM32 ->
        WasmPlatform(distribution)
}

internal fun linkObjectFiles(context: Context, objectFiles: List<ObjectFile>) =
        LinkStage(context).linkStage(objectFiles)

private class LinkStage(val context: Context) {

    private val config = context.config.configuration
    private val target = context.config.targetManager.target

    private val distribution = context.config.distribution

    private val optimize = config.get(KonanConfigKeys.OPTIMIZATION) ?: false

    private val platform = determinePlatformFlags(target, distribution)
    private val debug = config.get(KonanConfigKeys.DEBUG) ?: false
    private val dynamic = context.config.produce == CompilerOutputKind.DYNAMIC ||
            context.config.produce == CompilerOutputKind.FRAMEWORK
    private val nomain = config.get(KonanConfigKeys.NOMAIN) ?: false
    private val libraries = context.llvm.librariesToLink

    private fun asLinkerArgs(args: List<String>): List<String> {
        if (platform.useCompilerDriverAsLinker) {
            return args
        }

        val result = mutableListOf<String>()
        for (arg in args) {
            // If user passes compiler arguments to us - transform them to linker ones.
            if (arg.startsWith("-Wl,")) {
                result.addAll(arg.substring(4).split(','))
            } else {
                result.add(arg)
            }
        }
        return result
    }

    // Ideally we'd want to have 
    //      #pragma weak main = Konan_main
    // in the launcher.cpp.
    // Unfortunately, anything related to weak linking on MacOS
    // only seems to be working with dynamic libraries.
    // So we stick to "-alias _main _konan_main" on Mac.
    // And just do the same on Linux.
    private val entryPointSelector: List<String>
        get() = if (nomain || dynamic) emptyList() else platform.entrySelector

    private fun link(objectFiles: List<ObjectFile>, includedBinaries: List<String>,
                     userProvidedLibs: List<String>, libraryProvidedLinkerFlags: List<String>): ExecutableFile? {
        val frameworkLinkerArgs: List<String>
        val executable: String

        if (context.config.produce != CompilerOutputKind.FRAMEWORK) {
            frameworkLinkerArgs = emptyList()
            executable = context.config.outputFile
        } else {
            val framework = File(context.config.outputFile)
            val dylibName = framework.name.removeSuffix(".framework")
            val dylibRelativePath = when (target) {
                KonanTarget.IPHONE, KonanTarget.IPHONE_SIM -> dylibName
                KonanTarget.MACBOOK -> "Versions/A/$dylibName"
                else -> error(target)
            }
            frameworkLinkerArgs = listOf("-install_name", "@rpath/${framework.name}/$dylibRelativePath")
            val dylibPath = framework.child(dylibRelativePath)
            dylibPath.parentFile.mkdirs()
            executable = dylibPath.absolutePath
        }

        try {
            platform.linkCommand(objectFiles, includedBinaries, userProvidedLibs, executable, optimize, debug, dynamic).apply {
                + platform.targetLibffi
                + asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS))
                + entryPointSelector
                + frameworkLinkerArgs
                + platform.linkCommandSuffix()
                + platform.linkStaticLibraries(includedBinaries)
                + libraryProvidedLinkerFlags
                logger = context::log
            }.execute()

            if (debug && platform is MacOSBasedPlatform) {
                val outputDsymBundle = context.config.outputFile + ".dSYM" // `outputFile` is either binary or bundle.

                platform.dsymUtilCommand(executable, outputDsymBundle)
                        .logWith(context::log)
                        .execute()
            }
        } catch (e: KonanExternalToolFailure) {
            context.reportCompilationError("${e.toolName} invocation reported errors")
            return null
        }
        return executable
    }

    fun linkStage(objectFiles: List<ObjectFile>) {
        val (defalutLibs, userProvidedLibs) = libraries.partition { it.isDefaultLibrary }
        val libraryProvidedLinkerFlags = libraries.map { it.linkerOpts }.flatten()
        link(
                objectFiles,
                defalutLibs.map { it.includedPaths }.flatten(),
                userProvidedLibs.map { it.includedPaths }.flatten(),
                libraryProvidedLinkerFlags
        )
    }
}


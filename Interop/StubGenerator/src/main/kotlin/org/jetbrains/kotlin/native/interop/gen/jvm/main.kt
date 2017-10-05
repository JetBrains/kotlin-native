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

package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.tool.*
import org.jetbrains.kotlin.konan.util.DefFile
import org.jetbrains.kotlin.native.interop.gen.HeadersInclusionPolicyImpl
import org.jetbrains.kotlin.native.interop.gen.ImportsImpl
import org.jetbrains.kotlin.native.interop.indexer.*
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KFunction

fun main(args: Array<String>) = interop(args, null)

fun interop(args: Array<String>, argsToCompiler: MutableList<String>? = null) {

    processLib(parseArgs(args), argsToCompiler)
}

// Options, whose values are space-separated and can be escaped.
val escapedOptions = setOf("-compilerOpts", "-linkerOpts")

private fun String.asArgList(key: String) =
        if (escapedOptions.contains(key))
            this.split(Regex("(?<!\\\\)\\Q \\E")).filter { it.isNotEmpty() }.map { it.replace("\\ ", " ") }
        else
            listOf(this)

private fun parseArgs(args: Array<String>): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for (index in 0..args.size - 1 step 2) {
        val key = args[index]
        if (key[0] != '-') {
            throw IllegalArgumentException("Expected a flag with initial dash: $key")
        }
        if (index + 1 == args.size) {
            throw IllegalArgumentException("Expected an value after $key")
        }
        val value = args[index + 1].asArgList(key)
        commandLine[key]?.addAll(value) ?: commandLine.put(key, value.toMutableList())
    }
    return commandLine
}

// Performs substitution similar to:
//  foo = ${foo} ${foo.${arch}} ${foo.${os}}
private fun substitute(properties: Properties, substitutions: Map<String, String>) {
    for (key in properties.stringPropertyNames()) {
        for (substitution in substitutions.values) {
            val suffix = ".$substitution"
            if (key.endsWith(suffix)) {
                val baseKey = key.removeSuffix(suffix)
                val oldValue = properties.getProperty(baseKey, "")
                val appendedValue = properties.getProperty(key, "")
                val newValue = if (oldValue != "") "$oldValue $appendedValue" else appendedValue
                properties.setProperty(baseKey, newValue)
            }
        }
    }
}

private fun ProcessBuilder.runExpectingSuccess() {
    val res = this.start().waitFor()
    if (res != 0) {
        throw Error("Process finished with non-zero exit code: $res")
    }
}

private fun <T> Collection<T>.atMostOne(): T? {
    return when (this.size) {
        0 -> null
        1 -> this.iterator().next()
        else -> throw IllegalArgumentException("Collection has more than one element.")
    }
}

private fun List<String>?.isTrue(): Boolean {
    // The rightmost wins, null != "true".
    return this?.last() == "true"
}

private fun runCmd(command: Array<String>, workDir: File, verbose: Boolean = false) {
    val builder = ProcessBuilder(*command)
            .directory(workDir)

    val logFile: File?

    if (verbose) {
        println(command.joinToString(" "))
        builder.inheritIO()
        logFile = null
    } else {
        logFile = createTempFile(suffix = ".log")
        logFile.deleteOnExit()

        builder.redirectOutput(ProcessBuilder.Redirect.to(logFile))
                .redirectErrorStream(true)
    }

    try {
        builder.runExpectingSuccess()
    } catch (e: Throwable) {
        if (!verbose) {
            println(command.joinToString(" "))
            logFile!!.useLines {
                it.forEach { println(it) }
            }
        }

        throw e
    }
}

private fun loadProperties(file: File?, substitutions: Map<String, String>): Properties {
    val result = Properties()
    file?.bufferedReader()?.use { reader ->
        result.load(reader)
    }
    substitute(result, substitutions)
    return result
}

private fun Properties.storeProperties(file: File) {
    file.outputStream().use {
        this.store(it, null)
    }
}

private fun usage() {
    println("""
Run interop tool with -def <def_file_for_lib>.def
Following flags are supported:
  -def <file>.def specifies library definition file
  -compilerOpts <c compiler flags> specifies flags passed to clang
  -linkerOpts <linker flags> specifies flags passed to linker
  -verbose <boolean> increases verbosity
  -shims <boolean> adds generation of shims tracing native library calls
  -pkg <fully qualified package name> place the resulting definitions into the package
  -h <file>.h header files to parse
""")
}

private fun selectNativeLanguage(config: DefFile.DefFileConfig): Language {
    val languages = mapOf(
            "C" to Language.C,
            "Objective-C" to Language.OBJECTIVE_C
    )

    val language = config.language ?: return Language.C

    return languages[language] ?:
            error("Unexpected language '$language'. Possible values are: ${languages.keys.joinToString { "'$it'" }}")
}

private fun resolveLibraries(staticLibraries: List<String>, libraryPaths: List<String>): List<String> {
    val result = mutableListOf<String>()
    staticLibraries.forEach { library ->
        
        val resolution = libraryPaths.map { "$it/$library" } 
                .find { File(it).exists() }

        if (resolution != null) {
            result.add(resolution)
        } else {
            error("Could not find '$library' binary in neither of $libraryPaths")
        }
    }
    return result
}

private fun argsToCompiler(staticLibraries: List<String>, libraryPaths: List<String>): List<String> {
    return resolveLibraries(staticLibraries, libraryPaths)
        .map { it -> listOf("-includeBinary", it) } .flatten()
}

private fun parseImports(args: Map<String, List<String>>): ImportsImpl {
    val headerIdToPackage = (args["-import"] ?: emptyList()).map { arg ->
        val (pkg, joinedIds) = arg.split(':')
        val ids = joinedIds.split(',')
        ids.map { HeaderId(it) to pkg }
    }.reversed().flatten().toMap()

    return ImportsImpl(headerIdToPackage)
}

private fun processLib(args: Map<String, List<String>>, 
                       argsToCompiler: MutableList<String>?) {

    val userDir = System.getProperty("user.dir")
    val ktGenRoot = args["-generated"]?.single() ?: userDir
    val nativeLibsDir = args["-natives"]?.single() ?: userDir
    val flavorName = args["-flavor"]?.single() ?: "jvm"
    val flavor = KotlinPlatform.values().single { it.name.equals(flavorName, ignoreCase = true) }
    val defFile = args["-def"]?.single()?.let { File(it) }
    val manifestAddend = args["-manifest"]?.single()?.let { File(it) }
    
    if (defFile == null && args["-pkg"] == null) {
        usage()
        return
    }

    val tool = ToolConfig(
        args["-target"]?.single(),
        args["-properties"]?.single(),
        System.getProperty("konan.home")
    )
    tool.downloadDependencies()

    val def = DefFile(defFile, tool.substitutions)

    val additionalHeaders = args["-h"].orEmpty()
    val additionalCompilerOpts = args["-copt"].orEmpty() + args["-compilerOpts"].orEmpty()
    val additionalLinkerOpts = args["-lopt"].orEmpty() + args["-linkerOpts"].orEmpty()
    val generateShims = args["-shims"].isTrue()
    val verbose = args["-verbose"].isTrue()

    System.load(tool.libclang)

    val headerFiles = def.config.headers + additionalHeaders
    val language = selectNativeLanguage(def.config)
    val compilerOpts: List<String> = mutableListOf<String>().apply {
        addAll(def.config.compilerOpts)
        addAll(tool.defaultCompilerOpts)
        addAll(additionalCompilerOpts)
        addAll(when (language) {
            Language.C -> emptyList()
            Language.OBJECTIVE_C -> {
                // "Objective-C" within interop means "Objective-C with ARC":
                listOf("-fobjc-arc")
                // Using this flag here has two effects:
                // 1. The headers are parsed with ARC enabled, thus the API is visible correctly.
                // 2. The generated Objective-C stubs are compiled with ARC enabled, so reference counting
                // calls are inserted automatically.
            }
        })
    }

    val excludeSystemLibs = def.config.excludeSystemLibs
    val excludeDependentModules = def.config.excludeDependentModules

    val entryPoint = def.config.entryPoints.atMostOne()
    val linkerOpts =
            def.config.linkerOpts.toTypedArray() + 
            tool.defaultCompilerOpts + 
            additionalLinkerOpts
    val linkerName = args["-linker"]?.atMostOne() ?: def.config.linker
    val linker = "${tool.llvmHome}/bin/$linkerName"
    val compiler = "${tool.llvmHome}/bin/clang"
    val excludedFunctions = def.config.excludedFunctions.toSet()
    val staticLibraries = def.config.staticLibraries + args["-staticLibrary"].orEmpty()
    val libraryPaths = def.config.libraryPaths + args["-libraryPath"].orEmpty()
    argsToCompiler ?. let { it.addAll(argsToCompiler(staticLibraries, libraryPaths)) }

    val fqParts = (args["-pkg"]?.atMostOne() ?: def.config.packageName)?.let {
        it.split('.')
    } ?: defFile!!.name.split('.').reversed().drop(1)

    val outKtFileName = fqParts.last() + ".kt"

    val outKtPkg = fqParts.joinToString(".")
    val outKtFileRelative = (fqParts + outKtFileName).joinToString("/")
    val outKtFile = File(ktGenRoot, outKtFileRelative)

    val libName = args["-cstubsname"]?.atMostOne() ?: fqParts.joinToString("") + "stubs"

    val headerFilterGlobs = def.config.headerFilter
    val imports = parseImports(args)
    val headerInclusionPolicy = HeadersInclusionPolicyImpl(headerFilterGlobs, imports)

    val library = NativeLibrary(
            includes = headerFiles,
            additionalPreambleLines = def.defHeaderLines,
            compilerArgs = compilerOpts,
            language = language,
            excludeSystemLibs = excludeSystemLibs,
            excludeDepdendentModules = excludeDependentModules,
            headerInclusionPolicy = headerInclusionPolicy
    )

    val configuration = InteropConfiguration(
            library = library,
            pkgName = outKtPkg,
            excludedFunctions = excludedFunctions,
            strictEnums = def.config.strictEnums.toSet(),
            nonStrictEnums = def.config.nonStrictEnums.toSet()
    )

    val nativeIndex = buildNativeIndex(library)

    val gen = StubGenerator(nativeIndex, configuration, libName, generateShims, verbose, flavor, imports)

    outKtFile.parentFile.mkdirs()

    File(nativeLibsDir).mkdirs()
    val outCFile = File("$nativeLibsDir/$libName.${language.sourceFileExtension}") // TODO: select the better location.

    outKtFile.bufferedWriter().use { ktFile ->
        outCFile.bufferedWriter().use { cFile ->
            gen.generateFiles(ktFile = ktFile, cFile = cFile, entryPoint = entryPoint)
        }
    }

    // TODO: if a library has partially included headers, then it shouldn't be used as a dependency.
    def.manifestAddendProperties["includedHeaders"] = nativeIndex.includedHeaders.joinToString(" ") { it.value }
    def.manifestAddendProperties["pkg"] = outKtPkg

    gen.addManifestProperties(def.manifestAddendProperties)

    manifestAddend?.parentFile?.mkdirs()
    manifestAddend?.let { def.manifestAddendProperties.storeProperties(it) }

    val workDir = defFile?.absoluteFile?.parentFile ?: File(userDir)

    if (flavor == KotlinPlatform.JVM) {

        val outOFile = createTempFile(suffix = ".o")

        val compilerCmd = arrayOf(compiler, *gen.libraryForCStubs.compilerArgs.toTypedArray(),
                "-c", outCFile.absolutePath, "-o", outOFile.absolutePath)

        runCmd(compilerCmd, workDir, verbose)

        val outLib = File(nativeLibsDir, System.mapLibraryName(libName))

        val linkerCmd = arrayOf(linker,
                outOFile.absolutePath, "-shared", "-o", outLib.absolutePath,
                *linkerOpts)

        runCmd(linkerCmd, workDir, verbose)

        outOFile.delete()
    } else if (flavor == KotlinPlatform.NATIVE) {
        val outBcName = libName + ".bc"
        val outLib = File(nativeLibsDir, outBcName)
        val compilerCmd = arrayOf(compiler, *gen.libraryForCStubs.compilerArgs.toTypedArray(),
                "-emit-llvm", "-c", outCFile.absolutePath, "-o", outLib.absolutePath)

        runCmd(compilerCmd, workDir, verbose)
    }

    if (!args["-keepcstubs"].isTrue()) {
        outCFile.delete()
    }
}

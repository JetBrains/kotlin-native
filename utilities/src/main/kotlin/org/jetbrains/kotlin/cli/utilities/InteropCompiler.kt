/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.native.interop.gen.jvm.interop
import org.jetbrains.kliopt.ArgParser
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.native.interop.tool.*

// TODO: this function should eventually be eliminated from 'utilities'. 
// The interaction of interop and the compler should be streamlined.

fun invokeInterop(flavor: String, args: Array<String>): Array<String>? {
    val argParser = ArgParser(if (flavor == "native") getCInteropArguments() else getJSInteropArguments(),
            useDefaultHelpShortName = false)
    if (!argParser.parse(args))
        return null
    val outputFileName = argParser.get<String>("output")!!
    val noDefaultLibs = argParser.get<Boolean>(NODEFAULTLIBS)!!
    val purgeUserLibs = argParser.get<Boolean>(PURGE_USER_LIBS)!!
    val temporaryFilesDir = argParser.get<String>(TEMP_DIR)

    val buildDir = File("$outputFileName-build")
    val generatedDir = File(buildDir, "kotlin")
    val nativesDir = File(buildDir, "natives")
    val manifest = File(buildDir, "manifest.properties")
    val additionalArgs = listOf(
            "-generated", generatedDir.path,
            "-natives", nativesDir.path,
            "-flavor", flavor
    )
    val additionalProperties = mutableMapOf<String, Any>(
            "manifest" to manifest.path)
    val cstubsName ="cstubs"
    val libraries = argParser.getAll<String>("library") ?: listOf<String>()
    val repos = argParser.getAll<String>("repo") ?: listOf<String>()
    val targetRequest = argParser.get<String>("target")!!
    val target = PlatformManager().targetManager(targetRequest).target

    if (flavor == "native") {
        val resolver = defaultResolver(
                repos,
                libraries.filter { it.contains(File.separator) },
                target,
                Distribution(),
                listOf(KonanVersion.CURRENT)
        ).libraryResolver()
        val allLibraries = resolver.resolveWithDependencies(
                libraries.toUnresolvedLibraries, noStdLib = true, noDefaultLibs = noDefaultLibs
        ).getFullList()

        val imports = allLibraries.map { library ->
            // TODO: handle missing properties?
            library.packageFqName?.let { packageFqName ->
                val headerIds = library.includedHeaders
                "$packageFqName:${headerIds.joinToString(";")}"
            }
        }.filterNotNull()
        additionalProperties.putAll(mapOf("cstubsname" to cstubsName, "import" to imports))
    }

    val cinteropArgsToCompiler = interop(flavor, args + additionalArgs, additionalProperties) ?: return null

    val nativeStubs = 
        if (flavor == "wasm") 
            arrayOf("-include-binary", File(nativesDir, "js_stubs.js").path)
        else 
            arrayOf("-native-library", File(nativesDir, "$cstubsName.bc").path)

    val konancArgs = arrayOf(
        generatedDir.path, 
        "-produce", "library", 
        "-o", outputFileName,
        "-target", target.visibleName,
        "-manifest", manifest.path,
        "-Xtemporary-files-dir=$temporaryFilesDir") +
        nativeStubs +
        cinteropArgsToCompiler + 
        libraries.flatMap { listOf("-library", it) } + 
        repos.flatMap { listOf("-repo", it) } +
        (if (noDefaultLibs) arrayOf("-$NODEFAULTLIBS") else emptyArray()) +
        (if (purgeUserLibs) arrayOf("-$PURGE_USER_LIBS") else emptyArray())

    return konancArgs
}



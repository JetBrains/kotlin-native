package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.backend.konan.library.defaultResolver
import org.jetbrains.kotlin.backend.konan.library.impl.KonanLibrary
import org.jetbrains.kotlin.backend.konan.library.resolveLibrariesRecursive
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.TargetManager
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.cli.bc.main as konancMain
import org.jetbrains.kotlin.native.interop.gen.jvm.interop 
import org.jetbrains.kotlin.cli.klib.main as klibMain

fun invokeCinterop(args: Array<String>) {
    var outputFileName = "nativelib"
    var target = "host"
    val libraries = mutableListOf<String>()
    val repos = mutableListOf<String>()
    for (i in args.indices) {
        val arg = args[i]
        val nextArg = args.getOrNull(i + 1)
        if (arg.startsWith("-o"))
            outputFileName = nextArg ?: outputFileName
        if (arg == "-target")
            target = nextArg ?: target
        if (arg == "-library")
            libraries.addIfNotNull(nextArg)
        if (arg == "-r" || arg == "-repo")
            repos.addIfNotNull(nextArg)
    }

    val buildDir = File("$outputFileName-build")
    val generatedDir = File(buildDir, "kotlin")
    val nativesDir = File(buildDir, "natives")
    val cstubsName ="cstubs"
    val manifest = File(buildDir, "manifest.properties")

    val targetManager = TargetManager(target)
    val resolver = defaultResolver(repos, targetManager)
    val allLibraries = resolver.resolveLibrariesRecursive(
            libraries, targetManager.target, noStdLib = true, noDefaultLibs = false
    )

    val importArgs = allLibraries.flatMap {
        val library = KonanLibrary(it.libraryFile)
        val manifestProperties = library.manifestFile.loadProperties()
        // TODO: handle missing properties?
        manifestProperties["pkg"]?.let {
            val pkg = it as String
            val headerIds = (manifestProperties["includedHeaders"] as String).split(' ')
            val arg = "$pkg:${headerIds.joinToString(",")}"
            listOf("-import", arg)
        } ?: emptyList()
    }

    val additionalArgs = listOf<String>(
        "-generated", generatedDir.path, 
        "-natives", nativesDir.path, 
        "-cstubsname", cstubsName,
        "-manifest", manifest.path,
        "-flavor", "native"
    ) + importArgs

    val cinteropArgs = (additionalArgs + args.toList()).toTypedArray()
    val cinteropArgsToCompiler = mutableListOf<String>()
    interop(cinteropArgs, cinteropArgsToCompiler)

    val konancArgs = arrayOf(
        generatedDir.path, 
        "-produce", "library", 
        "-nativelibrary", File(nativesDir, "$cstubsName.bc").path,
        "-o", outputFileName,
        "-target", target,
        "-manifest", manifest.path
    ) + cinteropArgsToCompiler + libraries.flatMap { listOf("-library", it) } + repos.flatMap { listOf("-repo", it) }
    konancMain(konancArgs)
}

fun main(args: Array<String>) {
    val utilityName = args[0]
    val utilityArgs = args.drop(1).toTypedArray()
    when (utilityName) {
        "konanc" ->
            konancMain(utilityArgs)
        "cinterop" ->
            invokeCinterop(utilityArgs)
        "klib" ->
            klibMain(utilityArgs)
        else ->
            error("Unexpected utility name")
    }
}


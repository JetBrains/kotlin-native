/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.testing.native.*
import org.jetbrains.kotlin.bitcode.CompileToBitcode

plugins {
    id("compile-to-bitcode")
    id("runtime-testing")
}

googletest {
    revision = project.property("gtestRevision") as String
    refresh = project.hasProperty("refresh-gtest")
}

fun CompileToBitcode.includeRuntime() {
    headersDirs += files("../common/src/hash/headers", "src/main/cpp")
}

val hostName: String by project
val targetList: List<String> by project

bitcode {
    bitcode("runtime", file("src/main")) {
        dependsOn(
            ":common:${target}Hash",
            "${target}StdAlloc",
            "${target}OptAlloc",
            "${target}Mimalloc",
            "${target}Launcher",
            "${target}Debug",
            "${target}Release",
            "${target}Strict",
            "${target}Relaxed",
            "${target}ProfileRuntime",
            "${target}Objc",
            "${target}ExceptionsSupport",
            "${target}LegacyMemoryManager",
            "${target}ExperimentalMemoryManager"
        )
        includeRuntime()
        linkerArgs.add(project.file("../common/build/bitcode/main/$target/hash.bc").path)
    }

    bitcodeTest("MainTests", file("src/main")) {
        includeRuntime()
    }

    bitcode("mimalloc") {
        language = CompileToBitcode.Language.C
        includeFiles = listOf("**/*.c")
        excludeFiles += listOf("**/alloc-override*.c", "**/page-queue.c", "**/static.c", "**/bitmap.inc.c")
        srcDirs = files("$srcRoot/c")
        compilerArgs.addAll(listOf("-DKONAN_MI_MALLOC=1", "-Wno-unknown-pragmas", "-ftls-model=initial-exec",
                "-Wno-unused-function", "-Wno-error=atomic-alignment",
                "-Wno-unused-parameter" /* for windows 32*/))
        headersDirs = files("$srcRoot/c/include")

        onlyIf { targetSupportsMimallocAllocator(target) }
    }

    bitcode("launcher") {
        includeRuntime()
    }

    bitcode("debug") {
        includeRuntime()
    }

    bitcode("std_alloc")
    bitcode("opt_alloc")

    bitcode("exceptionsSupport", file("src/exceptions_support")) {
        includeRuntime()
    }

    bitcode("release") {
        includeRuntime()
    }

    bitcode("strict") {
        includeRuntime()
    }

    bitcode("relaxed") {
        includeRuntime()
    }

    bitcode("profileRuntime", file("src/profile_runtime"))

    bitcode("objc") {
        includeRuntime()
    }

    bitcodeTest("test_support") {
        includeFiles = listOf("**/*.cpp", "**/*.mm")
        includeRuntime()
    }

    bitcode("legacy_memory_manager", file("src/legacymm")) {
        includeRuntime()
    }

    bitcode("experimental_memory_manager", file("src/mm")) {
        includeRuntime()
    }

    test("StdAllocRuntimeTests", listOf(
            "Runtime",
            "MainTests",
            "LegacyMemoryManager",
            "Strict",
            "Release",
            "StdAlloc"
    ))

    test("MimallocRuntimeTests", listOf(
            "Runtime",
            "MainTests",
            "LegacyMemoryManager",
            "Strict",
            "Release",
            "Mimalloc",
            "OptAlloc"
    ))

    test("ExperimentalMMRuntimeTests", listOf(
            "Runtime",
            "MainTests",
            "ExperimentalMemoryManager",
            "Release",
            "Mimalloc",
            "OptAlloc"
    ))
}

val hostRuntime by tasks.registering {
    dependsOn("${hostName}Runtime")
}

val hostRuntimeTests by tasks.registering {
    dependsOn("${hostName}StdAllocRuntimeTests")
    dependsOn("${hostName}MimallocRuntimeTests")
    dependsOn("${hostName}ExperimentalMMRuntimeTests")
}

val hostStdAllocRuntimeTests by tasks.registering {
    dependsOn("${hostName}StdAllocRuntimeTests")
}

val hostMimallocRuntimeTests by tasks.registering {
    dependsOn("${hostName}MimallocRuntimeTests")
}

val hostExperimentalMMRuntimeTests by tasks.registering {
    dependsOn("${hostName}ExperimentalMMRuntimeTests")
}

val assemble by tasks.registering {
    dependsOn(tasks.withType(CompileToBitcode::class).matching {
        it.outputGroup == "main"
    })
}

val clean by tasks.registering {
    doLast {
        delete(buildDir)
    }
}

val generateJsMath by tasks.registering {
    dependsOn(":distCompiler")
    doLast {
        val distDir: File by project
        val jsinteropScript = if (PlatformInfo.isWindows()) "jsinterop.bat" else "jsinterop"
        val jsinterop = "$distDir/bin/$jsinteropScript"
        val targetDir = "$buildDir/generated"

        project.exec {
            commandLine(
                    jsinterop,
                    "-pkg", "kotlinx.interop.wasm.math",
                    "-o", "$targetDir/math",
                    "-target", "wasm32"
            )
        }

        val generated = file("$targetDir/math-build/natives/js_stubs.js")
        val mathJs = file("src/main/js/math.js")
        mathJs.writeText(
            "// NOTE: THIS FILE IS AUTO-GENERATED!\n" +
            "// Run ':runtime:generateJsMath' to re-generate it.\n\n"
        )
        mathJs.appendText(generated.readText())
    }
}

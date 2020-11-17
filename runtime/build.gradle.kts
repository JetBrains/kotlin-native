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
    targets = targetList

    create("Runtime", file("src/main")) {
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

    create("Mimalloc", file("src/mimalloc")) {
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

    create("Launcher", file("src/launcher")) {
        includeRuntime()
    }

    create("Debug", file("src/debug")) {
        includeRuntime()
    }

    create("StdAlloc", file("src/std_alloc"))
    create("OptAlloc", file("src/opt_alloc"))

    create("ExceptionsSupport", file("src/exceptions_support")) {
        includeRuntime()
    }

    create("Release", file("src/release")) {
        includeRuntime()
    }

    create("Strict", file("src/strict")) {
        includeRuntime()
    }

    create("Relaxed", file("src/relaxed")) {
        includeRuntime()
    }

    create("ProfileRuntime", file("src/profile_runtime"))

    create("Objc", file("src/objc")) {
        includeRuntime()
    }

    create("TestSupport", file("src/test_support"), outputGroup = "test") {
        includeRuntime()
        dependsOn("downloadGoogleTest")
        headersDirs += googletest.headersDirs
    }

    create("LegacyMemoryManager", file("src/legacymm")) {
        includeRuntime()
    }

    create("ExperimentalMemoryManager", file("src/mm")) {
        includeRuntime()
    }
}

targetList.forEach { targetName ->
    createTestTask(
            project,
            "StdAlloc",
            "${targetName}StdAllocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}LegacyMemoryManager",
                "${targetName}Strict",
                "${targetName}Release",
                "${targetName}StdAlloc"
            )
    ) {
        includeRuntime()
    }

    createTestTask(
            project,
            "Mimalloc",
            "${targetName}MimallocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}LegacyMemoryManager",
                "${targetName}Strict",
                "${targetName}Release",
                "${targetName}Mimalloc",
                "${targetName}OptAlloc"
            )
    ) {
        includeRuntime()
    }

    createTestTask(
            project,
            "ExperimentalMM",
            "${targetName}ExperimentalMMRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}ExperimentalMemoryManager",
                "${targetName}Release",
                "${targetName}Mimalloc",
                "${targetName}OptAlloc"
            )
    ) {
        includeRuntime()
    }

    tasks.register("${targetName}RuntimeTests") {
        dependsOn("${targetName}StdAllocRuntimeTests")
        dependsOn("${targetName}MimallocRuntimeTests")
        dependsOn("${targetName}ExperimentalMMRuntimeTests")
    }
}

val hostRuntime by tasks.registering {
    dependsOn("${hostName}Runtime")
}

val hostRuntimeTests by tasks.registering {
    dependsOn("${hostName}RuntimeTests")
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

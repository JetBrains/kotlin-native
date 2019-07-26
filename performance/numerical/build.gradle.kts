/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmark.BenchmarkingPlugin
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("benchmarking")
}

benchmark {
    applicationName = "BellardPi"
    commonSrcDirs = listOf("../../tools/benchmarks/shared/src", "src/main/kotlin", "../shared/src/main/kotlin")
    jvmSrcDirs = listOf("../shared/src/main/kotlin-jvm")
    nativeSrcDirs = listOf("../shared/src/main/kotlin-native/common")
    mingwSrcDirs = listOf("../shared/src/main/kotlin-native/mingw")
    posixSrcDirs = listOf("../shared/src/main/kotlin-native/posix")
    linkerOpts = listOf("$buildDir/pi.o")
}

val compileLibary by tasks.creating {
    doFirst {
        mkdir(buildDir)

        project.withConvention(ExecClang::class) {
            execKonanClang(HostManager.host) {
                args("-O3")
                args("-c", "$projectDir/src/nativeInterop/cinterop/pi.c")
                args("-o", "$buildDir/pi.o")
            }
        }
    }
}

val native = kotlin.targets.getByName("native") as KotlinNativeTarget
native.apply {
    compilations["main"].cinterops {
        create("pi") {
            headers("$projectDir/src/nativeInterop/cinterop/pi.h")
        }
    }
    binaries.getExecutable(BenchmarkingPlugin.NATIVE_EXECUTABLE_NAME, "RELEASE").linkTask.dependsOn(compileLibary)
}

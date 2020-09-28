package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.visibleName

class CacheTesting(private val buildCacheTaskProvider: TaskProvider<Task>,
                   val compilerArgs: List<String>, val isDynamic: Boolean) {
    val buildCacheTask: Task
        get() = buildCacheTaskProvider.get()
}

fun configureCacheTesting(project: Project): CacheTesting? {
    val cacheKindString = project.findProperty("test_with_cache_kind") as String? ?: return null
    val isDynamic = when (cacheKindString) {
        "dynamic" -> true
        "static" -> false
        else -> error(cacheKindString)
    }

    val cacheKind = if (isDynamic) {
        CompilerOutputKind.DYNAMIC_CACHE
    } else {
        CompilerOutputKind.STATIC_CACHE
    }

    val target = project.testTarget

    val cacheDir = project.file("${project.buildDir}/cache")
    val cacheFile = "$cacheDir/stdlib-cache"
    val dist = project.kotlinNativeDist
    val stdlib = "$dist/klib/common/stdlib"
    val compilerArgs = listOf("-Xcached-library=$stdlib,$cacheFile")

    val buildCacheTask = project.tasks.register("buildStdlibCache", Task::class.java) {
        it.doFirst {
            cacheDir.mkdirs()
        }
        it.dependsOnDist()
        val args = listOf("-p", cacheKind.visibleName, "-Xmake-cache=$stdlib", "-no-default-libs", "-nostdlib", "-g")
        project.compileKotlinNative(args, output = cacheDir.toPath().resolve("stdlib-cache"), target)
    }

    return CacheTesting(buildCacheTask, compilerArgs, isDynamic)
}
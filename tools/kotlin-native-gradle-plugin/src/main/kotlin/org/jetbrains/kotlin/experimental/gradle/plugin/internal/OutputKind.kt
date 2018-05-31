package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeBinary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

enum class OutputKind(
        val compilerOutputKind: CompilerOutputKind,
        val binaryClass: Class<out KotlinNativeBinary>,
        val runtimeUsageName: String? = null,
        val linkUsageName: String? = null
) {
    EXECUTABLE(CompilerOutputKind.PROGRAM, KotlinNativeExecutableImpl::class.java, Usage.NATIVE_RUNTIME, null),
    KLIBRARY(CompilerOutputKind.LIBRARY, KotlinNativeKLibraryImpl::class.java, null, KotlinNativeUsage.KLIB)
}
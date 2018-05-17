package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.konan.target.CompilerOutputKind

enum class KonanOutput(val cliOption: String, val kind: CompilerOutputKind) {
    PROGRAM("program", CompilerOutputKind.PROGRAM),
    DYNAMIC("dynamic", CompilerOutputKind.DYNAMIC),
    FRAMEWORK("framework", CompilerOutputKind.FRAMEWORK),
    LIBRARY("library", CompilerOutputKind.LIBRARY),
    BITCODE("bitcode", CompilerOutputKind.BITCODE)
}
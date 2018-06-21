package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.KonanVersion


interface KonanLibraryVersioning {
    val libraryVersion: String?
    val compilerVersion: KonanVersion
    val abiVersion: Int
}

package org.jetbrains.kotlin.konan.util

data class KonanAbiVersion(val version: Int) {
    companion object {
        val CURRENT = KonanAbiVersion(1)
    }
}
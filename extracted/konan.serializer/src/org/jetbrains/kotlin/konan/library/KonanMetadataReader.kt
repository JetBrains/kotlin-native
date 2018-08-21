package org.jetbrains.kotlin.konan.library

interface MetadataReader {
    fun loadSerializedModule(): ByteArray
    fun loadSerializedPackageFragment(fqName: String): ByteArray
}

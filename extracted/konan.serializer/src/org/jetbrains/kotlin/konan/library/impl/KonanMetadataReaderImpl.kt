package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.MetadataReader

class MetadataReaderImpl(library: KonanLibrary) : MetadataReader, KonanLibrary by library {

    override fun loadSerializedModule(): ByteArray = moduleHeaderFile.readBytes()
    override fun loadSerializedPackageFragment(fqName: String): ByteArray = packageFile(fqName).readBytes()
}

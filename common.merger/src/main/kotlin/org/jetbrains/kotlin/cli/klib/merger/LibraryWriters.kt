package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.backend.konan.library.impl.MetadataWriterImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.konan.target.KonanTarget

internal class MultiTargetLibraryWriter(
        override val libDir: File,
        val nopack: Boolean = false
) : KonanLibraryLayout {
    override val libraryName = libDir.path
    private val klibFile
        get() = File("${libDir.path}.$KLIB_FILE_EXTENSION")

    private val manifestProperties = Properties()

    init {
        libDir.deleteRecursively()
        klibFile.delete()
        libDir.mkdirs()
        linkdataDir.mkdirs()
        resourcesDir.mkdirs()
    }

    fun addLinkData(linkData: LinkData) {
        MetadataWriterImpl(this).addLinkData(linkData)
    }

    fun addManifestAddend(properties: Properties) {
        manifestProperties.putAll(properties)
    }

    fun commit() {
        manifestProperties.saveToFile(manifestFile)
        if (!nopack) {
            libDir.zipDirAs(klibFile)
            libDir.deleteRecursively()
        }
    }
}


internal class SingleTargetLibraryWriterImpl(
        override val libDir: File,
        override val target: KonanTarget
) : KonanLibraryLayout {

    override val libraryName = libDir.path

    init {
        targetDir.mkdirs()
        kotlinDir.mkdirs()
        nativeDir.mkdirs()
        includedDir.mkdirs()
    }

    fun addKotlinBitcode(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(kotlinDir, basename))
    }

    fun addNativeBitcode(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(nativeDir, basename))
    }

    fun addIncludedBinary(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(includedDir, basename))
    }

    fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        dataFlowGraphFile.writeBytes(dataFlowGraph)
    }

}



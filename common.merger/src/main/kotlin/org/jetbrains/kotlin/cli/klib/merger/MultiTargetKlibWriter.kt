package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.backend.konan.library.impl.MetadataWriterImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.parseKonanAbiVersion
import org.jetbrains.kotlin.konan.parseKonanVersion
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager

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
        File(library).recursiveCopyTo(File(nativeDir, basename))
    }

    fun addIncludedBinary(library: String) {
        val basename = File(library).name
        File(library).recursiveCopyTo(File(includedDir, basename))
    }

    fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        dataFlowGraphFile.writeBytes(dataFlowGraph)
    }

}

class MultiTargetKlibWriter(val libDir: File,
                            private val repository: File,
                            private val hostManager: PlatformManager) {
    private lateinit var manifest: java.util.Properties
    private lateinit var linkData: LinkData
    private val libs = mutableListOf<KonanLibrary>()

    fun addManifestAddend(manifest: java.util.Properties) {
        this.manifest = manifest
    }

    fun addLinkData(linkData: LinkData) {
        this.linkData = linkData
    }

    fun addKonanLibrary(lib: KonanLibrary) {
        libs.add(lib)
    }

    private fun checkInitialized() {
        if (!(::manifest.isInitialized)) {
            TODO("manifest must be not null")
        }
        if (!(::linkData.isInitialized)) {
            TODO("linkData must be not null")
        }

        if (libs.isEmpty()) {
            TODO("manifest has to be not null")
        }
    }

    private fun writeModuleDescriptorAndManifest(output: File) {
        val multiTargetLibraryWriter = MultiTargetLibraryWriter(output, nopack = true)
        multiTargetLibraryWriter.addLinkData(linkData)
        multiTargetLibraryWriter.addManifestAddend(manifest)
        multiTargetLibraryWriter.commit()
    }

    private fun writeSpecificParts(output: File, lib: KonanLibrary) {
        val targetVersion = lib.manifestProperties.getProperty(KLIB_PROPERTY_COMPILER_VERSION).parseKonanVersion()
        val abiVersion = lib.manifestProperties.getProperty(KLIB_PROPERTY_ABI_VERSION).parseKonanAbiVersion()
        for (target in lib.targetList.map { hostManager.targetByName(it) }) {
            val resolver = defaultResolver(
                    listOf(repository.absolutePath), emptyList(), target, Distribution(),
                    skipCurrentDir = true,
                    abiVersion = abiVersion,
                    compatibleCompilerVersions = listOf(targetVersion))
            val currentLib = resolver.resolve(lib.libraryName)

            val singleTargetLibraryWriterImpl = SingleTargetLibraryWriterImpl(output, target)
            currentLib.dataFlowGraph?.let { singleTargetLibraryWriterImpl.addDataFlowGraph(it) }

            currentLib.kotlinBitcodePaths.forEach {
                singleTargetLibraryWriterImpl.addKotlinBitcode(it)
            }

            currentLib.includedPaths.forEach {
                singleTargetLibraryWriterImpl.addIncludedBinary(it)
            }

            val kotlinIncludePaths = currentLib.kotlinBitcodePaths.toSet()
            currentLib.bitcodePaths.forEach {
                if (!kotlinIncludePaths.contains(it)) {
                    singleTargetLibraryWriterImpl.addNativeBitcode(it)
                }
            }
        }
    }

    fun commit() {
        checkInitialized()
        for (lib in libs) {
            writeSpecificParts(libDir, lib)
        }
        writeModuleDescriptorAndManifest(libDir)
    }
}


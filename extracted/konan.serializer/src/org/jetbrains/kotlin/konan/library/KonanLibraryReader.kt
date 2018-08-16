package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.properties.Properties

interface KonanLibraryReader {
    val libraryName: String
    val uniqueName: String
    val bitcodePaths: List<String>
    val includedPaths: List<String>
    val linkerOpts: List<String>
    val unresolvedDependencies: List<String>
    val dataFlowGraph: ByteArray?
    val isDefaultLibrary: Boolean get() = false
    val isNeededForLink: Boolean get() = true
    val manifestProperties: Properties
    val moduleHeaderData: ByteArray
    fun packageMetadata(fqName: String): ByteArray
    fun markPackageAccessed(fqName: String)
    fun moduleDescriptor(specifics: LanguageVersionSettings): ModuleDescriptor
}

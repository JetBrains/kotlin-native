/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.library.impl

import org.jetbrains.kotlin.backend.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.MetadataReader
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.library.UnresolvedLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.konan.util.substitute
import org.jetbrains.kotlin.konan.*

class LibraryReaderImpl(var libraryFile: File,
                        val target: KonanTarget? = null,
                        override val isDefaultLibrary: Boolean = false)
    : KonanLibraryReader {

    @Deprecated("Use the primary constructor")
    constructor(libraryFile: File,
        currentAbiVersion: Int,
        target: KonanTarget? = null,
        isDefaultLibrary: Boolean = false) : this(libraryFile, target, isDefaultLibrary)

    // For the zipped libraries inPlace gives files from zip file system
    // whereas realFiles extracts them to /tmp.
    // For unzipped libraries inPlace and realFiles are the same
    // providing files in the library directory.
    private val inPlace = KonanLibrary(libraryFile, target)
    private val realFiles = inPlace.realFiles

    override val manifestProperties: Properties by lazy {
        val properties = inPlace.manifestFile.loadProperties()
        if (target != null) substitute(properties, defaultTargetSubstitutions(target))
        properties
    }

    override val abiVersion: Int
        get() {
            val manifestAbiVersion = manifestProperties.getProperty("abi_version")!!
            return manifestAbiVersion.toInt()
        }

    val targetList = inPlace.targetsDir.listFiles.map{it.name}
    override val dataFlowGraph by lazy { inPlace.dataFlowGraphFile.let { if (it.exists) it.readBytes() else null } }

    override val libraryName 
        get() = inPlace.libraryName

    override val uniqueName
        get() = manifestProperties.propertyString("unique_name")!!

    override val libraryVersion: String?
        get() = manifestProperties.propertyString("library_version")

    override val compilerVersion
        get() = manifestProperties.propertyString("compiler_version")!!.parseKonanVersion()

    override val bitcodePaths: List<String>
        get() = (realFiles.kotlinDir.listFilesOrEmpty + realFiles.nativeDir.listFilesOrEmpty)
                .map { it.absolutePath }

    override val includedPaths: List<String>
        get() = (realFiles.includedDir.listFilesOrEmpty).map { it.absolutePath }

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList("linkerOpts", target!!.visibleName)

    override val unresolvedDependencies: List<UnresolvedLibrary>
        get() = manifestProperties.propertyList("depends")
                .map {
                    UnresolvedLibrary(it, manifestProperties.propertyString("dependency_version_$it")
                )}

    val resolvedDependencies = mutableListOf<LibraryReaderImpl>()


    // Metadata part

    override lateinit var metadataReader: MetadataReader

    override fun addMetadataReader(factory: (KonanLibrary)-> MetadataReader) {
        metadataReader = factory(inPlace)
    }

    override val isNeededForLink: Boolean get() = metadataReader.isNeededForLink
    override fun markPackageAccessed(fqName: String) = metadataReader.markPackageAccessed(fqName)
}

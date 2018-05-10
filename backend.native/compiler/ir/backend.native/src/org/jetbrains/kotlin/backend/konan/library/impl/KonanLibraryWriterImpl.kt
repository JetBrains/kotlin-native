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

import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.library.KonanLibrary
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class FileBasedLibraryWriter (
    val file: File): KonanLibraryWriter {
}

class LibraryWriterImpl(override val libDir: File, moduleName: String,
    override val abiVersion: Int,
    override val compilerVersion: KonanVersion,
    override val libraryVersion: String? = null,
    override val target: KonanTarget?,
    val nopack: Boolean = false):
        FileBasedLibraryWriter(libDir), KonanLibrary {

    constructor(path: String, moduleName: String, currentAbiVersion: Int,
        compilerVersion: KonanVersion, libraryVersion: String?,
        target:KonanTarget?, nopack: Boolean): 
        this(File(path), moduleName, currentAbiVersion, compilerVersion, libraryVersion, target, nopack)

    override val libraryName = libDir.path
    val klibFile 
       get() = File("${libDir.path}.klib")

    // TODO: Experiment with separate bitcode files.
    // Per package or per class.
    val mainBitcodeFile = File(kotlinDir, "program.kt.bc")
    override val mainBitcodeFileName = mainBitcodeFile.path
    val manifestProperties = Properties()

    init {
        // TODO: figure out the proper policy here.
        libDir.deleteRecursively()
        klibFile.delete()
        libDir.mkdirs()
        linkdataDir.mkdirs()
        targetDir.mkdirs()
        kotlinDir.mkdirs()
        nativeDir.mkdirs()
        includedDir.mkdirs()
        resourcesDir.mkdirs()
        // TODO: <name>:<hash> will go somewhere around here.
        manifestProperties.setProperty("unique_name", "$moduleName")
        manifestProperties.setProperty("abi_version", "$abiVersion")
        libraryVersion ?. let { manifestProperties.setProperty("library_version", it) }
        manifestProperties.setProperty("compiler_version", "$compilerVersion")
    }

    var llvmModule: LLVMModuleRef? = null

    override fun addKotlinBitcode(llvmModule: LLVMModuleRef) {
        this.llvmModule = llvmModule
        LLVMWriteBitcodeToFile(llvmModule, mainBitcodeFileName)
    }

    override fun addLinkData(linkData: LinkData) {
        MetadataWriterImpl(this).addLinkData(linkData)
    }

    override fun addNativeBitcode(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(nativeDir, basename)) 
    }

    override fun addIncludedBinary(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(includedDir, basename)) 
    }

    override fun addLinkDependencies(libraries: List<KonanLibraryReader>) {
        if (libraries.isEmpty()) {
            manifestProperties.remove("depends") 
            // make sure there are no leftovers from the .def file.
            return
        } else {
            val newValue = libraries .map { it.uniqueName } . joinToString(" ")
            manifestProperties.setProperty("depends", newValue)
            libraries.forEach { it ->
                if (it.libraryVersion != null) {
                    manifestProperties.setProperty("dependency_version_${it.uniqueName}", it.libraryVersion)
                }
            }
        }
    }

    override fun addManifestAddend(path: String) {
        val properties = File(path).loadProperties()
        manifestProperties.putAll(properties)
    }

    override fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        dataFlowGraphFile.writeBytes(dataFlowGraph)
    }

    override fun commit() {
        manifestProperties.saveToFile(manifestFile)
        if (!nopack) {
            libDir.zipDirAs(klibFile)
            libDir.deleteRecursively()
        }
    }
}

internal fun buildLibrary(
    natives: List<String>, 
    included: List<String>,
    linkDependencies: List<KonanLibraryReader>,
    linkData: LinkData, 
    abiVersion: Int,
    compilerVersion: KonanVersion,
    libraryVersion: String?,
    target: KonanTarget, 
    output: String, 
    moduleName: String, 
    llvmModule: LLVMModuleRef, 
    nopack: Boolean, 
    manifest: String?,
    dataFlowGraph: ByteArray?): KonanLibraryWriter {

    val library = LibraryWriterImpl(output, moduleName, abiVersion, compilerVersion, libraryVersion, target, nopack)

    library.addKotlinBitcode(llvmModule)
    library.addLinkData(linkData)
    natives.forEach {
        library.addNativeBitcode(it)
    }
    included.forEach {
        library.addIncludedBinary(it)
    }
    manifest ?.let { library.addManifestAddend(it) }
    library.addLinkDependencies(linkDependencies)
    dataFlowGraph?.let { library.addDataFlowGraph(it) }

    library.commit()
    return library
}


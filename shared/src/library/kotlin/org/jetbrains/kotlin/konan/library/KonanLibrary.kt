package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.util.DEFFILE_PROPERTY_MODULES
import org.jetbrains.kotlin.library.*

const val KLIB_PROPERTY_LINKED_OPTS = "linkerOpts"
const val KLIB_PROPERTY_INTEROP = "interop"
const val KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS = "exportForwardDeclarations"
const val KLIB_PROPERTY_INCLUDED_HEADERS = "includedHeaders"

// .def file properties are copied to manifest as is:
const val KLIB_PROPERTY_INTEROP_MODULES: String = DEFFILE_PROPERTY_MODULES

interface TargetedLibrary {
    val targetList: List<String>
    val includedPaths: List<String>
}

interface BitcodeLibrary : TargetedLibrary {
    val bitcodePaths: List<String>
}

interface KonanLibrary : BitcodeLibrary, KotlinLibrary {
    val linkerOpts: List<String>
}

val KonanLibrary.isInterop
    get() = manifestProperties.getProperty(KLIB_PROPERTY_INTEROP) == "true"

val KonanLibrary.packageFqName: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_PACKAGE)

val KonanLibrary.exportForwardDeclarations
    get() = manifestProperties.propertyList(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS, escapeInQuotes = true)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

val KonanLibrary.includedHeaders
    get() = manifestProperties.propertyList(KLIB_PROPERTY_INCLUDED_HEADERS, escapeInQuotes = true)

val KonanLibrary.interopModules: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_INTEROP_MODULES, escapeInQuotes = true)

package org.jetbrains.kotlin.serialization.konan.interop

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.konan.interop.InteropFqNames
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment

fun createInteropLibrary(reader: KonanLibraryReader): InteropLibrary? {

    if (reader.manifestProperties.getProperty("interop") != "true") return null

    val pkg = reader.manifestProperties.getProperty("package")
            ?: error("Inconsistent manifest: interop library ${reader.libraryName} should have `package` specified")

    val exportForwardDeclarations = reader.manifestProperties
            .getProperty("exportForwardDeclarations").split(' ')
            .map { it.trim() }.filter { it.isNotEmpty() }
            .map { FqName(it) }

    return InteropLibraryImpl(FqName(pkg), exportForwardDeclarations)
}

interface InteropLibrary {

    fun createSyntheticPackages(
            module: ModuleDescriptor,
            konanPackageFragments: List<KonanPackageFragment>
    ): List<PackageFragmentDescriptor>
}

private class InteropLibraryImpl(
        private val packageFqName: FqName,
        private val exportForwardDeclarations: List<FqName>
) : InteropLibrary {

    override fun createSyntheticPackages(
            module: ModuleDescriptor,
            konanPackageFragments: List<KonanPackageFragment>
    ): List<PackageFragmentDescriptor> {

        val interopPackageFragments = konanPackageFragments.filter { it.fqName == packageFqName }

        val result = mutableListOf<PackageFragmentDescriptor>()

        // Allow references to forwarding declarations to be resolved into classifiers declared in this library:
        listOf(InteropFqNames.cNamesStructs, InteropFqNames.objCNamesClasses, InteropFqNames.objCNamesProtocols).mapTo(result) { fqName ->
            ClassifierAliasingPackageFragmentDescriptor(interopPackageFragments, module, fqName)
        }
        // TODO: use separate namespaces for structs, enums, Objective-C protocols etc.

        result.add(ExportedForwardDeclarationsPackageFragmentDescriptor(
                module, packageFqName, exportForwardDeclarations
        ))

        return result
    }
}

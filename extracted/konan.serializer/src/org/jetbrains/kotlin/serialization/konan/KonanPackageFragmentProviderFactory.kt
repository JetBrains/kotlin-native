package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.interop.InteropFqNames
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager

interface KonanPackageFragmentProviderFactory {

    fun createPackageFragmentProvider(
            reader: KonanLibraryReader,
            fragmentNames: List<String>,
            storageManager: StorageManager,
            moduleDescriptor: ModuleDescriptor,
            configuration: DeserializationConfiguration
    ): PackageFragmentProvider
}

open class DefaultKonanPackageFragmentProviderFactoryImpl : KonanPackageFragmentProviderFactory {

    override fun createPackageFragmentProvider(
            reader: KonanLibraryReader,
            fragmentNames: List<String>,
            storageManager: StorageManager,
            moduleDescriptor: ModuleDescriptor,
            configuration: DeserializationConfiguration
    ): PackageFragmentProvider {

        val deserializedPackageFragments = fragmentNames.map{
            KonanPackageFragment(it, reader, storageManager, moduleDescriptor)
        }

        val syntheticPackageFragments = getSyntheticPackageFragments(
                reader,
                moduleDescriptor,
                deserializedPackageFragments)

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments + syntheticPackageFragments)

        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(
                moduleDescriptor,
                notFoundClasses,
                KonanSerializerProtocol)

        val components = DeserializationComponents(
                storageManager,
                moduleDescriptor,
                configuration,
                DeserializedClassDataFinder(provider),
                annotationAndConstantLoader,
                provider,
                LocalClassifierTypeSettings.Default,
                ErrorReporter.DO_NOTHING,
                LookupTracker.DO_NOTHING,
                NullFlexibleTypeDeserializer,
                emptyList(),
                notFoundClasses,
                ContractDeserializer.DEFAULT,
                extensionRegistryLite = KonanSerializerProtocol.extensionRegistry)

        for (packageFragment in deserializedPackageFragments) {
            packageFragment.initialize(components)
        }

        return provider
    }

    protected open fun getSyntheticPackageFragments(
            reader: KonanLibraryReader,
            moduleDescriptor: ModuleDescriptor,
            konanPackageFragments: List<KonanPackageFragment>
    ): List<PackageFragmentDescriptor> {

        if (reader.manifestProperties.getProperty("interop") != "true") return emptyList()

        val packageFqName = reader.manifestProperties.getProperty("package")?.let { FqName(it) }
                ?: error("Inconsistent manifest: interop library ${reader.libraryName} should have `package` specified")

        val exportForwardDeclarations = reader.manifestProperties.getProperty("exportForwardDeclarations")
                .split(' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { FqName(it) }

        val interopPackageFragments = konanPackageFragments.filter { it.fqName == packageFqName }

        val result = mutableListOf<PackageFragmentDescriptor>()

        // Allow references to forwarding declarations to be resolved into classifiers declared in this library:
        listOf(InteropFqNames.cNamesStructs, InteropFqNames.objCNamesClasses, InteropFqNames.objCNamesProtocols).mapTo(result) { fqName ->
            ClassifierAliasingPackageFragmentDescriptor(interopPackageFragments, moduleDescriptor, fqName)
        }
        // TODO: use separate namespaces for structs, enums, Objective-C protocols etc.

        result.add(ExportedForwardDeclarationsPackageFragmentDescriptor(moduleDescriptor, packageFqName, exportForwardDeclarations))

        return result
    }
}

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.createKonanModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.konan.interop.createInteropLibrary
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun parsePackageFragment(packageData: ByteArray): KonanProtoBuf.LinkDataPackageFragment =
        KonanProtoBuf.LinkDataPackageFragment.parseFrom(packageData, KonanSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryData: ByteArray): KonanProtoBuf.LinkDataLibrary =
        KonanProtoBuf.LinkDataLibrary.parseFrom(libraryData, KonanSerializerProtocol.extensionRegistry)

fun emptyPackages(libraryData: ByteArray) = parseModuleHeader(libraryData).emptyPackageList

fun createKonanPackageFragmentProvider(
        reader: KonanLibraryReader,
        fragmentNames: List<String>,
        storageManager: StorageManager, module: ModuleDescriptor,
        configuration: DeserializationConfiguration): PackageFragmentProvider {

    val packageFragments = fragmentNames.map{
        KonanPackageFragment(it, reader, storageManager, module)
    }

    val interopLibrary = createInteropLibrary(reader)

    val syntheticInteropPackageFragments =
            interopLibrary?.createSyntheticPackages(module, packageFragments) ?: emptyList()

    val provider = PackageFragmentProviderImpl(packageFragments + syntheticInteropPackageFragments)

    val notFoundClasses = NotFoundClasses(storageManager, module)

    val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(module, notFoundClasses, KonanSerializerProtocol)

    val components = DeserializationComponents(
            storageManager, module, configuration,
            DeserializedClassDataFinder(provider),
            annotationAndConstantLoader,
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING, NullFlexibleTypeDeserializer,
            emptyList(), notFoundClasses, ContractDeserializer.DEFAULT, extensionRegistryLite = KonanSerializerProtocol.extensionRegistry )

    for (packageFragment in packageFragments) {
        packageFragment.initialize(components)
    }

    return provider
}

internal fun deserializeModule(languageVersionSettings: LanguageVersionSettings,
                               reader: KonanLibraryReader): ModuleDescriptorImpl {

    val libraryProto = parseModuleHeader(reader.moduleHeaderData)

    val moduleName = libraryProto.moduleName

    val storageManager = LockBasedStorageManager()
    val moduleDescriptor = createKonanModuleDescriptor(
            Name.special(moduleName), storageManager,
            origin = DeserializedKonanModuleOrigin(reader)
    )
    val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    val provider = createKonanPackageFragmentProvider(
            reader,
            libraryProto.packageFragmentNameList,
            storageManager,
            moduleDescriptor, deserializationConfiguration)

    moduleDescriptor.initialize(provider)

    return moduleDescriptor
}

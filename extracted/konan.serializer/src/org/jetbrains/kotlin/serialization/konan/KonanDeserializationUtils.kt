package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.createKonanModuleDescriptor
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun parsePackageFragment(packageData: ByteArray): KonanProtoBuf.LinkDataPackageFragment =
        KonanProtoBuf.LinkDataPackageFragment.parseFrom(packageData, KonanSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryData: ByteArray): KonanProtoBuf.LinkDataLibrary =
        KonanProtoBuf.LinkDataLibrary.parseFrom(libraryData, KonanSerializerProtocol.extensionRegistry)

fun emptyPackages(libraryData: ByteArray) = parseModuleHeader(libraryData).emptyPackageList

fun deserializeModule(
        reader: KonanLibraryReader,
        languageVersionSettings: LanguageVersionSettings,
        factory: KonanPackageFragmentProviderFactory,
        storageManager: StorageManager = LockBasedStorageManager()
): ModuleDescriptorImpl {

    val libraryProto = parseModuleHeader(reader.moduleHeaderData)

    val moduleName = libraryProto.moduleName

    val moduleDescriptor = createKonanModuleDescriptor(
            Name.special(moduleName),
            storageManager,
            origin = DeserializedKonanModuleOrigin(reader)
    )
    val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    val provider = factory.createPackageFragmentProvider(
            reader,
            libraryProto.packageFragmentNameList,
            storageManager,
            moduleDescriptor,
            deserializationConfiguration)

    moduleDescriptor.initialize(provider)

    return moduleDescriptor
}

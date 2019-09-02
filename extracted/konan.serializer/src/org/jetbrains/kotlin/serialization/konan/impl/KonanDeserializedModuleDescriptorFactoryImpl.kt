package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KonanModuleDescriptorFactory
import org.jetbrains.kotlin.descriptors.konan.isKonanStdlib
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.konan.*
import org.jetbrains.kotlin.storage.StorageManager

internal class KonanDeserializedModuleDescriptorFactoryImpl(
        override val descriptorFactory: KonanModuleDescriptorFactory,
        override val packageFragmentsFactory: KonanDeserializedPackageFragmentsFactory
): KonanDeserializedModuleDescriptorFactory {

    override fun createDescriptor(
            library: KonanLibrary,
            languageVersionSettings: LanguageVersionSettings,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns,
            packageAccessedHandler: PackageAccessedHandler?
    ) = createDescriptorOptionalBuiltIns(library, languageVersionSettings, storageManager, builtIns, packageAccessedHandler)

    override fun createDescriptorAndNewBuiltIns(
            library: KonanLibrary,
            languageVersionSettings: LanguageVersionSettings,
            storageManager: StorageManager,
            packageAccessedHandler: PackageAccessedHandler?
    ) = createDescriptorOptionalBuiltIns(library, languageVersionSettings, storageManager, null, packageAccessedHandler)

    private fun createDescriptorOptionalBuiltIns(
            library: KonanLibrary,
            languageVersionSettings: LanguageVersionSettings,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns?,
            packageAccessedHandler: PackageAccessedHandler?
    ): ModuleDescriptorImpl {

        val libraryProto = parseModuleHeader(library.moduleHeaderData)

        val moduleName = Name.special(libraryProto.moduleName)
        val moduleOrigin = DeserializedKonanModuleOrigin(library)

        val moduleDescriptor = if (builtIns != null )
            descriptorFactory.createDescriptor(moduleName, storageManager, builtIns, moduleOrigin)
        else
            descriptorFactory.createDescriptorAndNewBuiltIns(moduleName, storageManager, moduleOrigin)

        val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

        val provider = createPackageFragmentProvider(
                library,
                packageAccessedHandler,
                libraryProto.packageFragmentNameList,
                storageManager,
                moduleDescriptor,
                deserializationConfiguration)

        if (!moduleDescriptor.isKonanStdlib())
            moduleDescriptor.initialize(provider)
        else {
            // [K][Suspend]FunctionN belong to stdlib.
            val packagePartProviders = mutableListOf(provider)
            packagePartProviders += functionInterfacePackageFragmentProvider(storageManager, moduleDescriptor)
            moduleDescriptor.initialize(CompositePackageFragmentProvider(packagePartProviders))
        }

        return moduleDescriptor
    }

    private fun createPackageFragmentProvider(
            library: KonanLibrary,
            packageAccessedHandler: PackageAccessedHandler?,
            packageFragmentNames: List<String>,
            storageManager: StorageManager,
            moduleDescriptor: ModuleDescriptor,
            configuration: DeserializationConfiguration
    ): PackageFragmentProvider {

        val deserializedPackageFragments = packageFragmentsFactory.createDeserializedPackageFragments(
                library, packageFragmentNames, moduleDescriptor, packageAccessedHandler, storageManager)

        val syntheticPackageFragments = packageFragmentsFactory.createSyntheticPackageFragments(
                library, deserializedPackageFragments, moduleDescriptor)

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
                ContractDeserializerImpl(configuration, storageManager),
                extensionRegistryLite = KonanSerializerProtocol.extensionRegistry)

        for (packageFragment in deserializedPackageFragments) {
            packageFragment.initialize(components)
        }

        return provider
    }
}

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KonanModuleDescriptorFactory
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

interface KonanDeserializedModuleDescriptorFactory {

    val descriptorFactory: KonanModuleDescriptorFactory
    val packageFragmentsFactory: KonanDeserializedPackageFragmentsFactory

    fun createDescriptor(
            library: KonanLibrary,
            languageVersionSettings: LanguageVersionSettings,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns,
            packageAccessedHandler: PackageAccessedHandler? = null
    ): ModuleDescriptorImpl

    /**
     * Please use this method with care: As far as it creates an instance of [KotlinBuiltIns] it should be
     * normally used for creation of the very first (e.g. "stdlib") module in the set of created modules.
     */
    fun createDescriptorAndNewBuiltIns(
            library: KonanLibrary,
            languageVersionSettings: LanguageVersionSettings,
            storageManager: StorageManager,
            packageAccessedHandler: PackageAccessedHandler? = null
    ): ModuleDescriptorImpl
}

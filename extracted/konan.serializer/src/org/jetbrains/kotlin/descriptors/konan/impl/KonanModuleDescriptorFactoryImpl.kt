package org.jetbrains.kotlin.descriptors.konan.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KonanModuleDescriptorFactory
import org.jetbrains.kotlin.descriptors.konan.KonanModuleOrigin
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

internal object KonanModuleDescriptorFactoryImpl: KonanModuleDescriptorFactory {

    override fun createDescriptor(
            name: Name,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns,
            origin: KonanModuleOrigin,
            customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ) = ModuleDescriptorImpl(
            name,
            storageManager,
            builtIns,
            capabilities = customCapabilities + mapOf(KonanModuleOrigin.CAPABILITY to origin))

    override fun createDescriptorAndNewBuiltIns(
            name: Name,
            storageManager: StorageManager,
            origin: KonanModuleOrigin,
            customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ): ModuleDescriptorImpl {

        val builtIns = KonanBuiltIns(storageManager)

        val moduleDescriptor = createDescriptor(name, storageManager, builtIns, origin, customCapabilities)
        builtIns.builtInsModule = moduleDescriptor

        return moduleDescriptor
    }
}

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

sealed class KonanModuleOrigin {

    companion object {
        val CAPABILITY = ModuleDescriptor.Capability<KonanModuleOrigin>("KonanModuleOrigin")
    }

    // Note: merging these two concepts (CompiledModules and KonanModuleOrigin) for simplicity:
    sealed class CompiledModules : KonanModuleOrigin() {

        data class DeserializedKonanModule(val reader: Any) : CompiledModules()

        // FIXME: ddol: replace `Any` by `KonanLibraryReader` when ready
        object CurrentKonanModule : CompiledModules()
    }


    object SyntheticModules : KonanModuleOrigin()
}


val ModuleDescriptor.konanModuleOrigin get() = this.getCapability(KonanModuleOrigin.CAPABILITY)!!

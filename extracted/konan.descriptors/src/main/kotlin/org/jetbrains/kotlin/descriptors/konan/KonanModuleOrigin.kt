package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

sealed class KonanModuleOrigin {
    companion object {
        val CAPABILITY = ModuleDescriptor.Capability<KonanModuleOrigin>("KonanModuleOrigin")
    }
}

// Note: merging these two concepts (LlvmSymbolOrigin and KonanModuleOrigin) for simplicity:
sealed class LlvmSymbolOrigin : KonanModuleOrigin()

// FIXME: ddol: replace `Any` by `KonanLibraryReader` when ready
data class DeserializedKonanModule(val reader: Any) : LlvmSymbolOrigin()
object CurrentKonanModule : LlvmSymbolOrigin()

object SyntheticModules : KonanModuleOrigin()

val ModuleDescriptor.konanModuleOrigin get() = this.getCapability(KonanModuleOrigin.CAPABILITY)!!

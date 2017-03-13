package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.KonanSharedVariablesManager

abstract internal class KonanBackendContext(val config: KonanConfig) : BackendContext {
    override val builtIns = KonanPlatform.builtIns

    override val sharedVariablesManager by lazy {
        // Creating lazily because builtIns module seems to be incomplete during `link` test;
        // TODO: investigate this.
        KonanSharedVariablesManager(builtIns)
    }
}

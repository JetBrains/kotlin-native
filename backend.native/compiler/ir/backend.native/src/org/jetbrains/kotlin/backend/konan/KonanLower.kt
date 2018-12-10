/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CompilerPhaseManager
import org.jetbrains.kotlin.backend.common.runPhases

internal class KonanLower(val context: Context,
                          val parentPhaser: CompilerPhaseManager<Context, *>) {
    fun lower() {
        val irModule = context.irModule!!
        parentPhaser.createChildManager(irModule, KonanIrModulePhaseRunner).runPhases(irModulePhaseList)
    }
}

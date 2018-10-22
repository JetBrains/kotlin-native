/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.reportCompilationWarning
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.CurrentKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.descriptors.konan.konanModuleOrigin
import org.jetbrains.kotlin.ir.util.report

internal class ObjCExportHeaderGeneratorImpl(val context: Context) : ObjCExportHeaderGenerator(
        context.moduleDescriptor,
        context.getExportedDependencies(),
        context.builtIns
) {

    override fun reportWarning(text: String) {
        context.reportCompilationWarning(text)
    }

    override fun reportWarning(method: FunctionDescriptor, text: String) {
        context.report(
                context.ir.get(method),
                text,
                isError = false
        )
    }
}

private fun Context.getExportedDependencies(): List<ModuleDescriptor> =
        this.moduleDescriptor.allDependencyModules.filter {
            val konanModuleOrigin = it.konanModuleOrigin
            when (konanModuleOrigin) {
                CurrentKonanModuleOrigin, SyntheticModulesOrigin -> false
                is DeserializedKonanModuleOrigin ->
                    konanModuleOrigin.library.libraryFile in this.config.exportedLibraries
            }
        }
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File

internal fun validateExportedLibraries(configuration: CompilerConfiguration, libraryFiles: List<File>) {
    val exportedLibraries = getExportedLibraries(configuration)

    val missingExportedLibraries = exportedLibraries - libraryFiles

    if (missingExportedLibraries.isNotEmpty()) {
        val message = buildString {
            appendln("Following libraries are specified to be exported with -Xexport-library, but not included to the build:")
            missingExportedLibraries.forEach { appendln(it) }
            appendln()
            appendln("Included libraries:")
            libraryFiles.forEach { appendln(it) }
        }

        configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
    }
}

internal fun getExportedLibraries(configuration: CompilerConfiguration) =
        configuration.getList(KonanConfigKeys.EXPORTED_LIBRARIES).map { File(it) }.toSet()

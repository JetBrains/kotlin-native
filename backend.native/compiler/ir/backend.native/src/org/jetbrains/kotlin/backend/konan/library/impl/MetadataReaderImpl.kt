/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.library.impl

import org.jetbrains.kotlin.backend.konan.library.KonanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.deserializeModule
import org.jetbrains.kotlin.konan.library.MetadataReader
import org.jetbrains.kotlin.backend.konan.serialization.emptyPackages
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.konan.library.KonanLibraryReader

class MetadataReaderImpl(library: KonanLibrary) : MetadataReader, KonanLibrary by library {

    val moduleHeaderData: ByteArray by lazy {
        moduleHeaderFile.readBytes()
    }

    override fun packageMetadata(fqName: String)
        = packageFile(fqName).readBytes()

    override var isNeededForLink: Boolean = false
        private set

    private val emptyPackages by lazy { emptyPackages(moduleHeaderData) }

    override fun markPackageAccessed(fqName: String) {
        if (!isNeededForLink // fast path
                && !emptyPackages.contains(fqName)) {
            isNeededForLink = true
        }
    }
}

fun KonanLibraryReader.moduleDescriptor(specifics: LanguageVersionSettings)
        = deserializeModule(specifics, this)

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

package org.jetbrains.kotlin.backend.konan.library

import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget

fun KonanLibrarySearchPathResolver.resolveImmediateLibraries(libraryNames: List<String>,
                                                             noStdLib: Boolean = false,
                                                             noDefaultLibs: Boolean = false): List<LibraryReaderImpl> {
    val userProvidedLibraries: List<LibraryReaderImpl> = libraryNames
            .map { UnresolvedLibrary(it, null) }
            .map { this.resolve(it) }

    val defaultLibraries = defaultLinks(nostdlib = noStdLib, noDefaultLibs = noDefaultLibs)

    // Make sure the user provided ones appear first, so that 
    // they have precedence over defaults when duplicates are eliminated.
    val resolvedLibraries = userProvidedLibraries + defaultLibraries

    warnOnLibraryDuplicates(resolvedLibraries.map { it.libraryFile })

    return resolvedLibraries.distinctBy { it.libraryFile.absolutePath }
}

private fun SearchPathResolver.warnOnLibraryDuplicates(resolvedLibraries: List<File>) {

    val duplicates = resolvedLibraries.groupBy { it.absolutePath } .values.filter { it.size > 1 }

    duplicates.forEach {
        logger("library included more than once: ${it.first().absolutePath}")
    }
}


fun SearchPathResolver.resolveLibrariesRecursive(immediateLibraries: List<LibraryReaderImpl>) {
    val cache = mutableMapOf<File, LibraryReaderImpl>()
    cache.putAll(immediateLibraries.map { it.libraryFile.absoluteFile to it })
    var newDependencies = cache.values.toList()
    do {
        newDependencies = newDependencies.map { library: LibraryReaderImpl ->
            library.unresolvedDependencies
                    .map { it to resolve(it) }
                    .map { (unresolved, resolved) ->
                        val absoluteFile = resolved.libraryFile.absoluteFile
                        if (absoluteFile in cache) {
                            library.resolvedDependencies.add(cache[absoluteFile]!!)
                            null
                        } else {
                            cache.put(absoluteFile ,resolved)
                            library.resolvedDependencies.add(resolved)
                            resolved
                        }
            }.filterNotNull()
        } .flatten()
    } while (newDependencies.isNotEmpty())
}

fun List<LibraryReaderImpl>.withResolvedDependencies(): List<LibraryReaderImpl> {
    val result = mutableSetOf<LibraryReaderImpl>()
    result.addAll(this)
    var newDependencies = result.toList()
    do {
        newDependencies = newDependencies
            .map { it -> it.resolvedDependencies } .flatten()
            .filter { it !in result }
        result.addAll(newDependencies)
    } while (newDependencies.isNotEmpty())
    return result.toList()
}

fun KonanLibrarySearchPathResolver.resolveLibrariesRecursive(libraryNames: List<String>,
                                                 noStdLib: Boolean = false,
                                                 noDefaultLibs: Boolean = false): List<LibraryReaderImpl> {
    val immediateLibraries = resolveImmediateLibraries(
                    libraryNames = libraryNames,
                    noStdLib = noStdLib,
                    noDefaultLibs = noDefaultLibs
            )
    resolveLibrariesRecursive(immediateLibraries)
    return immediateLibraries.withResolvedDependencies()
}

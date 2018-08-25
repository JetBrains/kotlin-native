package org.jetbrains.kotlin.konan.library.resolver

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.name.FqName

interface PackageAccessedHandler {

    fun markPackageAccessed(fqName: FqName)
}

/**
 * A [KonanLibrary] wrapper that is used for resolving library's dependencies.
 */
interface KonanResolvedLibrary: PackageAccessedHandler {

    // The library itself.
    val library: KonanLibrary

    // Dependencies on other libraries.
    val resolvedDependencies: List<KonanResolvedLibrary>

    // Whether it is needed to linker.
    val isNeededForLink: Boolean

    // Is provided by the distribution?
    val isDefault: Boolean
}

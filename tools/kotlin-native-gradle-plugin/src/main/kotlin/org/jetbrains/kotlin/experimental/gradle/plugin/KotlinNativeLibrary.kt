package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.nativeplatform.Linkage


interface KotlinNativeLibrary: ProductionKotlinNativeComponent {
    override fun getDevelopmentBinary(): Provider<out KotlinNativeKlib>  // TODO: Mmmm? Should we use dynamic here?

    /** Returns the list of linkage of this library. */
    val linkage: SetProperty<Linkage>
}
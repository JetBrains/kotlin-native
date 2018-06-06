package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.language.BinaryCollection
import org.gradle.language.ComponentWithBinaries
import org.gradle.language.ComponentWithDependencies
import org.gradle.language.ProductionComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.OutputKind
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 *  Class representing a Kotlin/Native component: application or library (both klib and dynamic)
 *  built for different targets.
 */
interface KotlinNativeComponent: ComponentWithBinaries, ComponentWithDependencies, ProductionComponent {

    /**
     * Defines the source files or directories of this component. You can add files or directories to this collection.
     * When a directory is added, all source files are included for compilation. When this collection is empty,
     * the directory src/main/kotlin is used by default.
     */
    val sources: KotlinNativeSourceSet

    /** Specifies Kotlin/Native targets used to build this component. */
    val konanTargets: SetProperty<KonanTarget>

    /** Specifies compiler outputs produced by this component */
    val outputKinds: SetProperty<OutputKind>

    /** Returns the binaries for this library. */
    override fun getBinaries(): BinaryCollection<out KotlinNativeBinary>

    /**
     * Returns the implementation dependencies of this component.
     */
    fun getImplementationDependencies(): Configuration

    /**
     * Returns the binary of the component to use as the default for development.
     */
    override fun getDevelopmentBinary(): Provider<out KotlinNativeBinary>
}
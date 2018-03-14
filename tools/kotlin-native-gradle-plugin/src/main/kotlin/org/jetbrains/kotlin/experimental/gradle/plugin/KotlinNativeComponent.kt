package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.language.BinaryCollection
import org.gradle.language.ComponentWithBinaries
import org.gradle.language.ComponentWithDependencies
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 *  Class representing a Kotlin/Native component: application or library (both klib and dynamic)
 *  built for different targets.
 */
interface KotlinNativeComponent: ComponentWithBinaries, ComponentWithDependencies {

    /**
     * Specifies the base name for this component. This name is used to calculate various output file names.
     * The default value is calculated from the project name.
     */
    // TODO: Do we need some kind of wrapper for Property (to get rid of baseName.get ... )
    val baseName: Property<String>

    /**
     * Defines the source files or directories of this component. You can add files or directories to this collection.
     * When a directory is added, all source files are included for compilation. When this collection is empty,
     * the directory src/main/kotlin is used by default.
     */
    val sources: KotlinNativeSourceSet

    /** Configures the source files or directories for this component. */
    fun source(action: Action<in ConfigurableFileCollection>)

    /** Specifies the kotlin native target used to build this component. */
    val konanTargets: SetProperty<KonanTarget>

    /** Returns the binaries for this library. */
    override fun getBinaries(): BinaryCollection<out KotlinNativeBinary>

    /**
     * Returns the implementation dependencies of this component.
     */
    fun getImplementationDependencies(): Configuration
}
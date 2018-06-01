package org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeComponent
import org.jetbrains.kotlin.konan.target.KonanTarget

interface KotlinNativeSourceSet: Named {

    val common: SourceDirectorySet
    val component: KotlinNativeComponent

    fun getCommonSources(): SourceDirectorySet = common
    fun getPlatformSources(target: KonanTarget): SourceDirectorySet
    fun getAllSources(target: KonanTarget): FileCollection

    fun common(configureClosure: Closure<*>): KotlinNativeSourceSet
    fun common(configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSet
    fun common(configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSet

    // TODO: Provide a more convenient way to set targets
    fun target(target: String): SourceDirectorySet
    fun target(vararg targets: String, configureClosure: Closure<*>): KotlinNativeSourceSet
    fun target(vararg targets: String, configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSet
    fun target(vararg targets: String, configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSet
}

package org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.util.ConfigureUtil.configure
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeComponent
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class DefaultKotlinNativeSourceSet @Inject constructor(
        private val name: String,
        val sourceDirectorySetFactory: SourceDirectorySetFactory
): KotlinNativeSourceSet {

    // TODO: Try to get rid of lateinit
    override lateinit var component: KotlinNativeComponent

    override val common: SourceDirectorySet = newSourceDirectorySet("common")

    private val platformSources: MutableMap<KonanTarget, SourceDirectorySet> = mutableMapOf()

    private fun newSourceDirectorySet(name: String) = sourceDirectorySetFactory.create(name).apply {
        filter.include("**/*.kt")
    }

    override fun getPlatformSources(target: KonanTarget): SourceDirectorySet = platformSources.getOrPut(target) {
        newSourceDirectorySet(target.name)
    }

    override fun getAllSources(target: KonanTarget): FileCollection = common + getPlatformSources(target)

    override fun getName(): String = name

    override fun common(configureClosure: Closure<*>): DefaultKotlinNativeSourceSet = apply {
        configure(configureClosure, common)
    }

    override fun common(configureAction: Action<in SourceDirectorySet>): DefaultKotlinNativeSourceSet = apply {
        configureAction.execute(common)
    }

    override fun common(configureLambda: SourceDirectorySet.() -> Unit): DefaultKotlinNativeSourceSet = apply {
        common.configureLambda()
    }
}
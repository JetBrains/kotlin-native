package org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil.configure
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeComponentImpl
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import javax.inject.Inject

open class KotlinNativeSourceSetImpl @Inject constructor(
        private val name: String,
        val sourceDirectorySetFactory: SourceDirectorySetFactory,
        val project: ProjectInternal
): KotlinNativeSourceSet {

    val objectFactory: ObjectFactory = project.objects

    override val component: KotlinNativeComponentImpl =
            objectFactory.newInstance(KotlinNativeComponentImpl::class.java, name, this).also {
                project.components.add(it)
            }

    override val common = newSourceDirectorySet("common")

    private val platformSources = mutableMapOf<KonanTarget, SourceDirectorySet>()

    private fun newSourceDirectorySet(name: String) = sourceDirectorySetFactory.create(name).apply {
        filter.include("**/*.kt")
    }

    override fun getPlatformSources(target: KonanTarget) = platformSources.getOrPut(target) {
        newSourceDirectorySet(target.name)
    }

    override fun getAllSources(target: KonanTarget): FileCollection = common + getPlatformSources(target)

    override fun getName(): String = name

    // region DSL

    override fun common(configureClosure: Closure<*>): KotlinNativeSourceSetImpl = apply {
        configure(configureClosure, common)
    }
    override fun common(configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSetImpl = apply {
        configureAction.execute(common)
    }
    override fun common(configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSetImpl = apply {
        common.configureLambda()
    }

    override fun target(target: String): SourceDirectorySet {
        val konanTarget = HostManager().targetByName(target)
        component.konanTargets.add(konanTarget)
        return getPlatformSources(konanTarget)
    }

    override fun target(vararg targets: String, configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSet {
        val hostManager = HostManager()
        targets.map { hostManager.targetByName(it) }.forEach {
            getPlatformSources(it).configureLambda()
        }
        return this
    }

    override fun target(vararg targets: String, configureClosure: Closure<*>): KotlinNativeSourceSet =
            target(*targets) { configure(configureClosure, this) }

    override fun target(vararg targets: String, configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSet =
            target(*targets) { configureAction.execute(this) }

    // endregion
}
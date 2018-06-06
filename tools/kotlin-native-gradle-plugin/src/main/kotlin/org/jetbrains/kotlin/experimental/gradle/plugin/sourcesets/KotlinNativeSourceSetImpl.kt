package org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil.configure
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeComponentImpl
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
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

    // Common source directory set configuration.
    override fun common(configureClosure: Closure<*>) = apply { configure(configureClosure, common) }
    override fun common(configureAction: Action<in SourceDirectorySet>) = apply { configureAction.execute(common) }
    override fun common(configureLambda: SourceDirectorySet.() -> Unit) = apply { common.configureLambda() }

    // Configuration of the corresponding software component.
    override fun component(configureClosure: Closure<*>) = apply { configure(configureClosure, component) }
    override fun component(configureAction: Action<in KotlinNativeComponentImpl>) =
            apply { configureAction.execute(component) }
    override fun component(configureLambda: KotlinNativeComponentImpl.() -> Unit) =
            apply { component.configureLambda() }

    // Adding new targets and configuration of target-specific source directory sets.
    override fun target(target: String): SourceDirectorySet {
        val konanTarget = HostManager().targetByName(target)
        component.konanTargets.add(konanTarget)
        return getPlatformSources(konanTarget)
    }

    override fun target(targets: Iterable<String>) = target(targets) {}

    override fun target(targets: Iterable<String>, configureLambda: SourceDirectorySet.() -> Unit) = apply {
        targets.forEach { target(it).configureLambda() }
    }

    override fun target(targets: Iterable<String>, configureClosure: Closure<*>) =
            target(targets) { configure(configureClosure, this) }

    override fun target(targets: Iterable<String>, configureAction: Action<in SourceDirectorySet>) =
            target(targets) { configureAction.execute(this) }

    override fun target(vararg targets: String) = target(targets.toList())

    override fun target(vararg targets: String, configureLambda: SourceDirectorySet.() -> Unit) =
            target(targets.toList(), configureLambda)

    override fun target(vararg targets: String, configureClosure: Closure<*>) =
            target(targets.toList(), configureClosure)

    override fun target(vararg targets: String, configureAction: Action<in SourceDirectorySet>) =
            target(targets.toList(), configureAction)
    // endregion
}
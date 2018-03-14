package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.provider.LockableSetProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.BinaryCollection
import org.gradle.language.ComponentDependencies
import org.gradle.language.cpp.internal.MainExecutableVariant
import org.gradle.language.internal.DefaultBinaryCollection
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.language.nativeplatform.internal.ComponentWithNames
import org.gradle.language.nativeplatform.internal.DefaultNativeComponent
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.language.nativeplatform.internal.PublicationAwareComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeBinary
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.plugins.kotlinNativeSourceSets
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.DefaultKotlinNativeSourceSet
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

abstract class DefaultKotlinNativeComponent @Inject constructor(
        private val name: String,
        objects: ObjectFactory,
        fileOperations: FileOperations
) : DefaultNativeComponent(fileOperations), KotlinNativeComponent, ComponentWithNames {

    override val baseName: Property<String> = objects.property(String::class.java)
    fun getBaseName(): Provider<String> = baseName

    // TODO: Filter only .kt files somehow. May be use createSourceView for this.
    override val sources: DefaultKotlinNativeSourceSet =
            objects.newInstance(DefaultKotlinNativeSourceSet::class.java, name).apply {
                component = this@DefaultKotlinNativeComponent
            }

    override val konanTargets: LockableSetProperty<KonanTarget> =
            LockableSetProperty(objects.setProperty(KonanTarget::class.java)).apply {
                set(mutableSetOf(HostManager.host))
                // TODO: Replace HostManger with PlatformManager
            }

    @Suppress("UNCHECKED_CAST")
    private val binaries = objects.newInstance(DefaultBinaryCollection::class.java, KotlinNativeBinary::class.java)
           as DefaultBinaryCollection<KotlinNativeBinary>
    override fun getBinaries(): DefaultBinaryCollection<KotlinNativeBinary> = binaries

    override fun getName(): String = name

    private val names = Names.of(name)
    override fun getNames(): Names = names

    private val dependencies: DefaultComponentDependencies = objects.newInstance(
            DefaultComponentDependencies::class.java,
            names.withSuffix("implementation"))

    override fun getDependencies(): ComponentDependencies = dependencies

    override fun getImplementationDependencies(): Configuration = dependencies.implementationDependencies
}

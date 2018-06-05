package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.component.PublishableComponent
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.language.ComponentDependencies
import org.gradle.language.ComponentWithDependencies
import org.gradle.language.ComponentWithOutputs
import org.gradle.language.cpp.CppBinary
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.language.nativeplatform.internal.ComponentWithNames
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.toolchain.NativeToolChain
import org.jetbrains.kotlin.experimental.gradle.plugin.ComponentWithBaseName
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeBinary
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.experimental.gradle.plugin.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

/*
 *  We use the same configuration hierarchy as Gradle native:
 *
 *  componentImplementation (dependencies for the whole component ( = sourceSet): something like 'foo:bar:1.0')
 *    ^
 *    |
 *  binaryImplementation (dependnecies of a particular target/flavor) (= getDependencies.implementationDependencies)
 *    ^             ^                ^
 *    |             |                |
 *  linkLibraries  runtimeLibraries  klibs (dependencies by type: klib, static lib, shared lib etc)
 *
 */
abstract class KotlinNativeBinaryImpl(
        private val name: String,
        private val baseName: Provider<String>,
        val sourceSet: KotlinNativeSourceSet,
        val identity: KotlinNativeVariantIdentity,
        val projectLayout: ProjectLayout,
        override val kind: CompilerOutputKind,
        objects: ObjectFactory,
        componentImplementation: Configuration,
        configurations: ConfigurationContainer,
        fileOperations: FileOperations
) : KotlinNativeBinary,
    ComponentWithNames,
    ComponentWithDependencies,
    ComponentWithBaseName,
    PublishableComponent,
    ComponentWithOutputs
{

    private val names = Names.of(name)
    override fun getNames(): Names = names

    override fun getName(): String = name

    override val konanTarget: KonanTarget
        get() = identity.konanTarget

    override fun getTargetPlatform(): KotlinNativePlatform = identity.targetPlatform

    open val debuggable: Boolean  get() = identity.isDebuggable
    open val optimized: Boolean   get() = identity.isOptimized

    override val sources: FileCollection
        get() = sourceSet.getAllSources(konanTarget)

    private val dependencies = objects.newInstance<DefaultComponentDependencies>(
            DefaultComponentDependencies::class.java,
            name + "Implementation"
    ).apply {
        implementationDependencies.extendsFrom(componentImplementation)
    }

    override fun getDependencies(): ComponentDependencies = dependencies
    fun getImplementationDependencies(): Configuration = dependencies.implementationDependencies

    // A configuration containing klibraries
    override val klibraries = configurations.create(names.withPrefix("klibraries")).apply {
        isCanBeConsumed = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinNativeUsage.KLIB))
        attributes.attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, debuggable)
        attributes.attribute(CppBinary.OPTIMIZED_ATTRIBUTE, optimized)
        attributes.attribute(KotlinNativeBinary.KONAN_TARGET_ATTRIBUTE, konanTarget.name)
        // Write
        attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, konanTarget.getGradleOSFamily(objects))
        extendsFrom(getImplementationDependencies())
    }

    override fun getBaseName(): Provider<String> = baseName

    override val compileTask: Property<KotlinNativeCompile> = objects.property(KotlinNativeCompile::class.java)

    override fun getCoordinates(): ModuleVersionIdentifier = identity.coordinates

    open fun isDebuggable(): Boolean = debuggable
    open fun isOptimized(): Boolean = optimized

    // TODO: Support native libraries
    fun getLinkLibraries(): FileCollection = projectLayout.filesFor()
    fun getRuntimeLibraries(): FileCollection = projectLayout.filesFor()

    fun getToolChain(): NativeToolChain =
            throw NotImplementedError("Kotlin/Native doesn't support the Gradle's toolchain model.")

    /** A name of a root folder for this binary's output under the build directory. */
    internal abstract val outputRootName: String

    private val outputs: ConfigurableFileCollection = fileOperations.files()
    override fun getOutputs() = outputs
}
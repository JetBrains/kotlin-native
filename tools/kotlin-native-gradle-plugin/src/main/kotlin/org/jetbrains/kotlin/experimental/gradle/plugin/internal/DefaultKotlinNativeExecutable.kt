package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.tasks.LinkExecutable
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeExecutable
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject

// TODO: SoftwareComponentInternal will be replaced by ComponentWithVariants
open class DefaultKotlinNativeExecutable @Inject constructor(
        name: String,
        objects: ObjectFactory,
        componentImplementation: Configuration,
        configurations: ConfigurationContainer,
        baseName: Provider<String>,
        sources: KotlinNativeSourceSet,
        identity: KotlinNativeVariantIdentity,
        projectLayout: ProjectLayout,
        fileOperations: FileOperations
) : DefaultKotlinNativeBinary(name,
        baseName,
        sources,
        identity,
        projectLayout,
        CompilerOutputKind.PROGRAM,
        objects,
        componentImplementation,
        configurations,
        fileOperations),
    KotlinNativeExecutable,
    SoftwareComponentInternal
{
    override fun getCoordinates(): ModuleVersionIdentifier = identity.coordinates

    // Properties

    // TODO: Set Runtime elements.

    private val runtimeElementsProperty: Property<Configuration> = objects.property(Configuration::class.java)

    private val executableFileProperty = projectLayout.fileProperty()
//    private val debuggerExecutableFileProperty = projectLayout.fileProperty()
//    private val runtimeFileProperty = projectLayout.fileProperty()
    private val installDirectoryProperty = projectLayout.directoryProperty()

    private val linkTaskProperty = objects.property(LinkExecutable::class.java)
    private val installTaskProperty = objects.property(InstallExecutable::class.java)

    // Interface Implementation
    override fun getRuntimeElements(): Property<Configuration> = runtimeElementsProperty

    override fun getExecutableFile(): Property<RegularFile> = executableFileProperty
//    override fun getDebuggerExecutableFile(): Property<RegularFile> = debuggerExecutableFileProperty
//    override fun getRuntimeFile(): Provider<RegularFile> = runtimeFileProperty
    override fun getInstallDirectory(): Property<Directory> = installDirectoryProperty

    override fun getLinkTask(): Property<LinkExecutable> = linkTaskProperty
    override fun getInstallTask(): Property<InstallExecutable> = installTaskProperty

//    override fun hasRuntimeFile(): Boolean = true
//    override fun getRuntimeAttributes(): AttributeContainer = identity.runtimeUsageContext.attributes
//    override fun getLinkage(): Linkage? = null

    override fun getUsages(): Set<UsageContext> = runtimeElementsProperty.get().let {
        setOf(DefaultUsageContext(identity.runtimeUsageContext, it.allArtifacts, it))
    }
}

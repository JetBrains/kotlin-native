package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.nativeplatform.Linkage
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeKLibrary
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject

open class KotlinNativeKLibraryImpl @Inject constructor(
        name: String,
        baseName: Provider<String>,
        componentImplementation: Configuration,
        sources: KotlinNativeSourceSet,
        identity: KotlinNativeVariantIdentity,
        objects: ObjectFactory,
        projectLayout: ProjectLayout,
        configurations: ConfigurationContainer,
        private val fileOperations: FileOperations
) : KotlinNativeBinaryImpl(name,
        baseName,
        sources,
        identity,
        projectLayout,
        CompilerOutputKind.LIBRARY,
        objects,
        componentImplementation,
        configurations),
    KotlinNativeKLibrary,
    SoftwareComponentInternal
{
    // Properties

    // The link elements configuration is created by the NativeBase plugin.
    private val linkElementsProperty: Property<Configuration> = objects.property(Configuration::class.java)
    private val linkFileProperty: RegularFileProperty = projectLayout.fileProperty()

    // Interface

    override fun getLinkElements()  = linkElementsProperty
    override fun getLinkFile() = linkFileProperty

    override fun getUsages(): Set<UsageContext> = linkElementsProperty.get().let {
        setOf(DefaultUsageContext(identity.linkUsageContext, it.allArtifacts, it))
    }

    override fun getLinkAttributes(): AttributeContainer = identity.linkUsageContext.attributes

    // TODO: Does Klib really match a static linkage in Gradle's terms?
    override fun getLinkage(): Linkage? = Linkage.STATIC

    override fun getOutputs(): FileCollection = fileOperations.files(linkFile.get())
}

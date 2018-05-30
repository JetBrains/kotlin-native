package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.apache.tools.ant.TaskContainer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.DefaultProjectPublication
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.TaskContainerInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.ComponentWithBinaries
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.cpp.ProductionCppComponent
import org.gradle.language.cpp.internal.DefaultCppComponent
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithExecutable
import org.gradle.language.plugins.NativeBasePlugin
import org.gradle.nativeplatform.plugins.NativeComponentModelPlugin
import org.gradle.nativeplatform.toolchain.internal.plugins.StandardToolChainsPlugin
import org.gradle.swiftpm.internal.SwiftPmTarget
import org.jetbrains.kotlin.container.ComponentContainer
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeBinary
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeExecutable
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeKlib
import org.jetbrains.kotlin.experimental.gradle.plugin.ProductionKotlinNativeComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.DefaultKotlinNativeBinary
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.DefaultKotlinNativeComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.DefaultKotlinNativeExecutable
import org.jetbrains.kotlin.experimental.gradle.plugin.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.tasks.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject


class KotlinNativeBasePlugin: Plugin<ProjectInternal> {

    private fun addCompilationTasks(
            tasks: TaskContainerInternal,
            components: SoftwareComponentContainer,
            buildDirectory: DirectoryProperty,
            providers: ProviderFactory
    ) {
        components.withType(DefaultKotlinNativeBinary::class.java) { binary ->
            val names = binary.names
            val target = binary.konanTarget
            val kind = binary.kind

            val compileTask = tasks.create(
                    names.getCompileTaskName(LANGUAGE_NAME),
                    KotlinNativeCompile::class.java
            ).apply {
                this.binary = binary
                this.outputFile.set(buildDirectory.file(providers.provider {
                    val prefix = kind.prefix(target)
                    val suffix = kind.suffix(target)
                    val baseName = binary.getBaseName().get()
                    "exe/${names.dirName}/${prefix}${baseName}${suffix}"
                }))
            }
            binary.compileTask.set(compileTask)
        }
    }


    override fun apply(project: ProjectInternal): Unit = with(project) {
        // TODO: Deal with compiler downloading.
        // TODO: Remove when this feature is available by default
        gradle.services.get(FeaturePreviews::class.java).enableFeature(FeaturePreviews.Feature.GRADLE_METADATA)

        // Apply base plugins
        project.pluginManager.apply(LifecycleBasePlugin::class.java)
        project.pluginManager.apply(NativeBasePlugin::class.java)

        // Create compile tasks
        addCompilationTasks(tasks, components, layout.buildDirectory, providers)
    }

    companion object {
        const val LANGUAGE_NAME = "KotlinNative"
        const val SOURCE_SETS_EXTENSION = "sourceSets"
    }

}

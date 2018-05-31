package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.gradle.api.Plugin
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.TaskContainerInternal
import org.gradle.api.provider.ProviderFactory
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.nativeplatform.ComponentWithLinkUsage
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithLinkUsage
import org.gradle.language.plugins.NativeBasePlugin
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeExecutable
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeBinaryImpl
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeExecutableImpl
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeKLibraryImpl
import org.jetbrains.kotlin.experimental.gradle.plugin.tasks.KotlinNativeCompile

// TODO: Support ProductionComponent (see Gradle NativeBase plugin)
class KotlinNativeBasePlugin: Plugin<ProjectInternal> {

    private fun addCompilationTasks(
            tasks: TaskContainerInternal,
            components: SoftwareComponentContainer,
            buildDirectory: DirectoryProperty,
            providers: ProviderFactory
    ) {
        components.withType(KotlinNativeBinaryImpl::class.java) { binary ->
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
                    // TODO: Change the paths
                }))
            }
            binary.compileTask.set(compileTask)

            when(binary) {
                is KotlinNativeExecutableImpl -> binary.runtimeFile.set(compileTask.outputFile)
                is KotlinNativeKLibraryImpl -> binary.linkFile.set(compileTask.outputFile)
            }
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

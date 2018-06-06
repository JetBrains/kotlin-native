package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.Plugin
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.TaskContainerInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ProviderFactory
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.plugins.NativeBasePlugin
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeBinaryImpl
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeExecutableImpl
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeKLibraryImpl
import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.KotlinNativeCompile

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
                outputFile.set(buildDirectory.file(providers.provider {
                    val root = binary.outputRootName
                    val prefix = kind.prefix(target)
                    val suffix = kind.suffix(target)
                    val baseName = binary.getBaseName().get()
                    "$root/${names.dirName}/${prefix}${baseName}${suffix}"
                }))

                group = BasePlugin.BUILD_GROUP
                description = "Compiles Kotlin/Native source set '${binary.sourceSet.name}' into a ${binary.kind.name.toLowerCase()}"
            }
            binary.compileTask.set(compileTask)
            binary.outputs.from(compileTask.outputFile)

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

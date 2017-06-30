package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask

//TODO: reuse KotlinPlatformImplementationPluginBase after migrating to 1.2.20
open class KotlinPlatformNativePlugin : NativePlatformImplementationPluginBase("native") {
    override fun apply(project: Project) {
        project.applyPlugin<KonanPlugin>()
        super.apply(project)
    }
}

// Patched copy of KotlinPlatformImplementationPluginBase from kotlin-gradle-plugin
// TODO: switch to origin after migrating to 1.2.20
open class NativePlatformImplementationPluginBase(platformName: String) : KotlinPlatformImplementationPluginBase(platformName) {
    private val commonProjects = arrayListOf<Project>()
    private val platformProjects = mutableSetOf<Project>()

    private var implementConfigurationIsUsed = false

    override fun apply(project: Project) {
        project.tasks.withType(KonanCompileTask::class.java).all {
            (it as KonanCompileTask).multiPlatform = true
        }

        val implementConfig = project.configurations.create(IMPLEMENT_CONFIG_NAME)
        val expectedByConfig = project.configurations.create(EXPECTED_BY_CONFIG_NAME)

        implementConfig.dependencies.whenObjectAdded {
            if (!implementConfigurationIsUsed) {
                implementConfigurationIsUsed = true
                project.logger.warn(IMPLEMENT_DEPRECATION_WARNING)
            }
        }

        listOf(implementConfig, expectedByConfig).forEach { config ->
            config.isTransitive = false

            config.dependencies.whenObjectAdded { dep ->
                if (dep is ProjectDependency) {
                    addCommonProject(dep.dependencyProject, project)
                } else {
                    throw GradleException("$project '${config.name}' dependency is not a project: $dep")
                }
            }
        }

        //HACK: add default src to srcFiles_ otherwise it would be skipped in KonanCompileTask.srcFiles
        project.whenEvaluated {
            platformProjects.forEach { platformProject ->
                platformProject.tasks.withType(KonanCompileTask::class.java).all {
                    it.srcFiles_.add(platformProject.konanDefaultSrcFiles)
                }
            }
        }
    }

    private fun addCommonProject(commonProject: Project, platformProject: Project) {
        commonProjects.add(commonProject)
        if (commonProjects.size > 1) {
            throw GradleException(
                    "Platform project $platformProject has more than one " +
                            "'$EXPECTED_BY_CONFIG_NAME'${if (implementConfigurationIsUsed) "/'$IMPLEMENT_CONFIG_NAME'" else ""} " +
                            "dependency: ${commonProjects.joinToString()}")
        }

        commonProject.whenEvaluated {
            if (!commonProject.pluginManager.hasPlugin("kotlin-platform-common")) {
                throw GradleException(
                        "Platform project $platformProject has an " +
                                "'$EXPECTED_BY_CONFIG_NAME'${if (implementConfigurationIsUsed) "/'$IMPLEMENT_CONFIG_NAME'" else ""} " +
                                "dependency to non-common project $commonProject")
            }

            commonProject.sourceSets.all { commonSourceSet ->
                //assume that common part always lies in main and native one in native
                commonSourceSet.kotlin!!.let {
                    sourceDirectorySet: SourceDirectorySet ->
                    platformProject.tasks.withType(KonanCompileTask::class.java).all {
                        it.srcFiles_.add(sourceDirectorySet)
                    }
                    platformProjects.add(platformProject)
                }
            }
        }
    }
}

//TODO switch to public one in kotlin plugin
private val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets


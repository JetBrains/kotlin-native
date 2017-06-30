package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask

//TODO: reuse KotlinPlatformImplementationPluginBase
open class KotlinPlatformNativePlugin : NativePlatformImplementationPluginBase("native") {
    override fun apply(project: Project) {
        project.applyPlugin<KonanPlugin>()
        super.apply(project)
    }
}

// Patched copy of KotlinPlatformImplementationPluginBase from kotlin-gradle-plugin
// TODO: switch to origin
open class NativePlatformImplementationPluginBase(platformName: String) : KotlinPlatformImplementationPluginBase(platformName) {
    private val commonProjects = arrayListOf<Project>()

    override fun apply(project: Project) {
        project.tasks.withType(KonanCompileTask::class.java).all {
            (it as KonanCompileTask).multiPlatform = true
        }

        //project.tasks.filterIsInstance<KonanCompileTask>().associateByTo(platformKotlinTasksBySourceSetName) { it.sourceSetName }

        val implementConfig = project.configurations.create("implement")
        implementConfig.isTransitive = false
        implementConfig.dependencies.whenObjectAdded { dep ->
            if (dep is ProjectDependency) {
                addCommonProject(dep.dependencyProject, project)
            }
            else {
                throw GradleException("$project `implement` dependency is not a project: $dep")
            }
        }
    }

    private fun addCommonProject(commonProject: Project, platformProject: Project) {
        commonProjects.add(commonProject)
        if (commonProjects.size > 1) {
            throw GradleException("Platform project $platformProject implements more than one common project: ${commonProjects.joinToString()}")
        }

        commonProject.whenEvaluated {
            if ((!commonProject.plugins.hasPlugin(KotlinPlatformCommonPlugin::class.java))) {
                throw GradleException("Platform project $platformProject implements non-common project $commonProject (`apply plugin 'kotlin-platform-kotlin'`)")
            }

            commonProject.sourceSets.all { commonSourceSet ->
                //assume that common part always lies in main and native one in native
                commonSourceSet.kotlin!!.let {
                    sourceDirectorySet: SourceDirectorySet ->
                    println("add commonSourceSet $sourceDirectorySet")
                    platformProject.tasks.withType(KonanCompileTask::class.java).all {
                        it.srcFiles_.add(sourceDirectorySet)
                    }
                }
            }
        }
    }
}

//TODO switch to public one in kotlin plugin
private val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets


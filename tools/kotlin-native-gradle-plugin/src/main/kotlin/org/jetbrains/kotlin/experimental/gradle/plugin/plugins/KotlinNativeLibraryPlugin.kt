package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal

// TODO: Move from experimental package. What should be the new package?
// TODO: What about ToolChains? Can we extend them?

class KotlinNativeLibraryPlugin: Plugin<ProjectInternal> {

    override fun apply(target: ProjectInternal?) {
        println("TTTT!!!")
    }

}
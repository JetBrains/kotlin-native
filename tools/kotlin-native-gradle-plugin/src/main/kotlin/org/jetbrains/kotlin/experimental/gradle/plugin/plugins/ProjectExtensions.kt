package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.DefaultKotlinNativeSourceSet

@Suppress("UNCHECKED_CAST")
val Project.kotlinNativeSourceSets: NamedDomainObjectContainer<DefaultKotlinNativeSourceSet>
    get() = extensions.getByName(KotlinNativeBasePlugin.SOURCE_SETS_EXTENSION)
            as NamedDomainObjectContainer<DefaultKotlinNativeSourceSet>

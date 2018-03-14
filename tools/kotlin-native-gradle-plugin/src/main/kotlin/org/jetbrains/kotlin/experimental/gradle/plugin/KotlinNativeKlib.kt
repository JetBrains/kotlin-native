package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.language.ComponentWithOutputs
import org.gradle.api.component.PublishableComponent
import org.gradle.language.nativeplatform.ComponentWithLinkFile
import org.gradle.language.nativeplatform.ComponentWithLinkUsage
import org.gradle.language.nativeplatform.ComponentWithRuntimeUsage
import org.gradle.language.nativeplatform.ComponentWithStaticLibrary

/**
 *  A component representing a klibrary.
 */
interface KotlinNativeKlib: KotlinNativeBinary,
        ComponentWithLinkFile,
        ComponentWithLinkUsage,
        ComponentWithOutputs,
        PublishableComponent


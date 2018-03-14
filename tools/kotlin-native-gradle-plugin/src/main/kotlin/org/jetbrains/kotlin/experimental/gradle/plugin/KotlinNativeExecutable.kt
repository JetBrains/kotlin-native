package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.language.ComponentWithOutputs
import org.gradle.api.component.PublishableComponent
import org.gradle.api.provider.Property
import org.gradle.language.nativeplatform.ComponentWithExecutable
import org.gradle.language.nativeplatform.ComponentWithInstallation
import org.gradle.language.nativeplatform.ComponentWithRuntimeUsage
import org.gradle.nativeplatform.Linkage
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider

/**
 * Represents Kotlin/Native executable.
 */
interface KotlinNativeExecutable: KotlinNativeBinary,
        ComponentWithExecutable,
        ComponentWithInstallation,
        ComponentWithOutputs,
        ComponentWithRuntimeUsage,
        PublishableComponent
{
//    /**  Returns the executable file to use with a debugger for this executable. */
//    fun getDebuggerExecutableFile(): Property<RegularFile>
//
//    /** TODO: Copied from Component with ConfigurableComponentWithRuntimeUsage. Javadoc  */
//    fun getImplementationDependencies(): Configuration
//
//    fun getLinkage(): Linkage?
//
//    fun hasRuntimeFile(): Boolean
//
//    fun getRuntimeFile(): Provider<RegularFile>
//
//    fun getRuntimeAttributes(): AttributeContainer
}
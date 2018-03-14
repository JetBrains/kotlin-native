package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.provider.LockableSetProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.language.ComponentDependencies
import org.gradle.language.cpp.internal.MainExecutableVariant
import org.gradle.language.cpp.internal.MainLibraryVariant
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.language.internal.DefaultLibraryDependencies
import org.gradle.language.nativeplatform.internal.PublicationAwareComponent
import org.gradle.nativeplatform.Linkage
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeKlib
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeLibrary
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.DefaultKotlinNativeSourceSet

// TODO: CPP plugin uses PlubicationAwareComponent interface here while the Swift one doesn't.
// What should we do?

open class DefaultKotlinNativeLibrary(
        name: String,
        objects: ObjectFactory,
        fileOperations: FileOperations
) : DefaultKotlinNativeComponent(name, objects, fileOperations), KotlinNativeLibrary {

    override fun getDisplayName(): DisplayName = Describables.withTypeAndName("Kotlin/Native library", name)

    private val developmentBinary = objects.property(KotlinNativeKlib::class.java)
    override fun getDevelopmentBinary(): Provider<out KotlinNativeKlib> = developmentBinary

    override val linkage: SetProperty<Linkage> =
            LockableSetProperty<Linkage>(objects.setProperty(Linkage::class.java)).apply {
                add(Linkage.STATIC)
            }
}

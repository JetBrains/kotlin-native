package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.provider.LockableSetProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.nativeplatform.Linkage
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeKlib
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeLibrary
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.DefaultKotlinNativeSourceSet

open class DefaultKotlinNativeLibrary(
        name: String,
        val objectFactory: ObjectFactory,
        fileOperations: FileOperations
) : DefaultKotlinNativeComponent(name, objectFactory, fileOperations), KotlinNativeLibrary {

    override fun getDisplayName(): DisplayName = Describables.withTypeAndName("Kotlin/Native library", name)

    private val developmentBinary = objectFactory.property(KotlinNativeKlib::class.java)
    override fun getDevelopmentBinary(): Provider<out KotlinNativeKlib> = developmentBinary

    override val linkage: SetProperty<Linkage> =
            LockableSetProperty<Linkage>(objectFactory.setProperty(Linkage::class.java)).apply {
                add(Linkage.STATIC)
            }
}

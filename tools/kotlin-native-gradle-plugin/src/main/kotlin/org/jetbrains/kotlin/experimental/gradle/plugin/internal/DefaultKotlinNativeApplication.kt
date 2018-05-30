package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.internal.Describables
import org.gradle.language.cpp.internal.MainExecutableVariant
import org.gradle.language.cpp.internal.NativeVariantIdentity
import org.gradle.language.nativeplatform.internal.PublicationAwareComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeApplication
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeExecutable
import javax.inject.Inject

open class DefaultKotlinNativeApplication @Inject constructor(
        name: String,
        val objectFactory: ObjectFactory,
        fileOperations: FileOperations
) : DefaultKotlinNativeComponent(name, objectFactory, fileOperations), KotlinNativeApplication, PublicationAwareComponent {

    override fun getDisplayName() = Describables.withTypeAndName("Kotlin/Native application", name)

    private val developmentBinaryProperty = objectFactory.property(KotlinNativeExecutable::class.java)
    override fun getDevelopmentBinary(): Property<KotlinNativeExecutable> = developmentBinaryProperty

    fun addExecutable(identity: NativeVariantIdentity): DefaultKotlinNativeExecutable =
            objectFactory.newInstance(
                DefaultKotlinNativeExecutable::class.java,
                "$name${identity.name.capitalize()}",
                getImplementationDependencies(),
                getBaseName(),
                sources,
                identity
            ).apply {
                binaries.add(this)
            }
}


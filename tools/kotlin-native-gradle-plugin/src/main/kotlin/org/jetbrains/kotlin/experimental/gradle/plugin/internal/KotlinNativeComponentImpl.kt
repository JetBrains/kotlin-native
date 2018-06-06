package org.jetbrains.kotlin.experimental.gradle.plugin.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.provider.LockableSetProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.language.ComponentDependencies
import org.gradle.language.cpp.internal.NativeVariantIdentity
import org.gradle.language.internal.DefaultBinaryCollection
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.language.nativeplatform.internal.ComponentWithNames
import org.gradle.language.nativeplatform.internal.DefaultNativeComponent
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.language.nativeplatform.internal.PublicationAwareComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeBinary
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSetImpl
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class KotlinNativeComponentImpl @Inject constructor(
        private val name: String,
        override val sources: KotlinNativeSourceSetImpl,
        private val objectFactory: ObjectFactory,
        fileOperations: FileOperations
) : DefaultNativeComponent(fileOperations), KotlinNativeComponent, ComponentWithNames, PublicationAwareComponent {

    override fun getDisplayName(): DisplayName = Describables.withTypeAndName("Kotlin/Native component", name)

    private val baseName: Property<String> = objectFactory.property(String::class.java).apply { set(name) }
    override fun getBaseName(): Property<String> = baseName

    override val konanTargets: LockableSetProperty<KonanTarget> =
            LockableSetProperty(objectFactory.setProperty(KonanTarget::class.java)).apply {
                set(mutableSetOf(HostManager.host))
            }

    override val outputKinds: LockableSetProperty<OutputKind> =
            LockableSetProperty(objectFactory.setProperty(OutputKind::class.java)).apply {
                set(mutableSetOf(OutputKind.EXECUTABLE, OutputKind.KLIBRARY))
            }

    @Suppress("UNCHECKED_CAST")
    private val binaries = objectFactory.newInstance(DefaultBinaryCollection::class.java, KotlinNativeBinary::class.java)
           as DefaultBinaryCollection<KotlinNativeBinary>
    override fun getBinaries(): DefaultBinaryCollection<KotlinNativeBinary> = binaries

    override fun getName(): String = name

    private val names = Names.of(name)
    override fun getNames(): Names = names

    private val dependencies: DefaultComponentDependencies = objectFactory.newInstance(
            DefaultComponentDependencies::class.java,
            names.withSuffix("implementation"))

    override fun getDependencies(): ComponentDependencies = dependencies

    override fun getImplementationDependencies(): Configuration = dependencies.implementationDependencies

    private val mainPublication = KotlinNativeVariant()

    override fun getMainPublication(): KotlinNativeVariant = mainPublication

    private val developmentBinaryProperty: Property<KotlinNativeBinary> =
            objectFactory.property(KotlinNativeBinary::class.java)

    override fun getDevelopmentBinary() = developmentBinaryProperty

    // region Adding binaries

    private fun <T: KotlinNativeBinary> addBinary(type: Class<T>, identity: NativeVariantIdentity): T =
            objectFactory.newInstance(
                    type,
                    "$name${identity.name.capitalize()}",
                    getBaseName(),
                    getImplementationDependencies(),
                    sources,
                    identity
            ).apply {
                binaries.add(this)
                mainPublication.variants.add(this)
            }

    private inline fun <reified T : KotlinNativeBinary> addBinary(identity: NativeVariantIdentity): T =
            addBinary(T::class.java, identity)


    fun addExecutable(identity: NativeVariantIdentity) = addBinary<KotlinNativeExecutableImpl>(identity)
    fun addKLibrary(identity: NativeVariantIdentity) = addBinary<KotlinNativeKLibraryImpl>(identity)

    fun addBinary(kind: OutputKind, identity: NativeVariantIdentity) = addBinary(kind.binaryClass, identity)

    // endregion.

    // region Kotlin/Native variant
    inner class KotlinNativeVariant: ComponentWithVariants, SoftwareComponentInternal {

        private val variants = mutableSetOf<KotlinNativeBinary>()
        override fun getVariants() = variants

        override fun getName(): String = this@KotlinNativeComponentImpl.name

        override fun getUsages(): Set<UsageContext> = emptySet()

    }
    // endregion

    companion object {
        @JvmStatic
        val EXECUTABLE = OutputKind.EXECUTABLE

        @JvmStatic
        val KLIBRARY = OutputKind.KLIBRARY
    }



}

package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.gradle.api.Plugin
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.cpp.CppBinary.DEBUGGABLE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.language.internal.NativeComponentFactory
import org.gradle.language.nativeplatform.internal.BuildType
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeBinary.Companion.KONAN_TARGET_ATTRIBUTE
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeComponent
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeComponentImpl
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeVariantIdentity
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSetFactory
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSetImpl
import javax.inject.Inject

// TODO: Move from experimental package. What should be the new package?
class KotlinNativePlugin @Inject constructor(val attributesFactory: ImmutableAttributesFactory)
    : Plugin<ProjectInternal> {

    private val Collection<*>.isDimensionVisible: Boolean
        get() = size > 1

    private fun createDimensionSuffix(dimensionName: String, multivalueProperty: Collection<*>): String =
            if (multivalueProperty.isDimensionVisible) {
                dimensionName.toLowerCase().capitalize()
            } else {
                ""
            }

    override fun apply(project: ProjectInternal): Unit = with(project) {
        pluginManager.apply(KotlinNativeBasePlugin::class.java)

        val instantiator = services.get(Instantiator::class.java)
        val objectFactory = objects

        // TODO: Use the base plugin
        @Suppress("UNCHECKED_CAST")
        val sourceSets = project.extensions.create(
                KotlinNativeBasePlugin.SOURCE_SETS_EXTENSION,
                FactoryNamedDomainObjectContainer::class.java,
                KotlinNativeSourceSetImpl::class.java,
                instantiator,
                KotlinNativeSourceSetFactory(this)
        ) as FactoryNamedDomainObjectContainer<KotlinNativeSourceSetImpl>

        sourceSets.create("main") {
            // Override the default component base name.
            it.component.baseName.set(project.name)
        }

        // Create binaries for host
        afterEvaluate {
            for (component in components.withType(KotlinNativeComponentImpl::class.java)) {
                component.konanTargets.lockNow()
                component.outputKinds.lockNow()
                val targets = component.konanTargets.get()
                val outputKinds = component.outputKinds.get()

                require(targets.isNotEmpty()) { "A Kotlin/Native target needs to be specified for the component." }
                require(outputKinds.isNotEmpty()) { "An output kind needs to be specified for the component." }

                val group = project.provider { project.group.toString() }
                val version = project.provider { project.version.toString() }

                for (kind in outputKinds) {
                    for (buildType in BuildType.DEFAULT_BUILD_TYPES) {
                        for (target in targets) {

                            val buildTypeSuffix = buildType.name
                            val outputKindSuffix = createDimensionSuffix(kind.name, outputKinds)
                            val targetSuffix = createDimensionSuffix(target.name, targets)
                            val variantName = "${buildTypeSuffix}${outputKindSuffix}${targetSuffix}"

                            // TODO: Move into a separate function
                            val linkUsageContext: DefaultUsageContext? = kind.linkUsageName?.let {
                                val usage = objectFactory.named(Usage::class.java, it)
                                val attributes = attributesFactory.mutable().apply {
                                    attribute(Usage.USAGE_ATTRIBUTE, usage)
                                    attribute(DEBUGGABLE_ATTRIBUTE, buildType.isDebuggable)
                                    attribute(OPTIMIZED_ATTRIBUTE, buildType.isOptimized)
                                    attribute(KONAN_TARGET_ATTRIBUTE, target.name)
                                }
                                DefaultUsageContext(variantName + "Link", usage, attributes)
                            }

                            val runtimeUsageContext = kind.runtimeUsageName?.let {
                                val usage = objectFactory.named(Usage::class.java, it)
                                val attributes = attributesFactory.mutable().apply {
                                    attribute(Usage.USAGE_ATTRIBUTE, usage)
                                    attribute(DEBUGGABLE_ATTRIBUTE, buildType.isDebuggable)
                                    attribute(OPTIMIZED_ATTRIBUTE, buildType.isOptimized)
                                    attribute(KONAN_TARGET_ATTRIBUTE, target.name)
                                }
                                DefaultUsageContext(variantName + "Runtime", usage, attributes)
                            }

                            // TODO: Do we need something like klibUsageContext?

                            val variantIdentity = KotlinNativeVariantIdentity(
                                    variantName,
                                    component.baseName,
                                    group, version, target,
                                    buildType.isDebuggable,
                                    buildType.isOptimized,
                                    linkUsageContext,
                                    runtimeUsageContext,
                                    objects
                            )

                            component.addBinary(kind, variantIdentity)
                        }
                    }
                }
                component.binaries.realizeNow()

            }
        }
        // TODO: Support test source set
    }

}
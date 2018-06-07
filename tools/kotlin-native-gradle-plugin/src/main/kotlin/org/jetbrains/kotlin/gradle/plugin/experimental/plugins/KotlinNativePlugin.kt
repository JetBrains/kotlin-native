package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.Plugin
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.cpp.CppBinary.DEBUGGABLE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary.Companion.KONAN_TARGET_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeBuildType
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeComponentImpl
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeVariantIdentity
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.OutputKind.Companion.getDevelopmentKind
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

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

    private fun createUsageContext(
            usageName: String,
            variantName: String,
            usageContextSuffix: String,
            buildType: KotlinNativeBuildType,
            target: KonanTarget,
            objectFactory: ObjectFactory
    ): DefaultUsageContext {
        val usage = objectFactory.named(Usage::class.java, usageName)
        val attributes = attributesFactory.mutable().apply {
            attribute(Usage.USAGE_ATTRIBUTE, usage)
            attribute(DEBUGGABLE_ATTRIBUTE, buildType.debuggable)
            attribute(OPTIMIZED_ATTRIBUTE, buildType.optimized)
            attribute(KONAN_TARGET_ATTRIBUTE, target.name)
        }
        return DefaultUsageContext(variantName + usageContextSuffix, usage, attributes)
    }

    override fun apply(project: ProjectInternal): Unit = with(project) {
        pluginManager.apply(KotlinNativeBasePlugin::class.java)

        val instantiator = services.get(Instantiator::class.java)
        val hostManager = HostManager()
        val objectFactory = objects

        // TODO: Use the kotlin base plugin
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
                val targets = component.konanTargets.get().filter { hostManager.isEnabled(it) }
                val outputKinds = component.outputKinds.get()

                require(targets.isNotEmpty()) { "A Kotlin/Native target needs to be specified for the component." }
                require(outputKinds.isNotEmpty()) { "An output kind needs to be specified for the component." }

                val group = project.provider { project.group.toString() }
                val version = project.provider { project.version.toString() }
                val developmentKind = outputKinds.getDevelopmentKind()

                for (kind in outputKinds) {
                    // TODO: Release is debuggable in Gradle's DEFAULT_BUILD_TYPES. Is it ok for us?
                    for (buildType in KotlinNativeBuildType.DEFAULT_BUILD_TYPES) {
                        for (target in targets) {

                            val buildTypeSuffix = buildType.name
                            val outputKindSuffix = createDimensionSuffix(kind.name, outputKinds)
                            val targetSuffix = createDimensionSuffix(target.name, targets)
                            val variantName = "${buildTypeSuffix}${outputKindSuffix}${targetSuffix}"

                            val linkUsageContext: DefaultUsageContext? = kind.linkUsageName?.let {
                                createUsageContext(it, variantName, "Link", buildType, target, objectFactory)
                            }

                            val runtimeUsageContext = kind.runtimeUsageName?.let {
                                createUsageContext(it, variantName, "Runtime", buildType, target, objectFactory)
                            }

                            // TODO: Do we need something like klibUsageContext?
                            val variantIdentity = KotlinNativeVariantIdentity(
                                    variantName,
                                    component.baseName,
                                    group, version, target,
                                    buildType.debuggable,
                                    buildType.optimized,
                                    linkUsageContext,
                                    runtimeUsageContext,
                                    objects
                            )

                            val binary = component.addBinary(kind, variantIdentity)
                            if (kind == developmentKind &&
                                buildType == KotlinNativeBuildType.DEBUG &&
                                target == HostManager.host) {
                                component.developmentBinary.set(binary)
                            }
                        }
                    }
                }
                component.binaries.realizeNow()

            }
        }
        // TODO: Support test source set
    }
}
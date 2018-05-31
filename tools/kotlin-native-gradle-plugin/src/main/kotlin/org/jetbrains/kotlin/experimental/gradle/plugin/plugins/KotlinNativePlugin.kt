package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.file.SourceDirectorySetFactory
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
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeUsage
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeVariantIdentity
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.DefaultKotlinNativeSourceSet
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject

// TODO: Move from experimental package. What should be the new package?
class KotlinNativePlugin @Inject constructor(
        val componentFactory: NativeComponentFactory,
        val attributesFactory: ImmutableAttributesFactory
): Plugin<ProjectInternal> {

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

        // Create sourceSets extension
        val sourceSetFactory = object: NamedDomainObjectFactory<KotlinNativeSourceSet> {
            override fun create(name: String): KotlinNativeSourceSet =
                    DefaultKotlinNativeSourceSet(name, project.services.get(SourceDirectorySetFactory::class.java))
        }

        // TODO: Consider creating new components. May be implicitly when a source set is created?
        //  TODO: May be rename the extension to avoid conflicts with java extension. Or (better) move it into a special 'kotlin' extension
        @Suppress("UNCHECKED_CAST")
        val sourceSets = project.extensions.create(
                KotlinNativeBasePlugin.SOURCE_SETS_EXTENSION,
                FactoryNamedDomainObjectContainer::class.java,
                DefaultKotlinNativeSourceSet::class.java,
                instantiator,
                sourceSetFactory
        ) as FactoryNamedDomainObjectContainer<DefaultKotlinNativeSourceSet>

        // Create main component and a sourceSet
        val component = componentFactory.newInstance(
                KotlinNativeComponent::class.java,
                KotlinNativeComponentImpl::class.java,
                "main"
        )
        component.sources.common.srcDir("src/main/kotlin")

        sourceSets.add(component.sources)
        components.add(component)

        component.baseName.set(project.name)

        // Create binaries for host
        afterEvaluate {
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

                        val buildTypeSuffix= buildType.name
                        val outputKindSuffix= createDimensionSuffix(kind.name, outputKinds)
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

                        // TODO: Do something with KLIB USAGE?
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
        // TODO: Support test source set
        // TODO: Support setting different targets
    }

}
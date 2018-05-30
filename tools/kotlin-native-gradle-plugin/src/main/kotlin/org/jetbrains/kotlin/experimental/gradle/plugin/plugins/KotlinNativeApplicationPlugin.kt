package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.internal.NativeComponentFactory
import org.gradle.language.nativeplatform.internal.BuildType
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeApplication
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.DefaultKotlinNativeApplication
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.DefaultKotlinNativeSourceSet
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import javax.inject.Inject

import org.gradle.language.cpp.CppBinary.DEBUGGABLE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.language.nativeplatform.internal.toolchains.ToolChainSelector
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeBinary.Companion.KONAN_TARGET_ATTRIBUTE
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.KotlinNativeVariantIdentity
import org.jetbrains.kotlin.konan.target.HostManager

// TODO: Move from experimental package. What should be the new package?
class KotlinNativeApplicationPlugin @Inject constructor(
        val componentFactory: NativeComponentFactory,
        val attributesFactory: ImmutableAttributesFactory
): Plugin<ProjectInternal> {

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
        val application = componentFactory.newInstance(
                KotlinNativeApplication::class.java,
                DefaultKotlinNativeApplication::class.java,
                "main"
        )
        application.sources.common.srcDir("src/main/kotlin")

        sourceSets.add(application.sources)
        extensions.add(KotlinNativeApplication::class.java, "application", application)
        components.add(application)

        application.baseName.set(project.name)

        // Create binaries for host
        afterEvaluate {
            application.konanTargets.lockNow()
            val targets = application.konanTargets.get()
            if (targets.isEmpty()) {
                throw IllegalArgumentException("An operating system needs to be specified for the application.")
            }

            val runtimeUsage = objectFactory.named(Usage::class.java, Usage.NATIVE_RUNTIME)
            for (buildType in BuildType.DEFAULT_BUILD_TYPES) {
                for (target in targets) {
                    // TODO: May be use the Gradle CPP plugin approach (see the CPP plugin)
                    val variantName = "${buildType.name}${target.visibleName.capitalize()}"
                    val group = project.provider { project.group.toString() }
                    val version = project.provider { project.version.toString() }

                    val runtimeAttributes = attributesFactory.mutable()
                    runtimeAttributes.attribute(Usage.USAGE_ATTRIBUTE, runtimeUsage)
                    runtimeAttributes.attribute(DEBUGGABLE_ATTRIBUTE, buildType.isDebuggable)
                    runtimeAttributes.attribute(OPTIMIZED_ATTRIBUTE, buildType.isOptimized)
                    runtimeAttributes.attribute(KONAN_TARGET_ATTRIBUTE, target.name)

                    val variantIdentity = KotlinNativeVariantIdentity(
                            variantName,
                            application.baseName,
                            group, version, target,
                            buildType.isDebuggable,
                            buildType.isOptimized,
                            null,
                            DefaultUsageContext(variantName + "Runtime", runtimeUsage, runtimeAttributes),
                            objects
                    )

                    //TODO: This part is made in the same manner as it's done in the Cpp plugin. Do we really need this separation?
                    if (target == HostManager.host) {
                        val executable = application.addExecutable(variantIdentity)

                        if (buildType == BuildType.DEBUG) {
                            application.developmentBinary.set(executable)
                        }

                        application.mainPublication.addVariant(executable)
                    } else {
                        application.mainPublication.addVariant(variantIdentity)
                    }

                }
            }
            application.binaries.realizeNow()

        }
        // TODO: Support test source set
        // TODO: Support setting different targets
    }

}
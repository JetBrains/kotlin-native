package org.jetbrains.kotlin.experimental.gradle.plugin.plugins

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.internal.NativeComponentFactory
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeApplication
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeLibrary
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.DefaultKotlinNativeApplication
import org.jetbrains.kotlin.experimental.gradle.plugin.internal.DefaultKotlinNativeLibrary
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.DefaultKotlinNativeSourceSet
import org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets.KotlinNativeSourceSet
import javax.inject.Inject

// TODO: Move from experimental package. What should be the new package?

class KotlinNativeLibraryPlugin @Inject constructor(
        val componentFactory: NativeComponentFactory,
        val attributesFactory: ImmutableAttributesFactory
): Plugin<ProjectInternal> {

    override fun apply(project: ProjectInternal): Unit = with(project) {
        // TODO: Move common with Executable code into the base plugin
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
        // TODO: End of the common code.

        // Create main component and a sourceSet
        // TODO: Here the only difference with the application plugin is in the component type.
        val library = componentFactory.newInstance(
                KotlinNativeLibrary::class.java,
                DefaultKotlinNativeLibrary::class.java,
                "main"
        )
        library.sources.common.srcDir("src/main/kotlin")

        sourceSets.add(library.sources)
        extensions.add(KotlinNativeLibrary::class.java, "application", library)
        components.add(library)

        library.baseName.set(project.name)
        // TODO: End of common code 2

        afterEvaluate {

        }


    }

}
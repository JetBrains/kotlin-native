/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.*
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskCollection
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import java.util.*
import javax.inject.Inject

/**
 * We use the following properties:
 *      konan.home       - directory where compiler is located (aka dist in konan project output).
 *      konan.version    - a konan compiler version for downloading.
 */

internal fun Project.hasProperty(property: KonanPlugin.ProjectProperty) = hasProperty(property.propertyName)
internal fun Project.findProperty(property: KonanPlugin.ProjectProperty) = findProperty(property.propertyName)

internal fun Project.getProperty(property: KonanPlugin.ProjectProperty) = findProperty(property)
        ?: throw IllegalArgumentException("No such property in the project: ${property.propertyName}")

internal fun Project.getProperty(property: KonanPlugin.ProjectProperty, defaultValue: Any?) =
        findProperty(property) ?: defaultValue

internal fun Project.setProperty(property: KonanPlugin.ProjectProperty, value: Any?) {
    extensions.extraProperties.set(property.propertyName, value)
}

// konanHome extension is set by downloadKonanCompiler task.
internal val Project.konanHome: String
    get() {
        assert(hasProperty(KonanPlugin.ProjectProperty.KONAN_HOME))
        return getProperty(KonanPlugin.ProjectProperty.KONAN_HOME).toString()
    }

internal val Project.konanBuildRoot               get() = "${buildDir.canonicalPath}/konan"
internal val Project.konanCompilerOutputDir       get() = "${konanBuildRoot}/bin"
internal val Project.konanInteropStubsOutputDir   get() = "${konanBuildRoot}/interopStubs"
internal val Project.konanInteropCompiledStubsDir get() = "${konanBuildRoot}/interopCompiledStubs"
internal val Project.konanInteropLibsOutputDir    get() = "${konanBuildRoot}/nativelibs"

internal val Project.konanDefaultSrcDir           get() = file("${projectDir.canonicalPath}/src/main/kotlin")
internal fun Project.konanDefaultDefFile(libName: String)
        = file("${projectDir.canonicalPath}/src/main/c_interop/$libName.def")

internal val Project.konanInteropClasspath
    get() = project.fileTree("${project.konanHome}/konan/lib/").apply { include("*.jar")  }

internal val Project.konanCompilerClasspath
    get() = project.fileTree("${project.konanHome}/konan/lib/").apply { include("*.jar")  }

@Suppress("UNCHECKED_CAST")
internal val Project.konanArtifactsContainer: NamedDomainObjectContainer<KonanCompileConfig>
    get() = extensions.getByName(KonanPlugin.ARTIFACTS_CONTAINER_NAME) as NamedDomainObjectContainer<KonanCompileConfig>

@Suppress("UNCHECKED_CAST")
internal val Project.konanInteropContainer: NamedDomainObjectContainer<KonanInteropConfig>
    get() = extensions.getByName(KonanPlugin.INTEROP_CONTAINER_NAME) as  NamedDomainObjectContainer<KonanInteropConfig>

internal val Project.konanCompilerDownloadTask  get() = tasks.getByName(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)

internal val Project.konanVersion
    get() = getProperty(KonanPlugin.ProjectProperty.KONAN_VERSION, KonanPlugin.DEFAULT_KONAN_VERSION) as String

internal fun Project.targetIsRequested(target: String?): Boolean {
    val targets = getProperty(KonanPlugin.ProjectProperty.KONAN_BUILD_TARGETS).toString().trim().split(' ')

    return (targets.contains(target) || 
            targets.contains("all") ||
            target == null)
}

internal fun Project.targetIsSupportedAndRequested(task: KonanTargetableTask) 
    = task.targetIsSupported && this.targetIsRequested(task.target)

internal val Project.supportedCompileTasks: TaskCollection<KonanCompileTask>
    get() = project.tasks.withType(KonanCompileTask::class.java).matching { 
        targetIsSupportedAndRequested(it)
    }

internal val Project.supportedInteropTasks: TaskCollection<KonanInteropTask>
    get() = project.tasks.withType(KonanInteropTask::class.java).matching { 
        targetIsSupportedAndRequested(it)
    }

internal fun Project.konanCompilerName(): String =
        "kotlin-native-${project.simpleOsName}-${this.konanVersion}"

internal fun Project.konanCompilerDownloadDir(): String =
        KonanCompilerDownloadTask.KONAN_PARENT_DIR + "/" + project.konanCompilerName()

internal class KonanCompileConfigFactory(val project: Project): NamedDomainObjectFactory<KonanCompileConfig> {
    override fun create(name: String): KonanCompileConfig = KonanCompileConfig(name, project)
}

internal class KonanInteropConfigFactory(val project: Project): NamedDomainObjectFactory<KonanInteropConfig> {
    override fun create(name: String): KonanInteropConfig = KonanInteropConfig(name, project)
}

// Useful extensions and functions ---------------------------------------

internal fun MutableList<String>.addArg(parameter: String, value: String) {
    add(parameter)
    add(value)
}

internal fun MutableList<String>.addArgIfNotNull(parameter: String, value: String?) {
    if (value != null) {
        addArg(parameter, value)
    }
}

internal fun MutableList<String>.addKey(key: String, enabled: Boolean) {
    if (enabled) {
        add(key)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: FileCollection) {
    values.files.forEach {
        addArg(parameter, it.canonicalPath)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: Collection<FileCollection>) {
    values.forEach {
        addFileArgs(parameter, it)
    }
}

internal fun MutableList<String>.addListArg(parameter: String, values: List<String>) {
    if (values.isNotEmpty()) {
        addArg(parameter, values.joinToString(separator = " "))
    }
}

internal fun dumpProperties(task: Task) {
    when (task) {
        is KonanCompileTask -> {
            println()
            println("Compilation task: ${task.name}")
            println("outputDir          : ${task.outputDir}")
            println("artifact           : ${task.artifact}")
            println("artifactPath       : ${task.artifactPath}")
            println("inputFiles         : ${task.inputFiles.joinToString(prefix = "[", separator = ", ", postfix = "]")}")
            println("produce            : ${task.produce}")
            println("libraries          : ${task.libraries}")
            println("nativeLibraries    : ${task.nativeLibraries}")
            println("linkerOpts         : ${task.linkerOpts}")
            println("enableDebug        : ${task.enableDebug}")
            println("noStdLib           : ${task.noStdLib}")
            println("noMain             : ${task.noMain}")
            println("enableOptimization : ${task.enableOptimization}")
            println("enableAssertions   : ${task.enableAssertions}")
            println("target             : ${task.target}")
            println("languageVersion    : ${task.languageVersion}")
            println("apiVersion         : ${task.apiVersion}")
            println()
        }
        is KonanInteropTask -> {
            println()
            println("Stub generation task: ${task.name}")
            println("stubsDir           : ${task.stubsDir}")
            println("libsDir            : ${task.libsDir}")
            println("defFile            : ${task.defFile}")
            println("target             : ${task.target}")
            println("pkg                : ${task.pkg}")
            println("compilerOpts       : ${task.compilerOpts}")
            println("linkerOpts         : ${task.linkerOpts}")
            println("headers            : ${task.headers.joinToString(prefix = "[", separator = ", ", postfix = "]")}")
            println("linkFiles          : ${task.linkFiles}")
            println()
        }
        else -> {
            println("Unsupported task.")
        }
    }
}

internal fun setDefaultInputs(project: Project) {
    project.konanArtifactsContainer.asSequence()
            .filter { it.compilationTask.inputFiles.isEmpty() }
            .forEach { config ->
                project.konanDefaultSrcDir.takeIf { it.exists() }?.let {
                    config.inputDir(it.canonicalPath)
                }
            }

    project.konanInteropContainer.asSequence()
            .filter { it.generateStubsTask.defFile == null }
            .forEach { config ->
                project.konanDefaultDefFile(config.name).takeIf { it.exists() }?.let {
                    config.defFile(it)
                }
            }
}

class KonanPlugin @Inject constructor(private val registry: ToolingModelBuilderRegistry)
    : Plugin<Project> {

    enum class ProjectProperty(val propertyName: String) {
        KONAN_HOME          ("konan.home"),
        KONAN_VERSION       ("konan.version"),
        KONAN_BUILD_TARGETS ("konan.build.targets"),
        DOWNLOAD_COMPILER   ("download.compiler")
    }

    companion object {
        internal const val ARTIFACTS_CONTAINER_NAME = "konanArtifacts"
        internal const val INTEROP_CONTAINER_NAME   = "konanInterop"
        internal const val KONAN_DOWNLOAD_TASK_NAME = "downloadKonanCompiler"

        internal val DEFAULT_KONAN_VERSION = Properties().apply {
            load(KonanPlugin::class.java.getResourceAsStream("/META-INF/gradle-plugins/konan.properties") ?:
                throw RuntimeException("Cannot find a properties file"))
        }.getProperty("default-konan-version") ?: throw RuntimeException("Cannot read the default compiler version")
    }

    /**
     * Looks for task with given name in the given project.
     * If such task isn't found, will create it. Returns created/found task.
     */
    private fun Project.getTask(name: String): Task = getTasksByName(name, false).single()

    private fun Project.getOrCreateTask(name: String): Task {
        val tasks = getTasksByName(name, false)
        assert(tasks.size <= 1)
        return if (tasks.isEmpty()) {
            this.tasks.create(name, DefaultTask::class.java)
        } else {
            tasks.single()
        }
    }

    // TODO: Create default config? what about test sources?
    override fun apply(project: Project?) {
        if (project == null) { return }
        registry.register(KonanToolingModelBuilder)
        project.plugins.apply("base")
        // Create necessary tasks and extensions.
        project.tasks.create(KONAN_DOWNLOAD_TASK_NAME, KonanCompilerDownloadTask::class.java)
        project.extensions.add(ARTIFACTS_CONTAINER_NAME,
                project.container(KonanCompileConfig::class.java, KonanCompileConfigFactory(project)))
        project.extensions.add(INTEROP_CONTAINER_NAME,
                project.container(KonanInteropConfig::class.java, KonanInteropConfigFactory(project)))

        // Set additional project properties like konan.home, konan.build.targets etc.
        if (!project.hasProperty(ProjectProperty.KONAN_HOME)) {
            project.setProperty(ProjectProperty.KONAN_HOME, project.konanCompilerDownloadDir())
            project.setProperty(ProjectProperty.DOWNLOAD_COMPILER, true)
        }
        if (!project.hasProperty(ProjectProperty.KONAN_BUILD_TARGETS)) {
            project.setProperty(ProjectProperty.KONAN_BUILD_TARGETS, project.host)
        }

        // Create and set up aggregate building tasks.
        val compileKonanTask = project.getOrCreateTask("compileKonan").apply {
            dependsOn(project.supportedCompileTasks)
            group = BasePlugin.BUILD_GROUP
            description = "Compiles all the Kotlin/Native artifacts supported"
        }
        project.getTask("build").apply {
            dependsOn(compileKonanTask)
        }

        // Add default source paths after project evaluation.
        project.afterEvaluate(::setDefaultInputs)

        // Set compilation parameters for artifacts using interop.
        project.afterEvaluate { prj ->
            prj.konanArtifactsContainer.forEach { it.processInterops() }
        }

        // Create task to run supported executables.
        project.getOrCreateTask("run").apply {
            dependsOn(project.getTask("build"))
            doLast {
                for (task in project.tasks.withType(KonanCompileTask::class.java).matching { !it.isCrossCompile}) {
                    if (task?.produce == "program") {
                        project.exec {
                            with(it) {
                                commandLine(task.artifactPath)
                                if (project.extensions.extraProperties.has("runArgs")) {
                                    args(project.extensions.extraProperties.get("runArgs").toString().split(' '))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

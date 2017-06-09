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

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskCollection
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import javax.inject.Inject

/**
 * We use the following properties:
 *      konan.home       - directory where compiler is located (aka dist in konan project output).
 *      konan.version    - a konan compiler version for downloading.
 */

// konanHome extension is set by downloadKonanCompiler task.
internal val Project.konanHome: String
    get() {
        assert(extensions.extraProperties.has(KonanPlugin.KONAN_HOME_PROPERTY_NAME))
        return extensions.extraProperties.get(KonanPlugin.KONAN_HOME_PROPERTY_NAME).toString()
    }

internal val Project.konanBuildRoot               get() = "${buildDir.canonicalPath}/konan"
internal val Project.konanCompilerOutputDir       get() = "${konanBuildRoot}/bin"
internal val Project.konanInteropStubsOutputDir   get() = "${konanBuildRoot}/interopStubs"
internal val Project.konanInteropCompiledStubsDir get() = "${konanBuildRoot}/interopCompiledStubs"
internal val Project.konanInteropLibsOutputDir    get() = "${konanBuildRoot}/nativelibs"

internal val Project.konanArtifactsContainer: KonanArtifactsContainer
    get() = extensions.getByName(KonanPlugin.COMPILER_EXTENSION_NAME) as KonanArtifactsContainer
internal val Project.konanInteropContainer: KonanInteropContainer
    get() = extensions.getByName(KonanPlugin.INTEROP_EXTENSION_NAME) as KonanInteropContainer

internal val Project.konanCompilerDownloadTask  get() = tasks.getByName(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)

internal val Project.konanVersion
    get() = findProperty(KonanPlugin.KONAN_VERSION_PROPERTY_NAME) as String? ?: KonanPlugin.DEFAULT_KONAN_VERSION

internal fun Project.isTargetSupported(target: String?): Boolean {
    val targets = extensions.extraProperties.get(KonanPlugin.KONAN_BUILD_TARGETS).toString().trim().split(' ')
    if (targets.contains("all")) {
        return target?.isSupported() ?: true
    } else {
        return target == null || targets.contains(target)
    }
}

internal val Project.supportedCompileTasks: TaskCollection<KonanCompileTask>
    get() {

        return project.tasks.withType(KonanCompileTask::class.java).matching { isTargetSupported(it.target) }
    }

internal val Project.supportedInteropTasks: TaskCollection<KonanInteropTask>
    get() {
        return project.tasks.withType(KonanInteropTask::class.java).matching { isTargetSupported(it.target) }
    }

internal fun Project.konanCompilerName(): String =
        "kotlin-native-${KonanCompilerDownloadTask.simpleOsName()}-${this.konanVersion}"

internal fun Project.konanCompilerDownloadDir(): String =
        KonanCompilerDownloadTask.KONAN_PARENT_DIR + "/" + project.konanCompilerName()

class KonanArtifactsContainer(val project: ProjectInternal): AbstractNamedDomainObjectContainer<KonanCompileConfig>(
        KonanCompileConfig::class.java,
        project.gradle.services.get(Instantiator::class.java)) {

    override fun doCreate(name: String): KonanCompileConfig =
            KonanCompileConfig(name, project)
}

class KonanInteropContainer(val project: ProjectInternal): AbstractNamedDomainObjectContainer<KonanInteropConfig>(
        KonanInteropConfig::class.java,
        project.gradle.services.get(Instantiator::class.java)) {

    override fun doCreate(name: String): KonanInteropConfig =
            KonanInteropConfig(name, project)
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
            println("artifactPath       : ${task.artifactPath}")
            println("inputFiles         : ${task.inputFiles.joinToString(prefix = "[", separator = ", ", postfix = "]")}")
            println("libraries          : ${task.libraries}")
            println("nativeLibraries    : ${task.nativeLibraries}")
            println("linkerOpts         : ${task.linkerOpts}")
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
            println("linker             : ${task.linker}")
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

private fun String.isSupported(): Boolean {
    val os = KonanCompilerDownloadTask.simpleOsName()
    return when (os) {
        "macos" -> this == "macbook" || this == "iphone"
        "linux" -> this == "linux" || this == "raspberrypi"
        "windows" -> this == "mingw"
        else -> false
    }
}


class KonanPlugin @Inject constructor(private val registry: ToolingModelBuilderRegistry)
    : Plugin<ProjectInternal> {

    companion object {
        internal const val COMPILER_EXTENSION_NAME = "konanArtifacts"
        internal const val INTEROP_EXTENSION_NAME = "konanInterop"
        internal const val KONAN_DOWNLOAD_TASK_NAME = "downloadKonanCompiler"

        internal const val KONAN_HOME_PROPERTY_NAME = "konan.home"
        internal const val KONAN_VERSION_PROPERTY_NAME = "konan.version"
        internal const val KONAN_BUILD_TARGETS = "konan.build.targets"

        internal const val DEFAULT_KONAN_VERSION = "0.2"
    }

    /**
     * Looks for task with given name in the given project.
     * If such task isn't found, will create it. Returns created/found task.
     */
    private fun getTask(project: ProjectInternal, name: String): Task {
        var tasks = project.getTasksByName(name, false)
        assert(tasks.size <= 1)
        return if (tasks.isEmpty()) {
            project.tasks.create(name, DefaultTask::class.java)
        } else {
            tasks.single()
        }
    }

    // TODO: Create default config? what about test sources?
    override fun apply(project: ProjectInternal?) {
        if (project == null) { return }
        registry.register(KonanToolingModelBuilder)
        project.tasks.create(KONAN_DOWNLOAD_TASK_NAME, KonanCompilerDownloadTask::class.java)
        project.extensions.add(COMPILER_EXTENSION_NAME, KonanArtifactsContainer(project))
        project.extensions.add(INTEROP_EXTENSION_NAME, KonanInteropContainer(project))
        if (!project.extensions.extraProperties.has(KonanPlugin.KONAN_HOME_PROPERTY_NAME)) {
            project.extensions.extraProperties.set(KonanPlugin.KONAN_HOME_PROPERTY_NAME, project.konanCompilerDownloadDir())
        }
        val hostTarget =  when (KonanCompilerDownloadTask.simpleOsName()) {
            "macos" -> "macbook"
            "linux" -> "linux"
            "windows" -> "mingw"
            else -> throw IllegalStateException("Unsupported platform")
        }
        if (!project.extensions.extraProperties.has(KonanPlugin.KONAN_BUILD_TARGETS)) {
            project.extensions.extraProperties.set(KonanPlugin.KONAN_BUILD_TARGETS, hostTarget)
        }
        getTask(project, "clean").doLast {
            project.delete(project.konanBuildRoot)
        }
        getTask(project, "build").apply {
            dependsOn(project.supportedCompileTasks)
        }

        getTask(project, "run").apply {
            dependsOn(getTask(project, "build"))
            doLast {
                for (task in project.tasks.withType(KonanCompileTask::class.java).matching { it.target?.equals(hostTarget) ?: true}) {
                    if (task.artifactPath.endsWith("kexe")) {
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

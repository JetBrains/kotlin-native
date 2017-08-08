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

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.*
import java.io.File

/**
 *  What we can:
 *
 *  konanInterop {
 *      pkgName {
 *          defFile <def-file>
 *          pkg <package with stubs>
 *          target <target: linux/macbook/iphone/iphone_sim>
 *          compilerOpts <Options for native stubs compilation>
 *          linkerOpts <Options for native stubs >
 *          headers <headers to process>
 *          includeDirs <directories where headers are located>
 *          linkFiles <files which will be linked with native stubs>
 *          dumpParameters <Option to print parameters of task before execution>
 *      }
 *
 *      // TODO: add configuration for konan compiler
 *  }
 */

open class KonanInteropTask: KonanTargetableTask() {

    internal companion object {
        const val INTEROP_MAIN = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
    }

    internal fun init(libName: String) {
        dependsOn(project.konanCompilerDownloadTask)
        this.libName = libName
    }

    internal val INTEROP_JVM_ARGS: List<String>
        @Internal get() = listOf("-Dkonan.home=${project.konanHome}", "-Djava.library.path=${project.konanHome}/konan/nativelib")
    internal val INTEROP_CLASSPATH: String
        @Internal get() = "${project.konanHome}/konan/lib/"

    // Output directories -----------------------------------------------------

    /** Directory with autogenerated interop stubs (*.kt) */
    @OutputDirectory
    val stubsDir = project.file("${project.konanInteropStubsOutputDir}/$name")

    /** Directory with library bitcodes (*.bc) */
    @OutputDirectory
    val libsDir = project.file("${project.konanInteropLibsOutputDir}/$name")

    // Interop stub generator parameters -------------------------------------

    @Optional @InputFile var defFile: File? = null
        internal set
    @Optional @Input var pkg: String? = null
        internal set
    @Input lateinit var libName: String

    @Optional @Input var linker: String? = null
        internal set

    @Optional @Input var manifest: String? = null
        internal set

    @Input var dumpParameters: Boolean = false
    @Input val compilerOpts   = mutableListOf<String>()
    @Input val linkerOpts     = mutableListOf<String>()

    // TODO: Check if we can use only one FileCollection instead of set.
    @InputFiles val headers   = mutableSetOf<FileCollection>()
    @InputFiles val linkFiles = mutableSetOf<FileCollection>()

    @Input val konanVersion = project.konanVersion

    @TaskAction
    fun exec() {
        if (dumpParameters) dumpProperties(this@KonanInteropTask)

        project.javaexec {
            with(it) {
                main = INTEROP_MAIN
                classpath = project.fileTree(INTEROP_CLASSPATH).apply { include("*.jar") }
                jvmArgs(INTEROP_JVM_ARGS)
                environment("LIBCLANG_DISABLE_CRASH_RECOVERY", "1")
                // TODO: remove this hack.
                if (project.host == "mingw") {
                    environment("PATH", "${project.konanHome}\\dependencies\\msys2-mingw-w64-x86_64-gcc-6.3.0-clang-llvm-3.9.1-windows-x86-64\\bin;${System.getenv("PATH")}")
                }

                args(buildArgs().apply { logger.info("Interop args: ${this.joinToString(separator = " ")}") })
            }
        }
    }

    protected fun buildArgs() = mutableListOf<String>().apply {
        addArg("-properties", "${project.konanHome}/konan/konan.properties")
        addArg("-flavor", "native")

        addArg("-generated", stubsDir.canonicalPath)
        addArg("-natives", libsDir.canonicalPath)
        manifest ?.let {addArg("-manifest", it)}

        addArgIfNotNull("-target", target)
        addArgIfNotNull("-def", defFile?.canonicalPath)
        addArg("-pkg", pkg ?: libName)
        addArgIfNotNull("-linker", linker)

        addFileArgs("-h", headers)

        compilerOpts.forEach {
            addArg("-copt", it)
        }

        val linkerOpts = mutableListOf<String>().apply { addAll(linkerOpts) }
        linkFiles.forEach {
            linkerOpts.addAll(it.files.map { it.canonicalPath })
        }
        linkerOpts.forEach {
            addArg("-lopt", it)
        }
    }

}

open class KonanInteropConfig(
        val configName: String,
        val project: Project
): Named {

    override fun getName() = configName

    // Child tasks ------------------------------------------------------------

    // Task to process the library and generate stubs
    val generateStubsTask: KonanInteropTask = project.tasks.create(
            "gen${name.capitalize()}InteropStubs",
            KonanInteropTask::class.java) {
        it.init(name)
        it.manifest = "${it.stubsDir.path}/manifest.properties"
        it.group = BasePlugin.BUILD_GROUP
        it.description = "Generates stubs for the Kotlin/Native interop '$name'"
    }
    // Config and task to compile *.kt stubs into a library
    internal val compileStubsConfig = KonanCompileConfig("${name}InteropStubs", project, "compile").apply {
        compilationTask.dependsOn(generateStubsTask)
        outputDir("${project.konanInteropCompiledStubsDir}/$name")
        produce("library")
        inputFiles(project.fileTree(generateStubsTask.stubsDir).apply { builtBy(generateStubsTask) })
        manifest("${generateStubsTask.stubsDir.path}/manifest.properties")
        compilationTask.group = BasePlugin.BUILD_GROUP
        compilationTask.description = "Compiles stubs for the Kotlin/Native interop '${this@KonanInteropConfig.name}'"
    }
    val compileStubsTask = compileStubsConfig.compilationTask

    // DSL methods ------------------------------------------------------------

    fun defFile(file: Any) = with(generateStubsTask) {
        defFile = project.file(file)
    }

    fun pkg(value: String) = with(generateStubsTask) {
        pkg = value
    }

    fun target(value: String) = with(generateStubsTask) {
        generateStubsTask.target = value
        compileStubsTask.target = value
    }

    fun compilerOpts(vararg values: String) = with(generateStubsTask) {
        compilerOpts.addAll(values)
    }

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any) = with(generateStubsTask) {
        headers.add(project.files(files))
    }
    fun headers(files: FileCollection) = with(generateStubsTask) {
        headers.add(files)
    }


    fun includeDirs(vararg values: String) = with(generateStubsTask) {
        compilerOpts.addAll(values.map { "-I$it" })
    }

    fun linker(value: String) = with(generateStubsTask) {
        linker = value
    }

    fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    fun linkerOpts(values: List<String>) = with(generateStubsTask) {
        linkerOpts.addAll(values)
    }

    fun link(vararg files: Any) = with(generateStubsTask) {
        linkFiles.add(project.files(files))
    }
    fun link(files: FileCollection) = with(generateStubsTask) {
        linkFiles.add(files)
    }

    fun dumpParameters(value: Boolean) = with(generateStubsTask) {
        dumpParameters = value
        compileStubsTask.dumpParameters = value
    }

}


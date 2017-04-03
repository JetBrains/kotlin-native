package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File


open class KonanInteropTask: DefaultTask() {

    internal companion object {
        const val INTEROP_MAIN = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
    }

    init {
        dependsOn(project.konanCompilerDownloadTask)
    }

    internal val INTEROP_JVM_ARGS: List<String>
        get() = listOf("-Dkonan.home=${project.konanHome}", "-Djava.library.path=${project.konanHome}/konan/nativelib")
    internal val INTEROP_CLASSPATH: String
        get() = "${project.konanHome}/konan/lib/"


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
    @Optional @Input var target: String? = null
        internal set
    @Optional @Input var pkg:    String? = null
        internal set
    @Optional @Input var linker: String? = null
        internal set

    @Input val compilerOpts   = mutableListOf<String>()
    @Input val linkerOpts     = mutableListOf<String>()

    // TODO: Check if we can use only one FileCollection instead of set.
    @InputFiles val headers   = mutableSetOf<FileCollection>()
    @InputFiles val linkFiles = mutableSetOf<FileCollection>()

    @TaskAction
    fun exec() {
        project.javaexec {
            with(it) {
                main = INTEROP_MAIN
                classpath = project.fileTree(INTEROP_CLASSPATH).apply { include("*.jar") }
                jvmArgs(INTEROP_JVM_ARGS)
                environment("LIBCLANG_DISABLE_CRASH_RECOVERY", "1")
                environment("LD_LIBRARY_PATH", "${project.konanHome}/konan/nativelib")
                environment("DYLD_LIBRARY_PATH", "${project.konanHome}/konan/nativelib")

                args(buildArgs().apply { logger.info("Interop args: ${this.joinToString(separator = " ")}") })
            }
        }
    }

    protected fun buildArgs() = mutableListOf<String>().apply {
        addArg("-properties", "${project.konanHome}/konan/konan.properties")
        addArg("-flavor", "native")

        addArg("-generated", stubsDir.canonicalPath)
        addArg("-natives", libsDir.canonicalPath)

        addArgIfNotNull("-target", target)
        addArgIfNotNull("-def", defFile?.canonicalPath)
        addArgIfNotNull("-pkg", pkg)
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

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.konan.KonanVersion

class AppCDS(val project: Project) {
    private val dist = project.findProperty("konan.home") as? String ?: "dist"

    private val konanVersionFull = (project.findProperty("konanVersionFull") as? KonanVersion)
            ?: throw IllegalStateException("Kotlin/Native version is not set")

    val classesListPath: String = project.file("utilities/classes.list").absolutePath

    val sharedArchivePath: String = project.file(dist).resolve("tools/appcds_archive"/* +
            konanVersionFull.toString(showMeta = true, showBuild = true)*/).absolutePath

    val dumpArchiveArgs = listOf("-Xshare:dump", "-XX:SharedClassListFile=$classesListPath",
            "-XX:+UnlockDiagnosticVMOptions", "-XX:SharedArchiveFile=$sharedArchivePath")

    val useArchiveArgs = listOf("-Xshare:auto", "-XX:+UnlockDiagnosticVMOptions",
            "-XX:SharedArchiveFile=$sharedArchivePath")

    fun dumpArchiveTask() = project.tasks.create("DumpAppCDSArchive", JavaExec::class.java).apply {
        main = "org.jetbrains.kotlin.cli.utilities.MainKt"
        classpath = project.fileTree("$dist/konan/lib/").apply { include("*.jar")  }
        jvmArgs = dumpArchiveArgs
        val konanHome = project.file(dist).canonicalPath
        systemProperties(mutableMapOf(
                "konan.home" to konanHome,
                "java.library.path" to "$konanHome/konan/nativelib"))
        // TODO: env
    }
}
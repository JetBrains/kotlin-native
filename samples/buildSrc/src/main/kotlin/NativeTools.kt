@file:JvmName("NativeTools")

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import java.nio.file.Paths

/*
 * This file includes short-cuts needed purely for Kotlin/Native samples.
 */

// Needed purely for test purposes:
@get:JvmName("testMavenRepoUrl")
val testMavenRepoUrl by lazy { "file://" + Paths.get(userHome, ".m2-kotlin-native-samples") }

// This will be dropped after full support of JS-interop.
// Warning: May throw exception if no property was found!
fun Project.getJsInteropKlibFileName(packageName: String) =
        Paths.get(buildDir.toString(), "klib", "$packageName-jsinterop.klib").toString()

fun Project.createJsInteropTask(
        name: String,
        packageName: String
): Task {
    val task = tasks.create(name, Exec::class.java)
    task.workingDir = projectDir
    task.commandLine(
            Paths.get(properties["org.jetbrains.kotlin.native.home"] as String, "bin", "jsinterop").toString(),
            "-pkg", packageName,
            "-o", getJsInteropKlibFileName(packageName),
            "-target", "wasm32"
    )
    return task
}

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

fun Project.createStdlibTest(name: String, configure: (KonanStdlibTestRunner) -> Unit = {}): KonanStdlibTestRunner =
        project.tasks.create("execute${name.capitalize()}", KonanStdlibTestRunner::class.java).apply {
            // Configure test task.
            val testOutputStdlib = (project.findProperty("testOutputStdlib") as? File)?.toString()
                    ?: throw RuntimeException("Output directory testOutputStdlib is not set")
            val target = project.testTarget()
            executable = "$testOutputStdlib/${target.name}/$name.${target.family.exeSuffix}"
            useFilter = false
            testLogger = RunnerLogger.GTEST

            // Configure also
            configure(this)

            // Set dependencies.
            val compileTask = "compileKonan${name.capitalize()}"
            dependsOn(compileTask)
            setDistDependencyFor(compileTask)
            finalizedBy("resultsTask")
        }

fun Project.setDistDependencyFor(taskName: String) {
    project.setDistDependencyFor(project.tasks.getByName(taskName))
}

fun Project.setDistDependencyFor(t: Task) {
    val rootTasks = project.rootProject.tasks
    // We don't build the compiler if a custom dist path is specified.
    if (project.findProperty("useCustomDist") != null) {
        t.dependsOn(rootTasks.getByName("dist"), rootTasks.getByName("distPlatformLibs"))
        val target = project.testTarget()
        if (target != HostManager.host) {
            // if a test_target property is set then tests should depend on a crossDist
            // otherwise runtime components would not be build for a target.
            t.dependsOn(rootTasks.getByName("${target.name}CrossDist"),
                    rootTasks.getByName("${target.name}PlatformLibs"))
        }
    }
}

package org.jetbrains.kotlin.gradle.plugin.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

open class CompatibilityTests {
    val tmpFolder = TemporaryFolder()
        @Rule get

    val projectDirectory: File
        get() = tmpFolder.root

    @Test
    fun `Plugin should fail if running with Gradle prior to 4_6`() {
        val project = KonanProject.createEmpty(projectDirectory)
        val result = project
                .createRunner()
                .withGradleVersion("4.5")
                .withArguments("tasks")
                .buildAndFail()
        println(result.output)
        assertTrue("Build doesn't show the warning message") {
            result.output.contains("Kotlin/Native Gradle plugin is incompatible with this version of Gradle.")
        }
    }
}
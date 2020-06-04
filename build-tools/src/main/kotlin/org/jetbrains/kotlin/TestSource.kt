package org.jetbrains.kotlin

import org.gradle.api.Project
import java.nio.file.Paths

/**
 * Gets test sources from the input [source].
 * @param source a string that represents either a path to a test source or a directory with sources.
 * @param outputDirectory output directory
 */
fun Project.testSources(source: String, outputDirectory: String, language: Language): List<String> =
        if (project.file(source).isFile) {
            parseDirectives(project.file(source).toPath(), outputDirectory)
                    .apply {
                        forEach { it.writeTextToFile() }
                    }
                    .map { it.path }
        } else {
            project.fileTree(source) {
                // include only files with the language extension
                it.include("*${language.extension}")
            }.files.map { it.path }
        }

enum class Language(val extension: String) {
    Kotlin(".kt"), Obj(".m"), Swift(".swift")
}

/**
 * Represent a single test file that belongs to the [module].
 */
data class TestFile(val name: String,
                    val path: String,
                    var text: String = "",
                    val module: TestModule = TestModule.default,
                    val language: Language = Language.Kotlin
) {
    /**
     * Writes [text] to the file created from the [path].
     */
    fun writeTextToFile() {
        Paths.get(path).takeUnless { text.isEmpty() }?.run {
            parent.toFile()
                    .takeUnless { it.exists() }
                    ?.mkdirs()
            toFile().writeText(text)
        }
    }
}
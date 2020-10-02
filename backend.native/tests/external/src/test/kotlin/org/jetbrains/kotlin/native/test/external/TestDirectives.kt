/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.test.external

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

private const val MODULE_DELIMITER = ",\\s*"
// This pattern is a copy from the kotlin/compiler/tests-common/tests/org/jetbrains/kotlin/test/TestFiles.java
private val FILE_OR_MODULE_PATTERN: Pattern = Pattern.compile("(?://\\s*MODULE:\\s*([^()\\n]+)(?:\\(([^()]+(?:" +
        "$MODULE_DELIMITER[^()]+)*)\\))?\\s*(?:\\(([^()]+(?:$MODULE_DELIMITER[^()]+)*)\\))?\\s*)?//\\s*FILE:\\s*(.*)$",
        Pattern.MULTILINE)

/**
 * Creates test files from the given source file that may contain different test directives.
 *
 * @return list of test files [TestFile] to be compiled
 */
fun buildCompileList(source: Path, outputDirectory: String): List<TestFile> {
    val result = mutableListOf<TestFile>()
    val srcFile = source.toFile()
    // Remove diagnostic parameters in external tests.
    val srcText = srcFile.readText().replace(Regex("<!.*?!>(.*?)<!>")) { match -> match.groupValues[1] }

    if (srcText.contains("// WITH_COROUTINES")) {
        result.add(TestFile("helpers.kt", "$outputDirectory/helpers.kt",
                createTextForHelpers(true), TestModule.support))
    }

    val matcher = FILE_OR_MODULE_PATTERN.matcher(srcText)
    if (!matcher.find()) {
        // There is only one file in the input
        result.add(TestFile(srcFile.name, "$outputDirectory/${srcFile.name}", srcText))
    } else {
        // There are several files
        var processedChars = 0
        var module: TestModule = TestModule.default
        var nextFileExists = true
        while (nextFileExists) {
            var moduleName = matcher.group(1)
            val moduleDependencies = matcher.group(2)
            val moduleFriends = matcher.group(3)

            if (moduleName != null) {
                moduleName = moduleName.trim { it <= ' ' }
                module = TestModule("${srcFile.name}.$moduleName",
                        moduleDependencies.parseModuleList().map {
                            if (it != "support") "${srcFile.name}.$it" else it
                        },
                        moduleFriends.parseModuleList().map { "${srcFile.name}.$it" })
            }

            val fileName = matcher.group(4)
            val filePath = "$outputDirectory/$fileName"
            val start = processedChars
            nextFileExists = matcher.find()
            val end = if (nextFileExists) matcher.start() else srcText.length
            val fileText = srcText.substring(start, end)
            processedChars = end
            if (fileName.endsWith(".kt")) {
                result.add(TestFile(fileName, filePath, fileText, module))
            }
        }
    }
    return result
}

private fun String?.parseModuleList() = this
        ?.split(Pattern.compile(MODULE_DELIMITER), 0)
        ?: emptyList()

/**
 * Test module from the test source declared by the [FILE_OR_MODULE_PATTERN].
 * Module should have a [name] and could have [dependencies] on other modules and [friends].
 *
 * There are 2 predefined modules:
 *  - [default] that contains all sources that don't declare a module,
 *  - [support] for a helper sources like Coroutines support.
 */
data class TestModule(
        val name: String,
        val dependencies: List<String>,
        val friends: List<String>
) {
    fun isDefaultModule() = this == default || name.endsWith(".main")

    companion object {
        val default = TestModule("default", emptyList(), emptyList())
        val support = TestModule("support", emptyList(), emptyList())
    }
}

/**
 * Represent a single test file that belongs to the [module].
 */
data class TestFile(val name: String,
                    val path: String,
                    var text: String = "",
                    val module: TestModule = TestModule.default
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

/**
 * Exclude list format:
 * `path/to/file.kt  // comment`
 */
fun isExcluded(source: File, excludes: File): Boolean =
        excludes.readLines()
                .map {
                    // Remove comments section and drop all spaces in the end
                    it.substringBeforeLast("//").trim()
                }
                .find {
                    source.absolutePath.replace("\\", "/").contains(it)
                } != null

private fun findLinesWithPrefixesRemoved(text: String, prefix: String): List<String> =
        text.lines()
                .filter { it.startsWith(prefix) }
                .map { it.removePrefix(prefix) }

fun isEnabledForNativeBackend(source: File): Boolean {
    val text = source.readText()

    val languageSettings = findLinesWithPrefixesRemoved(text, "// !LANGUAGE: ")
    if (languageSettings.isNotEmpty()) {
        val settings = languageSettings.first()
        if (settings.contains("-ProperIeee754Comparisons") ||  // K/N supports only proper IEEE754 comparisons
                settings.contains("-ReleaseCoroutines")    ||  // only release coroutines
                settings.contains("-DataClassInheritance") ||  // old behavior is not supported
                settings.contains("-ProhibitAssigningSingleElementsToVarargsInNamedForm")) { // Prohibit such assignments
            return false
        }
    }

    val version = findLinesWithPrefixesRemoved(text, "// LANGUAGE_VERSION: ")
    if (version.isNotEmpty() && (!version.contains("1.3") || !version.contains("1.4"))) {
        // Support tests for 1.3 and exclude 1.2
        return false
    }

    val apiVersion = findLinesWithPrefixesRemoved(text, "// !API_VERSION: ")
    if (apiVersion.isNotEmpty() && !apiVersion.contains("1.4")) {
        return false
    }

    val targetBackend = findLinesWithPrefixesRemoved(text, "// TARGET_BACKEND")
    if (targetBackend.isNotEmpty()) {
        // There is some target backend. Check if it is NATIVE or not.
        return targetBackend.contains("NATIVE")
    } else {
        // No target backend. Check if NATIVE backend is ignored.
        if (findLinesWithPrefixesRemoved(text, "// IGNORE_BACKEND: ").any { it.contains("NATIVE") }) return false
        // No ignored backends. Check if test is targeted to FULL_JDK or has JVM_TARGET set
        if (findLinesWithPrefixesRemoved(text, "// FULL_JDK").isNotEmpty()) return false
        if (findLinesWithPrefixesRemoved(text, "// JVM_TARGET:").isNotEmpty()) return false
        return true
    }
}

fun parseLanguageFlags(source: File): List<String> {
    val text = source.readText()
    val flags = mutableListOf<String>()

    val languageSettings = findLinesWithPrefixesRemoved(text, "// !LANGUAGE: ")
    if (languageSettings.isNotEmpty()) {
        languageSettings.forEach { line ->
            line.split(" ").toList().forEach { flags.add("-XXLanguage:$it") }
        }
    }

    val experimentalSettings = findLinesWithPrefixesRemoved(text, "// !USE_EXPERIMENTAL: ")
    if (experimentalSettings.isNotEmpty()) {
        experimentalSettings.forEach { line ->
            line.split(" ").toList().forEach { flags.add("-Xopt-in=$it") }
        }
    }
    val expectActualLinker = findLinesWithPrefixesRemoved(text, "// EXPECT_ACTUAL_LINKER")
    if (expectActualLinker.isNotEmpty()) {
        flags.add("-Xexpect-actual-linker")
    }
    return flags
}
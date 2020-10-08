package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.utils.DFS
import java.io.File
import java.nio.file.Paths
import java.util.regex.Pattern

fun createTestFiles(src: File, outputDirectory: String): List<TestFile> {
    val identifier = "[a-zA-Z_][a-zA-Z0-9_]"
    val fullQualified = "[a-zA-Z_][a-zA-Z0-9_.*]"
    val importRegex = "(?m)^ *import +"

    val packagePattern = Regex("(?m)^ *package +(${fullQualified}*)")
    val importPattern = Regex("${importRegex}(${fullQualified}*)")
    val boxPattern = Regex("(?m)fun +box *( *)")
    val classPattern = Regex(".*(class|object|enum|interface) +(${identifier}*).")

    val sourceName = "_" + normalize(src.nameWithoutExtension)
    val packages = linkedSetOf<String>()
    val imports = mutableListOf<String>()
    val classes = mutableListOf<String>()
    val vars = hashSetOf<String>()    // variables or function parameters that have the same name as the package
    var mainModule: TestModule? = null

    val testFiles = buildCompileList(src.toPath(), "$outputDirectory/${src.name}")
    for (testFile in testFiles) {
        var text = testFile.text
        if (text.contains("COROUTINES_PACKAGE")) {
            text = text.replace("COROUTINES_PACKAGE", "kotlin.coroutines")
        }
        var pkg: String
        if (packagePattern.containsMatchIn(text)) {
            pkg = packagePattern.find(text)?.destructured?.component1() ?: ""
            packages.add(pkg)
            pkg = "$sourceName.$pkg"
            text = text.replaceFirst(packagePattern, "package $pkg")
        } else {
            pkg = sourceName
            text = insertInTextAfter(text, "\npackage $pkg\n", "@file:")
        }
        if (boxPattern.containsMatchIn(text)) {
            imports.add("${pkg}.*")
            mainModule = testFile.module
        }

        // Find mutable objects that should be marked as ThreadLocal
        if (testFile.name != "helpers.kt") {
            text = markMutableObjects(text)
        }
        testFile.text = text
    }
    for (testFile in testFiles) {
        var text = testFile.text
        // Find if there are any imports in the file
        if (importPattern.containsMatchIn(text)) {
            // Prepend package name to found imports
            importPattern.findAll(text).forEach { matchRes ->
                val importStatement: String = matchRes.destructured.component1()
                val subImport = importStatement.let {
                    val dotIdx = it.indexOf('.')
                    if (dotIdx > 0) it.substring(0, dotIdx) else it
                }
                if (packages.contains(subImport) || classes.contains(subImport)) {
                    // add only to those who import packages or import classes from the test files
                    text = text.replaceFirst(Regex("${importRegex}${Pattern.quote(importStatement)}"),
                            "import $sourceName.$importStatement")
                } else if (classPattern.containsMatchIn(text)) {
                    // special case for import from the local class
                    classPattern.findAll(text).forEach { clsMatch ->
                        val cl = clsMatch.destructured.component2()
                        classes.add(cl)
                        if (subImport == cl) {
                            text = text.replaceFirst(Regex("${importRegex}${Pattern.quote(importStatement)}"),
                                    "import $sourceName.$importStatement")
                        }
                    }
                }
            }
        } else if (packages.isEmpty()) {
            // Add import statement after package
            var pkg: String? = null
            if (packagePattern.containsMatchIn(text)) {
                pkg = "package " + packagePattern.find(text)
                        ?.destructured
                        ?.component1()
                text = text.replaceFirst(packagePattern, "")
            }
            text = insertInTextAfter(text, (if (pkg != null) "\n$pkg\n" else "") +
                    "import $sourceName.*\n", "@file:")
        }
        // now replace all package usages in full qualified names
        var res = ""                      // filesToCompile
        text.lines().forEach {
            var line = it
            packages.forEach { pkg ->
                // line contains val or var declaration or function parameter declaration
                if (Regex(".*va[lr] *$pkg( *: *$fullQualified*)?( *get\\(\\))? *=.*").matches(line) ||
                        Regex(".*fun .*\\(\n?\\s*$pkg:.*/").matches(line)) {
                    vars.add(pkg)
                }
                if (line.contains("$pkg.") &&
                        !(packagePattern.matches(line) || importPattern.matches(line)) &&
                        !vars.contains(pkg)) {
                    var idx = 0
                    while (line.indexOf(pkg, idx) >= 0) {
                        idx = line.indexOf(pkg, idx)
                        if (!Character.isJavaIdentifierPart(line[idx - 1])) {
                            line = line.substring(0, idx) + "$sourceName.$pkg" + line.substring(idx + pkg.length)
                            idx += sourceName.length + pkg.length + 1
                        } else {
                            idx += pkg.length
                        }
                    }
                }
            }
            res += "$line\n"
        }
        testFile.text = res
    }
    return mutableListOf<TestFile>().apply {
        addAll(testFiles)
        add(TestFile(
                name = "_launcher.kt",
                path = "$outputDirectory/${src.name}/_launcher.kt",
                text = createLauncherFileText(src, imports),
                module = mainModule ?: TestModule.default
        ))
    }
}

private fun normalize(name: String): String = name.replace('-', '_').replace('.', '_')

private fun markMutableObjects(text: String): String =
        mutableListOf<String>().apply {
            text.lines().forEach { line ->
                // FIXME: find only those who has vars inside
                // Find object declarations and companion objects
                if (line.matches(Regex("\\s*(private|public|internal)?\\s*object [a-zA-Z_][a-zA-Z0-9_]*\\s*.*"))
                        || line.matches(Regex("\\s*(private|public|internal)?\\s*companion object.*"))) {
                    add("@kotlin.native.ThreadLocal")
                }
                add(line)
            }
        }.joinToString(separator = System.lineSeparator())

/**
 * There are tests that require non-trivial 'package foo' in test launcher.
 */
private fun createLauncherFileText(src: File, imports: List<String>): String = StringBuilder().run {
    val pack = normalize(src.nameWithoutExtension)
    append("package _$pack\n")
    for (v in imports) {
        append("import $v\n")
    }
    append("""
    import kotlin.test.Test

    @Test
    fun runTest() {
        @Suppress("UNUSED_VARIABLE")
        val result = box()
        if (result != "OK") throw AssertionError("Test failed with: " + result)
    }""".trimIndent())
}.toString()

private fun insertInTextAfter(text: String, insert: String, after: String): String {
    val begin = text.indexOf(after)
    return if (begin != -1) {
        val end = text.indexOf("\n", begin)
        text.substring(0, end) + insert + text.substring(end)
    } else {
        insert + text
    }
}

class KonanExternalCompiler(private val project: Project,
                            private val enableKonanAssertions: Boolean,
                            private val twoStageCompilation: Boolean) {
    private fun runCompiler(filesToCompile: List<String>, output: String, moreArgs: List<String>) {
        val sources = filesToCompile.writeToArgFile("${output}_argfile")
        val args = mutableListOf("-output", output,
                "@${sources.absolutePath}")
        args.addAll(moreArgs)
        args.addAll(project.globalTestArgs)
        if (enableKonanAssertions) {
            args += "-ea"
        }
        project.compileKotlinNative(args.toList(), Paths.get(output), project.testTarget)
    }

    fun compileTestExecutable(sources: List<TestFile>,
                              executablePath: String,
                              flags: List<String>) {
        if (twoStageCompilation) {
            // Two-stage compilation.
            val klibPath = "$executablePath.klib"
            val files = sources.map { it.path }
            if (files.isNotEmpty()) {
                runCompiler(files, klibPath, flags + listOf("-p", "library"))
                runCompiler(emptyList<String>(), executablePath, flags + "-Xinclude=$klibPath")
            }
        } else {
            // Regular compilation with modules.
            val modules: Map<String, TestModule> = sources
                    .map { it.module }
                    .distinct()
                    .associateBy { it.name }

            val neighbors = object : DFS.Neighbors<TestModule> {
                override fun getNeighbors(current: TestModule): Iterable<TestModule> {
                    return current.dependencies.mapNotNull { modules[it] }
                }
            }
            val orderedModules: List<TestModule> = DFS.topologicalOrder(modules.values, neighbors)
            val libs = hashSetOf<String>()
            orderedModules.asReversed()
                    .filter { !it.isDefaultModule() }
                    .forEach { module ->
                        val klibModulePath = "$executablePath.${module.name}.klib"
                        libs.addAll(module.dependencies)
                        val klibs = libs.flatMap { listOf("-l", "$executablePath.${it}.klib") }
                        val friends = if (module.friends.isEmpty())
                            module.friends.flatMap { listOf("-friend-modules", "$executablePath.${it}.klib") }
                        else emptyList()
                        runCompiler(sources.filter { it.module == module }.map { it.path },
                                klibModulePath, flags + listOf("-p", "library") + klibs + friends)
                    }

            val compileMain = sources.filter {
                it.module.isDefaultModule() || it.module == TestModule.support
            }
            compileMain.forEach { f ->
                libs.addAll(f.module.dependencies)
            }
            val friends = compileMain.flatMap { it.module.friends }.toSet()
            if (compileMain.isNotEmpty()) {
                runCompiler(compileMain.map { it.path }, executablePath, flags +
                        libs.flatMap { listOf("-l", "$executablePath.${it}.klib") } +
                        friends.flatMap { listOf("-friend-modules", "$executablePath.${it}.klib") }
                )
            }
        }
    }
}
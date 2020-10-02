package org.jetbrains.kotlin.native.test.external

import org.junit.Test
import java.io.FileFilter
import java.nio.file.Paths

class ExternalTest {
    private val buildDir = Paths.get("build")
    private val excludes = Paths.get("excludelist").toFile()
    private val enableKonanAssertions = true
    private val enableTwoStageCompilation = System.getProperty("test.two_stage_compilation").toBoolean()
    private val verboseTest = System.getProperty("test.verbose").toBoolean()
    private val globalArgs = System.getProperty("test.global_args").split(" ")

    @Test
    fun run() {
        val testSourcesDir = buildDir.resolve("external")
        val testDirs = testSourcesDir.toFile()
                .walkTopDown()
                .onEnter { file ->
                    file.listFiles(FileFilter { it.name.endsWith(".kt") || it.isDirectory })!!.isNotEmpty()
                }
                .filter { it.isDirectory }
                .toList()
        for (dir in testDirs) {
            if (verboseTest) println("Processing: $dir")
            // Build tests in the group
            val compileFlags = mutableListOf("-tr")
            // FIXME: empty global args is a string "[]"
//            compileFlags.addAll(globalArgs)
            val testName = dir.path.toString()
                    .substring("build/".length)
                    .replace(Regex("[-/\\\\]"), "_")
            val outputDirectory = buildDir.resolve("external_test")
                    .resolve(dir.path.substring("build/".length))
                    .toFile()
                    .also { it.mkdirs() }
            if (verboseTest) println("Output dir: $outputDirectory")
            val executablePath = "$outputDirectory/program.tr"

            val ktFiles = dir.listFiles(FileFilter { it.isFile && it.name.endsWith(".kt") })
                    ?: throw RuntimeException("$dir is empty")
            val compileList = mutableListOf<TestFile>()
                    ktFiles.filter { !isExcluded(it, excludes) && isEnabledForNativeBackend(it) }
                    .forEach {
                        compileFlags.addAll(parseLanguageFlags(it))
                        compileList.addAll(createTestFiles(it, outputDirectory.toString()))
                    }
            compileList.forEach { it.writeTextToFile() }
            try {
                KonanExternalCompiler(enableKonanAssertions, enableTwoStageCompilation)
                        .compileTestExecutable(compileList, executablePath, compileFlags)
            } catch (ex: Exception) {
                println("""
                    Failed to compile the following files:
                    ${compileList.joinToString(prefix = "[", separator = ", ", postfix = "]")}
                    """.trimIndent())
                throw RuntimeException("Compilation failed", ex)
            }

            // Run the tests
            val testArgs = mutableListOf("--ktest_logger=SILENT")
            ktFiles.forEach { file ->
                testArgs += "--ktest_filter=_${normalize(file.nameWithoutExtension)}.*"
                if (!isExcluded(file, excludes) && isEnabledForNativeBackend(file)) {
                    runExecutable()
                } else {
                    println("Test skipped: ${file.name}")
                }
            }
        }
    }

    private fun runExecutable() {
        TODO("Not yet implemented")
    }
}
/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.ExecSpec

import java.io.File
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

import org.jetbrains.kotlin.konan.target.HostManager

abstract class KonanTest : DefaultTask() {
    enum class Logger {
        EMPTY,    // Built without test runner
        GTEST,    // Google test log output
        TEAMCITY, // TeamCity log output
        SIMPLE,   // Prints simple messages of passed/failed tests
        SILENT    // Prints no log of passed/failed tests
    }

    var disabled: Boolean
        get() = !enabled
        @Optional set(value) { enabled = !value }

    /**
     * Test output directory. Used to store processed sources and binary artifacts.
     */
    lateinit var outputDirectory: String

    /**
     * Test logger to be used for the test built with TestRunner (`-tr` option).
     */
    @Optional
    lateinit var testLogger: Logger

    /**
     * Test executable arguments.
     */
    @Input
    lateinit var arguments: MutableList<String>

    /**
     * Test executable.
     */
    @Input
    lateinit var executable: String

    /**
     * Test source.
     */
    @Optional
    lateinit var source: String

    /**
     * Sets test filtering to choose the exact test in the executable built with TestRunner.
     */
    @Input
    var useFilter = true

    /**
     * An action to be executed before the build.
     * As this run task comes after the build task all actions for doFirst
     * should be done before the build and not run.
     */
    @Optional
    var doBefore: Action<in Task>? = null

    @Suppress("UnstableApiUsage")
    override fun configure(config: Closure<*>): Task {
        super.configure(config)

        // Set Gradle properties for the better navigation
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Kotlin/Native test infrastructure task"

        if (!::arguments.isInitialized) {
            arguments = mutableListOf()
        }
        if (::testLogger.isInitialized && testLogger != Logger.EMPTY) {
            arguments.add("--ktest_logger=$testLogger")
        }
        if (useFilter && ::source.isInitialized) {
            arguments.add("--ktest_filter=${source.convertToPattern()}")
        }
        project.setDistDependencyFor(this)
        return this
    }

    @TaskAction
    open fun run() = project.executeAndCheck(project.file(executable).toPath(), arguments)

    // Converts to runner's pattern
    private fun String.convertToPattern() = this.replace('/', '.').replace(".kt", "") + (".*")

    internal fun ProcessOutput.print(prepend: String = "") {
        if (project.verboseTest)
            println(prepend + """
                |stdout:$stdOut
                |stderr:$stdErr
                |exit code: $exitCode
                """.trimMargin())
    }
}

/**
 * Create a test task of the given type. Supports configuration with Closure passed form build.gradle file.
 */
fun <T: KonanTest> Project.createTest(name: String, type: Class<T>, config: Closure<*>): T =
        project.tasks.create(name, type).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)
            if (enabled) {
                // Configure test task.
                val target = project.testTarget
                val compileTask = project.tasks.getByName("compileKonan${name.capitalize()}${target.name.capitalize()}")
                // If run task depends on something, compile task should also depend on this.
                val dependencies = dependsOn.toList() // save to the list, otherwise it will cause cyclic dependency.
                compileTask.dependsOn(dependencies)
                // Run task should depend on compile task.
                dependsOn(compileTask)
                setDistDependencyFor(compileTask)
                if (doBefore != null) compileTask.doFirst(doBefore!!)
                compileTask.enabled = enabled
            }
        }

/**
 * Task to run tests compiled with TestRunner.
 * Runs tests with GTEST output and parses it to create statistics info
 */
open class KonanGTest : KonanTest() {
    init {
        // Use GTEST logger to parse test results later
        testLogger = Logger.GTEST
        outputDirectory = "${project.testOutputStdlib}/$name"
    }

    lateinit var statistics: Statistics

    @TaskAction
    override fun run() = runProcess(
            executor = project.executor::execute,
            executable = executable,
            args = arguments
    ).run {
        statistics = parse(stdOut)
        print()
        check(exitCode == 0) { "Test $executable exited with $exitCode" }
    }

    private fun parse(output: String): Statistics = Statistics().apply {
        Pattern.compile("\\[  PASSED  ] ([0-9]*) tests\\.").matcher(output)
                .apply { if (find()) pass(group(1).toInt()) }

        Pattern.compile("\\[  FAILED  ] ([0-9]*) tests.*").matcher(output)
                .apply { if (find()) fail(group(1).toInt()) }
        if (total == 0) {
            // No test were run. Try to find if we've tried to run something
            this.error(Pattern.compile("\\[={10}] Running ([0-9]*) tests from ([0-9]*) test cases\\..*")
                    .matcher(output)
                    .run { if (find()) group(1).toInt() else 1 })
        }
    }
}

/**
 * Task to run tests built into a single predefined binary named `localTest`.
 * Note: this task should depend on task that builds a test binary.
 */
open class KonanLocalTest : KonanTest() {
    init {
        // local tests built into a single binary with the known name
        val target = project.testTarget
        outputDirectory = project.testOutputLocal
        executable = "$outputDirectory/${target.name}/localTest.${target.family.exeSuffix}"
        testLogger = Logger.SILENT
    }

    @Optional
    var expectedExitStatus = 0

    /**
     * Should this test fail or not.
     */
    @Optional
    var expectedFail = false

    /**
     * Used to validate output as a gold value.
     */
    @Optional
    lateinit var goldValue: String

    /**
     * Checks test's output against gold value and returns true if the output matches the expectation.
     */
    @Optional
    var outputChecker: (String) -> Boolean = { str -> (!::goldValue.isInitialized || goldValue == str) }

    /**
     * Input test data to be passed to process' stdin.
     */
    @Optional
    lateinit var testData: String

    /**
     * Should compiler message be read and validated with output checker or gold value.
     */
    @Optional
    var compilerMessages = false

    @TaskAction
    override fun run() {
        val output = if (::testData.isInitialized)
            runProcessWithInput(project.executor::execute, executable, arguments, testData)
        else
            runProcess(project.executor::execute, executable, arguments)
        if (compilerMessages) {
            // TODO: as for now it captures output only in the driver task.
            // It should capture output from the build task using Gradle's LoggerManager and LoggerOutput
            val compilationLog = project.file("$executable.compilation.log").readText()
            output.stdOut = compilationLog + output.stdOut
        }
        output.check()
        output.print()
    }

    private fun ProcessOutput.check() {
        val exitCodeMismatch = exitCode != expectedExitStatus
        if (exitCodeMismatch) {
            val message = "Expected exit status: $expectedExitStatus, actual: $exitCode"
            check(expectedFail) { """
                    |Test failed. $message
                    |stdout: $stdOut
                    |stderr: $stdErr
                    """.trimMargin()
            }
            println("Expected failure. $message")
        }

        val result = stdOut + stdErr
        val goldValueMismatch = !outputChecker(result.replace(System.lineSeparator(), "\n"))
        if (goldValueMismatch) {
            val message = if (::goldValue.isInitialized)
                "Expected output: $goldValue, actual output: $result"
            else
                "Actual output doesn't match with output checker: $result"

            check(expectedFail) { "Test failed. $message" }
            println("Expected failure. $message")
        }

        check(!exitCodeMismatch && !goldValueMismatch && !expectedFail) { "Unexpected pass" }
    }
}

/**
 * Executes a standalone tests provided with either @param executable or by the tasks @param name.
 * The executable itself should be built by the konan plugin.
 */
open class KonanStandaloneTest : KonanLocalTest() {
    init {
        val target = project.testTarget
        outputDirectory = "${project.testOutputLocal}/$name"
        executable = "$outputDirectory/${target.name}/$name.${target.family.exeSuffix}"
        useFilter = false
        testLogger = Logger.EMPTY
    }

    @Optional
    var enableKonanAssertions = true

    private var _flags: MutableList<String> = mutableListOf()
    /**
     * Compiler flags used to build a test.
     */
    var flags: MutableList<String>
        get() {
                if (enableKonanAssertions) _flags.add("-ea")
                return _flags
            }
        @Optional
        set(value) {
            _flags = value
        }

    fun getSources() = buildCompileList(outputDirectory)
}

/**
 * This is another way to run the konanc compiler. It runs a konanc shell script.
 *
 * @note This task is not intended for regular testing as project.exec + a shell script isolate the jvm from IDEA.
 * @see KonanLocalTest to be used as a regular task.
 */
open class KonanDriverTest : KonanStandaloneTest() {
    override fun configure(config: Closure<*>): Task {
        super.configure(config)
        if (doBefore != null) doFirst(doBefore!!)
        return this
    }

    @TaskAction
    override fun run() {
        konan()
        super.run()
    }

    private fun konan() {
        val dist = project.rootProject.file(project.findProperty("org.jetbrains.kotlin.native.home") ?:
        project.findProperty("konan.home") ?: "dist")
        val konancDriver = if (HostManager.hostIsMingw) "konanc.bat" else "konanc"
        val konanc = File("${dist.canonicalPath}/bin/$konancDriver").absolutePath

        File(executable).mkdirs()

        val args = mutableListOf("-output", executable).apply {
            if (project.testTarget != HostManager.host) {
                add("-target")
                add(project.testTarget.visibleName)
            }
            addAll(getSources())
            addAll(flags)
            addAll(project.globalTestArgs)
            if (enableKonanAssertions) add("-ea")
        }

        // run konanc compiler locally
        runProcess(localExecutor(project), konanc, args).run {
            print("Konanc compiler execution:")
            project.file("$executable.compilation.log").run {
                writeText(stdOut)
                writeText(stdErr)
            }
            check(exitCode == 0) { "Compiler failed with exit code $exitCode" }
        }
    }
}

open class KonanInteropTest : KonanStandaloneTest() {
    /**
     * Name of the interop library
     */
    @Input
    lateinit var interop: String
}

open class KonanLinkTest : KonanStandaloneTest() {
    @Input
    lateinit var lib: String
}

/**
 * Test task to check a library built by `-produce dynamic`.
 * C source code should contain `testlib` as a reference to a testing library.
 * It will be replaced then by the actual library name.
 */
open class KonanDynamicTest : KonanStandaloneTest() {
    /**
     * File path to the C source.
     */
    @Input
    lateinit var cSource: String

    @TaskAction
    override fun run() {
        clang()
        super.run()
    }

    // Replace testlib_api.h and all occurrences of the testlib with the actual name of the test
    private fun processCSource(): String {
        val sourceFile = File(cSource)
        val res = sourceFile.readText()
                .replace("#include \"testlib_api.h\"", "#include \"lib${name}_api.h\"")
                .replace("testlib", "lib${name}")
        val newFileName = "$outputDirectory/${sourceFile.name}"
        println(newFileName)
        File(newFileName).run {
            createNewFile()
            writeText(res)
        }
        return newFileName
    }

    private fun clang() {
        val log = ByteArrayOutputStream()
        val plugin = project.convention.getPlugin(ExecClang::class.java)
        val execResult = plugin.execKonanClang(project.testTarget, Action<ExecSpec> {
            it.workingDir = File(outputDirectory)
            it.executable = "clang"
            val artifactsDir = "$outputDirectory/${project.testTarget}"
            it.args = listOf(processCSource(),
                    "-o", executable,
                    "-I", artifactsDir,
                    "-L", artifactsDir,
                    "-l", name,
                    "-Wl,-rpath,$artifactsDir")

            it.standardOutput = log
            it.errorOutput = log
            it.isIgnoreExitValue = true
        })
        log.toString("UTF-8").also {
            project.file("$executable.compilation.log").writeText(it)
            println(it)
        }
        execResult.assertNormalExitValue()
    }
}
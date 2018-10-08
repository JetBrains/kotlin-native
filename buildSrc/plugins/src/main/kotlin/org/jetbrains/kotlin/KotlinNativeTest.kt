package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

enum class RunnerLogger {
    GTEST,
    TEAMCITY,
    SIMPLE,
    SILENT
}

open class KonanTestRunner : DefaultTask() {
    @Optional
    var testLogger = RunnerLogger.SILENT

    @Input
    lateinit var arguments: List<String>

    @Input
    lateinit var executable: String

    @Optional
    lateinit var source: String

    @Input
    var useFilter = true

    override fun configure(config: Closure<*>): Task {
        super.configure(config)
        if (!::arguments.isInitialized) {
            arguments = ArrayList()
        }
        arguments += "--ktest_logger=$testLogger"
        if (useFilter && ::source.isInitialized) {
            arguments += "--ktest_filter=${source.convertToPattern()}"
        }
        return this
    }

    @TaskAction
    open fun run() = project.executeAndCheck(project.file(executable).toPath(), arguments)

    // Converts to runner's pattern
    private fun String.convertToPattern() = this.replace('/', '.').replace(".kt", "") + (".*")
}

/**
 * Task to run and parse output of stdlib tests
 */
open class KonanStdlibTestRunner : KonanTestRunner() {
    init {
        // Use GTEST logger to parse test results later
        testLogger = RunnerLogger.GTEST
    }

    lateinit var statistics: Statistics

    @TaskAction
    override fun run() {
        val (stdOut, stdErr, exitCode) = runProcess(executor = project.executor::execute,
                executable = executable, args = arguments)
        statistics = parse(stdOut)
        println("$stdOut$stdErr")
        check(exitCode == 0) { "Test $executable exited with $exitCode" }
    }

    private fun parse(output: String): Statistics = Statistics().apply {
        Pattern.compile("\\[  PASSED  ] ([0-9]*) tests\\.").matcher(output)
                .apply { if (find()) pass(group(1).toInt()) }

        Pattern.compile("\\[  FAILED  ] ([0-9]*) tests.*").matcher(output)
                .apply { if (find()) fail(group(1).toInt()) }
        if (total == 0) {
            // No test were run. Try to find if there we tried to run something
            error(Pattern.compile("\\[={10}] Running ([0-9]*) tests from ([0-9]*) test cases\\..*")
                    .matcher(output).run { if (find()) group(1).toInt() else 1 })
        }
    }
}

data class Statistics(
        var passed: Int = 0,
        var failed: Int = 0,
        var error: Int = 0,
        var skipped: Int = 0) {

    val total: Int
        get() = passed + failed + error + skipped

    fun pass(count: Int = 1) { passed += count }

    fun skip(count: Int = 1) { skipped += count }

    fun fail(count: Int = 1) { failed += count }

    fun error(count: Int = 1) { error += count }

    fun add(other: Statistics) {
        passed += other.passed
        failed += other.failed
        error += other.error
        skipped += other.skipped
    }
}

open class KonanLocalTestRunner : KonanTestRunner() {
    init {
        val testOutputLocal = project.findProperty("testOutputLocal")
        val target = project.testTarget()
        executable = "$testOutputLocal/${target.name}/localTest.${target.family.exeSuffix}"
    }

    @Optional
    var expectedExitStatus = 0

    @Optional
    var expectedFail = false

    @Optional
    lateinit var goldValue: String

    @Optional
    lateinit var testData: String

    @TaskAction
    override fun run() {
        val (stdOut, stdErr, exitCode) = when(::testData.isInitialized) {
            true -> runProcessWithInput(project.executor::execute, executable, arguments, testData)
            false -> runProcess(project.executor::execute, executable, arguments)
        }
        val exitCodeMismatch = exitCode != expectedExitStatus
        if (exitCodeMismatch) {
            val message = "Expected exit status: $expectedExitStatus, actual: $exitCode"
            check(expectedFail) { """
                Test failed. $message
                stdout: $stdOut
                stderr: $stdErr
                """.trimIndent() }
            println("Expected failure. $message")
        }

        val result = stdOut + stdErr
        val goldValueMismatch = ::goldValue.isInitialized && goldValue != result.replace(System.lineSeparator(), "\n")
        if (goldValueMismatch) {
            val message = "Expected output: $goldValue, actual output: $result"
            check(expectedFail) { "Test failed. $message" }
            println("Expected failure. $message")
        }

        check(!exitCodeMismatch && !goldValueMismatch && !expectedFail) { "Unexpected pass" }
    }
}
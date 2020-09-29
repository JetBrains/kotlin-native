/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

import java.nio.file.Paths
import java.util.function.Function

class RunExternalTestGroup extends JavaExec {
    def platformManager = project.rootProject.platformManager
    def target = platformManager.targetManager(project.testTarget).target
    def dist = UtilsKt.getKotlinNativeDist(project)

    def enableKonanAssertions = true
    String outputDirectory = null
    String goldValue = null
    // Checks test's output against gold value and returns true if the output matches the expectation
    Function<String, Boolean> outputChecker = { str -> (goldValue == null || goldValue == str) }
    boolean printOutput = true
    String testData = null
    int expectedExitStatus = 0
    List<String> arguments = null
    List<String> flags = null

    boolean multiRuns = false
    List<List<String>> multiArguments = null

    boolean expectedFail = false
    boolean compilerMessages = false

    // Uses directory defined in $outputSourceSetName source set
    void createOutputDirectory() {
        if (outputDirectory != null) {
            return
        }
        def outputSourceSet = UtilsKt.getTestOutputExternal(project)
        outputDirectory = Paths.get(outputSourceSet, name).toString()
        project.file(outputDirectory).mkdirs()
    }

    RunExternalTestGroup() {
        // We don't build the compiler if a custom dist path is specified.
        UtilsKt.dependsOnDist(this)
        main = 'org.jetbrains.kotlin.cli.bc.K2NativeKt'
    }

    @Override
    void exec() {
        // Perhaps later we will return this exec() back but for now rest of infrastructure expects
        // compilation begins on runCompiler call, to emulate this behaviour we call super.exec() after
        // configuration part at runCompiler.
    }

    // FIXME: output directory here changes and hence this is not a property
    String executablePath() { return "$outputDirectory/program.tr" }

    OutputStream out

    void runExecutable() {
        if (!enabled) {
            println "Test is disabled: $name"
            return
        }
        def program = executablePath()
        def suffix = target.family.exeSuffix
        def exe = "$program.$suffix"

        println "execution: $exe"

        def compilerMessagesText = compilerMessages ? project.file("${program}.compilation.log").getText('UTF-8') : ""

        out = new ByteArrayOutputStream()
        //TODO Add test timeout

        def times = multiRuns ? multiArguments.size() : 1

        def exitCodeMismatch = false
        for (int i = 0; i < times; i++) {
            ExecResult execResult = project.execute {

                commandLine exe

                if (arguments != null) {
                    args arguments
                }
                if (multiRuns && multiArguments[i] != null) {
                    args multiArguments[i]
                }
                if (testData != null) {
                    standardInput = new ByteArrayInputStream(testData.bytes)
                }
                standardOutput = out

                ignoreExitValue = true
            }

            exitCodeMismatch |= execResult.exitValue != expectedExitStatus
            if (exitCodeMismatch) {
                def message = "Expected exit status: $expectedExitStatus, actual: ${execResult.exitValue}"
                if (this.expectedFail) {
                    println("Expected failure. $message")
                } else {
                    throw new TestFailedException("Test failed on iteration $i. $message\n ${out.toString("UTF-8")}")
                }
            }
        }
        def result = compilerMessagesText + out.toString("UTF-8")
        if (printOutput) {
            println(result)
        }
        result = result.replace(System.lineSeparator(), "\n")
        def goldValueMismatch = !outputChecker.apply(result)
        if (goldValueMismatch) {
            def message
            if (goldValue != null) {
                message = "Expected output: $goldValue, actual output: $result"
            } else {
                message = "Actual output doesn't match output checker: $result"
            }
            if (this.expectedFail) {
                println("Expected failure. $message")
            } else {
                throw new TestFailedException("Test failed. $message")
            }
        }

        if (!exitCodeMismatch && !goldValueMismatch && this.expectedFail) println("Unexpected pass")
    }

    /**
     * If true, the test executable will be built in two stages:
     * 1. Build a klibrary from sources.
     * 2. Build a final executable from this klibrary.
     */
    @Input
    public Boolean enableTwoStageCompilation = false

    @Input
    def groupDirectory = "."

    String filter = project.findProperty("filter")

    def testGroupReporter = new KonanTestGroupReportEnvironment(project)

    static String normalize(String name) {
        name.replace('.kt', '')
                .replace('-','_')
                .replace('.', '_')
    }

    @TaskAction
    void executeTest() {
        createOutputDirectory()
        // Form the test list.
        List<File> ktFiles = project.buildDir.toPath().resolve(groupDirectory).toFile()
                .listFiles({
                    it.isFile() && it.name.endsWith(".kt")
                } as FileFilter)
        if (filter != null) {
            def pattern = ~filter
            ktFiles = ktFiles.findAll {
                it.name =~ pattern
            }
        }

        File excludes = project.file("excludelist")

        testGroupReporter.suite(name) { suite ->
            // Build tests in the group
            flags = (flags ?: []) + "-tr"
            List<TestFile> compileList = []
            ktFiles.each {
                if (!TestDirectivesKt.isExcluded(it, excludes) && TestDirectivesKt.isEnabledForNativeBackend(it)) {
                    flags.addAll(TestDirectivesKt.parseLanguageFlags(it))
                    compileList.addAll(ExternalTestFactoryKt.createTestFiles(it, outputDirectory))
                }
            }
            compileList*.writeTextToFile()
            try {
                new KonanExternalCompiler(project, enableKonanAssertions, enableTwoStageCompilation)
                        .compileTestExecutable(compileList, executablePath(), flags)
            } catch (Exception ex) {
                project.logger.quiet("ERROR: Compilation failed for test suite: $name with exception", ex)
                project.logger.quiet("The following files were unable to compile:")
                ktFiles.each { project.logger.quiet(it.name) }
                suite.abort(ex, ktFiles.size())
                throw new RuntimeException("Compilation failed", ex)
            }

            // Run the tests.
            arguments = (arguments ?: []) + "--ktest_logger=SILENT"
            ktFiles.each { file ->
                def savedArgs = arguments
                arguments += "--ktest_filter=_${normalize(file.name)}.*"
                use(KonanTestSuiteReportKt) {
                    project.logger.quiet("TEST: $file.name " +
                            "(done: $testGroupReporter.statistics.total/${ktFiles.size()}, " +
                            "passed: $testGroupReporter.statistics.passed, " +
                            "skipped: $testGroupReporter.statistics.skipped)")
                }
                if (!TestDirectivesKt.isExcluded(file, excludes) && TestDirectivesKt.isEnabledForNativeBackend(file)) {
                    suite.executeTest(file.name) {
                       project.logger.quiet(project.buildDir.relativePath(file))
                       runExecutable()
                    }
                } else {
                    suite.skipTest(file.name)
                }
                arguments = savedArgs
            }
        }
    }
}

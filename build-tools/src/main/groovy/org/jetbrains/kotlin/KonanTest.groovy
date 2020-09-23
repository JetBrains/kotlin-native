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
import org.jetbrains.kotlin.utils.DFS

import java.nio.file.Paths
import java.util.function.Function
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import java.util.stream.Collectors

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

    protected void runCompiler(List<String> filesToCompile, String output, List<String> moreArgs) {
        def log = new ByteArrayOutputStream()
        try {
            classpath = project.fileTree("$dist.canonicalPath/konan/lib/") {
                include '*.jar'
            }
            jvmArgs "-Xmx4G"
            enableAssertions = true
            def sources = File.createTempFile(name,".lst")
            sources.deleteOnExit()
            def sourcesWriter = sources.newWriter()
            filesToCompile.each { f ->
                sourcesWriter.write(f.chars().any { Character.isWhitespace(it) }
                        ? "\"${f.replace("\\", "\\\\")}\"\n" // escape file name
                        : "$f\n")
            }
            sourcesWriter.close()
            args = ["-output", output,
                    "@${sources.absolutePath}",
                    *moreArgs,
                    *project.globalTestArgs]
            if (project.testTarget) {
                args "-target", target.visibleName
            }
            if (enableKonanAssertions) {
                args "-ea"
            }
            if (project.hasProperty("test_verbose")) {
                println("Files to compile: $filesToCompile")
                println(args)
            }
            standardOutput = log
            errorOutput = log
            super.exec()
        } finally {
            def logString = log.toString("UTF-8")
            project.file("${output}.compilation.log").write(logString)
            println(logString)
        }
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
    public def enableTwoStageCompilation = false

    @Input
    def groupDirectory = "."

    String filter = project.findProperty("filter")

    def testGroupReporter = new KonanTestGroupReportEnvironment(project)

    void parseLanguageFlags(String src) {
        def text = project.buildDir.toPath().resolve(src).text
        def languageSettings = findLinesWithPrefixesRemoved(text, "// !LANGUAGE: ")
        if (languageSettings.size() != 0) {
            languageSettings.forEach { line ->
                line.split(" ").toList().forEach { flags.add("-XXLanguage:$it") }
            }
        }

        def experimentalSettings = findLinesWithPrefixesRemoved(text, "// !USE_EXPERIMENTAL: ")
        if (experimentalSettings.size() != 0) {
            experimentalSettings.forEach { line ->
                line.split(" ").toList().forEach { flags.add("-Xopt-in=$it") }
            }
        }
        def expectActualLinker = findLinesWithPrefixesRemoved(text, "// EXPECT_ACTUAL_LINKER")
        if (expectActualLinker.size() != 0) {
            flags.add("-Xexpect-actual-linker")
        }
    }

    static String normalize(String name) {
        name.replace('.kt', '')
                .replace('-','_')
                .replace('.', '_')
    }

    static List<String> findLinesWithPrefixesRemoved(String text, String prefix) {
        def result = []
        text.eachLine {
            if (it.startsWith(prefix)) {
                result.add(it - prefix)
            }
        }
        return result
    }

    static def excludeList = [
            "external/compiler/codegen/boxInline/multiplatform/defaultArguments/receiversAndParametersInLambda.kt", // KT-36880
            "external/compiler/compileKotlinAgainstKotlin/specialBridgesInDependencies.kt",         // FIXME: inherits final class
            "external/compiler/codegen/box/multiplatform/multiModule/expectActualTypealiasLink.kt", // KT-40137
            "external/compiler/codegen/box/multiplatform/multiModule/expectActualMemberLink.kt",    // KT-33091
            "external/compiler/codegen/box/multiplatform/multiModule/expectActualLink.kt",          // KT-41901
            "external/compiler/codegen/box/coroutines/multiModule/"                                 // KT-40121
    ]

    boolean isEnabledForNativeBackend(String fileName) {
        def text = project.buildDir.toPath().resolve(fileName).text

        if (excludeList.any { fileName.replace(File.separator, "/").contains(it) }) return false

        def languageSettings = findLinesWithPrefixesRemoved(text, '// !LANGUAGE: ')
        if (!languageSettings.empty) {
            def settings = languageSettings.first()
            if (settings.contains('-ProperIeee754Comparisons') ||  // K/N supports only proper IEEE754 comparisons
                    settings.contains('-ReleaseCoroutines') ||     // only release coroutines
                    settings.contains('-DataClassInheritance') ||  // old behavior is not supported
                    settings.contains('-ProhibitAssigningSingleElementsToVarargsInNamedForm')) { // Prohibit these assignments
                return false
            }
        }

        def version = findLinesWithPrefixesRemoved(text, '// LANGUAGE_VERSION: ')
        if (version.size() != 0 && (!version.contains("1.3") || !version.contains("1.4"))) {
            // Support tests for 1.3 and exclude 1.2
            return false
        }

        def apiVersion = findLinesWithPrefixesRemoved(text, '// !API_VERSION: ')
        if (apiVersion.size() != 0 && !apiVersion.contains("1.4")) {
            return false
        }

        def targetBackend = findLinesWithPrefixesRemoved(text, "// TARGET_BACKEND")
        if (targetBackend.size() != 0) {
            // There is some target backend. Check if it is NATIVE or not.
            for (String s : targetBackend) {
                if (s.contains("NATIVE")){ return true }
            }
            return false
        } else {
            // No target backend. Check if NATIVE backend is ignored.
            def ignoredBackends = findLinesWithPrefixesRemoved(text, "// IGNORE_BACKEND: ")
            for (String s : ignoredBackends) {
                if (s.contains("NATIVE")) { return false }
            }
            // No ignored backends. Check if test is targeted to FULL_JDK or has JVM_TARGET set
            if (!findLinesWithPrefixesRemoved(text, "// FULL_JDK").isEmpty()) { return false }
            if (!findLinesWithPrefixesRemoved(text, "// JVM_TARGET:").isEmpty()) { return false }
            return true
        }
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

        testGroupReporter.suite(name) { suite ->
            // Build tests in the group
            flags = (flags ?: []) + "-tr"
            List<TestFile> compileList = []
            ktFiles.each {
                def src = project.buildDir.relativePath(it)
                if (isEnabledForNativeBackend(src)) {
                    parseLanguageFlags(src)
                    compileList.addAll(ExternalTestFactoryKt.createTestFiles(it, outputDirectory))
                }
            }
            compileList*.writeTextToFile()
            try {
                if (enableTwoStageCompilation) {
                    // Two-stage compilation.
                    def klibPath = "${executablePath()}.klib"
                    def files = compileList.stream()
                            .map { it.path }
                            .collect(Collectors.toList())
                    if (!files.empty) {
                        runCompiler(files, klibPath, flags + ["-p", "library"])
                        runCompiler([], executablePath(), flags + ["-Xinclude=$klibPath"])
                    }
                } else {
                    // Regular compilation with modules.
                    Map<String, TestModule> modules = compileList.stream()
                            .map { it.module }
                            .distinct()
                            .collect(Collectors.toMap({ it.name }, UnaryOperator.identity() ))

                    List<TestModule> orderedModules = DFS.INSTANCE.topologicalOrder(modules.values()) { module ->
                        module.dependencies.collect { modules[it] }.findAll { it != null }
                    }
                    Set<String> libs = new HashSet<String>()
                    orderedModules.reverse().each { module ->
                        if (!module.isDefaultModule()) {
                            def klibModulePath = "${executablePath()}.${module.name}.klib"
                            libs.addAll(module.dependencies)
                            def klibs = libs.collectMany { ["-l", "${executablePath()}.${it}.klib"] }.toList()
                            def friends = module.friends ?
                                    module.friends.collectMany {
                                        ["-friend-modules", "${executablePath()}.${it}.klib"]
                                    }.toList() : []
                            runCompiler(compileList.findAll { it.module == module }.collect { it.path },
                                    klibModulePath, flags + ["-p", "library"] + klibs + friends)
                        }
                    }

                    def compileMain = compileList.findAll {
                        it.module.isDefaultModule() || it.module == TestModule.support
                    }
                    compileMain.forEach { f ->
                        libs.addAll(f.module.dependencies)
                    }
                    def friends = compileMain.collectMany {it.module.friends }.toSet()
                    if (!compileMain.empty) {
                        runCompiler(compileMain.collect { it.path }, executablePath(), flags +
                                libs.collectMany { ["-l", "${executablePath()}.${it}.klib"] }.toList() +
                                friends.collectMany {["-friend-modules", "${executablePath()}.${it}.klib"]}.toList()
                        )
                    }
                }
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
                def src = project.buildDir.relativePath(file)
                def savedArgs = arguments
                arguments += "--ktest_filter=_${normalize(file.name)}.*"
                use(KonanTestSuiteReportKt) {
                    project.logger.quiet("TEST: $file.name " +
                            "(done: $testGroupReporter.statistics.total/${ktFiles.size()}, " +
                            "passed: $testGroupReporter.statistics.passed, " +
                            "skipped: $testGroupReporter.statistics.skipped)")
                }
                if (isEnabledForNativeBackend(src)) {
                    suite.executeTest(file.name) {
                       project.logger.quiet(src)
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

package org.jetbrains.kotlin

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

abstract class KonanTest extends DefaultTask {
    protected String source
    def backendNative = project.project(":backend.native")
    def runtimeProject = project.project(":runtime")
    def dist = project.rootProject.file("dist")
    def runtimeBc = new File("${dist.canonicalPath}/lib/runtime.bc").absolutePath
    def launcherBc = new File("${dist.canonicalPath}/lib/launcher.bc").absolutePath
    def startKtBc = new File("${dist.canonicalPath}/lib/start.kt.bc").absolutePath
    def stdlibKtBc = new File("${dist.canonicalPath}/lib/stdlib.kt.bc").absolutePath
    def mainC = 'main.c'
    def outputSourceSetName = "testOutputLocal"
    String outputDirectory = null
    String goldValue = null
    String testData = null
    List<String> arguments = null

    boolean enabled = true

    public void setDisabled(boolean value) {
        this.enabled = !value
    }

    // Uses directory defined in $outputSourceSetName source set.
    // If such source set doesn't exist, uses temporary directory.
    public void createOutputDirectory() {
        if (outputDirectory != null) {
            return
        }

        def outputSourceSet = project.sourceSets.findByName(getOutputSourceSetName())
        if (outputSourceSet != null) {
            outputDirectory = outputSourceSet.output.getDirs().getSingleFile().absolutePath + "/$name"
            project.file(outputDirectory).mkdirs()
        } else {
            outputDirectory = getTemporaryDir().absolutePath
        }
    }

    public KonanTest(){
        // TODO: that's a long reach up the project tree.
        // May be we should reorganize a little.
        dependsOn(project.rootProject.tasks['dist'])
    }

    abstract void compileTest(List<String> filesToCompile, String exe)

    protected void runCompiler(List<String> filesToCompile, String output, List<String> moreArgs) {
        def log = new ByteArrayOutputStream()
        try {
            project.javaexec {
                main = 'org.jetbrains.kotlin.cli.bc.K2NativeKt'
                classpath = project.configurations.cli_bc
                jvmArgs "-ea",
                        "-Dkonan.home=${dist.canonicalPath}",
                        "-Djava.library.path=${dist.canonicalPath}/konan/nativelib"
                args("-output", output,
                        *filesToCompile,
                        *moreArgs,
                        *project.globalArgs)
                standardOutput = log
                errorOutput = log
            }
        } finally {
            def logString = log.toString()
            project.file("${output}.compilation.log").write(logString)
            println(logString)
        }
    }

    protected void runCompiler(String source, String output, List<String> moreArgs) {
        runCompiler([source], output, moreArgs)
    }

    String buildExePath() {
        def exeName = project.file(source).name.replace(".kt", ".kt.exe")
        return "$outputDirectory/$exeName"
    }

    // TODO refactor
    List<String> buildCompileList() {
        def result = []
        def filePattern = ~/(?m)\/\/\s*FILE:\s*(.*)$/
        def srcFile = project.file(source)
        def srcText = srcFile.text
        def matcher = filePattern.matcher(srcText)

        if (!matcher.find()) {
            // There is only one file in the input
            project.copy{
                from srcFile.absolutePath
                into outputDirectory
            }
            def newFile ="$outputDirectory/${srcFile.name}"
            result.add(newFile)
        } else {
            // There are several files
            def processedChars = 0
            while (true) {
                def filePath = "$outputDirectory/${matcher.group(1)}"
                def start = processedChars
                def nextFileExists = matcher.find()
                def end = nextFileExists ? matcher.start() : srcText.length()
                def fileText = srcText.substring(start, end)
                processedChars = end
                createFile(filePath, fileText)
                result.add(filePath)
                if (!nextFileExists) break
            }
        }
        return result
    }

    void createFile(String file, String text) {
        project.file(file).write(text)
    }

    @TaskAction
    void executeTest() {
        createOutputDirectory()
        def exe = buildExePath()

        compileTest(buildCompileList(), exe)
        println "execution :$exe"

        def out = null
        //TODO Add test timeout
        project.exec {
            commandLine exe
            if (arguments != null) {
                args arguments
            }
            if (testData != null) {
                standardInput = new ByteArrayInputStream(testData.bytes)
            }
            if (goldValue != null) {
                out = new ByteArrayOutputStream()
                standardOutput = out
            }

        }
        if (goldValue != null && goldValue != out.toString()) {
            throw new TestFailedException("test failed.")
        }
    }
}

class TestFailedException extends RuntimeException {
    public TestFailedException(String s) {
        super(s)
    }
}
@ParallelizableTask
class RunKonanTest extends KonanTest {
    void compileTest(List<String> filesToCompile, String exe) {
        runCompiler(filesToCompile, exe, [])
    }
}

@ParallelizableTask
class RunInteropKonanTest extends KonanTest {

    private String interop
    private NamedNativeInteropConfig interopConf

    void setInterop(String value) {
        this.interop = value
        this.interopConf = project.kotlinNativeInterop[value]
        this.dependsOn(this.interopConf.genTask)
    }

    void compileTest(List<String> filesToCompile, String exe) {
        String interopBc = exe + "-interop.bc"
        runCompiler([interopConf.generatedSrcDir.absolutePath], interopBc, ["-nolink"])

        String interopStubsBc = new File(interopConf.nativeLibsDir, interop + "stubs.bc").absolutePath

        runCompiler(filesToCompile, exe, ["-library", interopBc, "-nativelibrary", interopStubsBc])
    }
}

@ParallelizableTask
class LinkKonanTest extends KonanTest {
    protected String lib

    void compileTest(List<String> filesToCompile, String exe) {
        def libDir = project.file(lib).absolutePath
        def libBc = "${libDir}.bc"

        runCompiler(lib, libBc, ['-nolink', '-nostdlib'])
        runCompiler(filesToCompile, exe, ['-library', libBc])
    }
}

@ParallelizableTask
class RunExternalTestGroup extends RunKonanTest {

    def groupDirectory = "."
    def outputSourceSetName = "testOutputExternal"
    String filter = project.findProperty("filter")
    Map<String, TestResult> results = [:]
    Statistics statistics = new Statistics()

    RunExternalTestGroup() {
        goldValue = "OK"
    }

    static enum TestStatus {
        PASSED,
        FAILED,
        ERROR,
        SKIPPED
    }
    static class TestResult {
        TestStatus status = null
        String comment = null

        TestResult(TestStatus status, String comment = ""){
            this.status = status;
            this.comment = comment;
        }
    }
    static class Statistics {
        int total = 0
        int passed = 0
        int failed = 0
        int error = 0
        int skipped = 0

        void pass(int count = 1) {
            passed += count
            total += count
        }

        void skip(int count = 1) {
            skipped += count
            total += count
        }

        void fail(int count = 1) {
            failed += count
            total += count
        }

        void error(int count = 1) {
            error += count
            total += count
        }

        void add(Statistics other) {
            total   += other.total
            passed  += other.passed
            failed  += other.failed
            error  += other.error
            skipped += other.skipped
        }
    }

    List<String> buildCompileList() {
        def packagePattern = ~/(?m)package\s*([a-zA-z-][a-zA-Z0-9._$-]*)/
        def boxPattern = ~/(?m)fun\s*box\s*\(\s*\)/
        def boxPackage = ""

        def result = super.buildCompileList()
        for (String filePath : result) {
            def text = project.file(filePath).text
            if (text =~ boxPattern && text =~ packagePattern){
                boxPackage = (text =~ packagePattern)[0][1]
                boxPackage += '.'
                break
            }
        }
        createLauncherFile("$outputDirectory/_launcher.kt", boxPackage)
        result.add("$outputDirectory/_launcher.kt")
        result.add(project.file("testUtils.kt"))
        return result
    }

    /**
     * There are tests that require non-trivial 'package foo' in test launcher.
     */
    void createLauncherFile(String file, String pkg) {
        createFile(file, """
import kotlin.test.TestFailedException

fun main(args : Array<String>) {
  try { 
    print(${pkg}box())
  } catch (e:TestFailedException) {
    print("FAIL")
  }
}
""")
    }

    List<String> findLinesWithPrefixesRemoved(String text, String prefix) {
        def result = []
        text.eachLine {
            if (it.startsWith(prefix)) {
                result.add(it - prefix)
            }
        }
        return result
    }

    boolean isEnabledForNativeBackend(String fileName) {
        def text = project.file(fileName).text
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
            return true
        }
    }

    @TaskAction
    @Override
    void executeTest() {
        createOutputDirectory()

        // Form the test list.
        List<File> ktFiles = project.file(groupDirectory).listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                pathname.isFile() && pathname.name.endsWith(".kt")
            }
        })
        if (filter != null) {
            def pattern = ~filter
            ktFiles = ktFiles.findAll {
                it.name =~ pattern
            }
        }

        // Run the tests.
        def currentResult = null
        statistics = new Statistics()
        ktFiles.each {
            source = project.relativePath(it)
            def testCase = testCase(it.name, statistics)
            println("TEST: $it.name ($statistics.total/${ktFiles.size()}, passed: $statistics.passed, skipped: $statistics.skipped)")
            testCase.start()
            if (isEnabledForNativeBackend(source)) {
                try {
                    super.executeTest()
                    currentResult = testCase.pass()
                } catch (TestFailedException e) {
                    currentResult = testCase.fail(e)
                } catch (Exception ex) {
                    currentResult = testCase.error(ex)
                }
            } else {
                currentResult = testCase.skip()
            }
            println("TEST $currentResult.status\n")
            results.put(it.name, currentResult)
        }

        // Save the report.
        def reportFile = project.file("${outputDirectory}/results.json")
        def json = JsonOutput.toJson(["statistics" : statistics, "tests" : results])
        reportFile.write(JsonOutput.prettyPrint(json))
        println("TOTAL PASSED: $statistics.passed/$statistics.total (SKIPPED: $statistics.skipped)")
    }

    KonanTestCase testCase(String name, Statistics statistics) {
        if (System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE") != null)
            return new TeamcityKonanTestCase(name, statistics)
        return new KonanTestCase(name, statistics)
    }

    class KonanTestCase {
        protected name;
        protected statistics;
        KonanTestCase(String name, Statistics statistics) {
            this.name       = name
            this.statistics = statistics
        }

        void start(){}

        TestResult pass() {
            statistics.pass()
            return new TestResult(TestStatus.PASSED)
        }

        TestResult fail(TestFailedException e) {
            statistics.fail()
            return new TestResult(TestStatus.FAILED, "Cause: ${e.getCause()?.getMessage()}")
        }

        TestResult error(Exception e) {
            statistics.error()
            return new TestResult(TestStatus.ERROR, "Exception: ${e.getMessage()}. Cause: ${e.getCause()?.getMessage()}")
        }

        TestResult skip() {
            return new TestResult(TestStatus.SKIPPED)
        }
    }

    class TeamcityKonanTestCase extends KonanTestCase {
        TeamcityKonanTestCase(String name, Statistics statistics) {
            super(name, statistics)
        }

        private teamcityReport(String msg) {
            println("##teamcity[$msg]")
        }

        private teamcityFinish() {
            teamcityReport("testFinished name='$name'")
        }

        void start() {
            teamcityReport("testStarted name='$name'")
        }

        TestResult pass() {
            teamcityFinish()
            return super.pass()
        }


        TestResult fail(TestFailedException e) {
            teamcityReport("testFailed type='comparisonFailure' name='$name' message='${e.getMessage()}'")
            teamcityFinish()
            return super.fail(e)
        }

        TestResult error(Exception e) {
            def writer = new StringWriter()
            e.printStackTrace(new PrintWriter(writer))
            def rawString  = writer.toString()
            /**
             * Teamcity require escaping some symbols in pipe manner.
             * https://github.com/GitTools/GitVersion/issues/94
             */
            def formatedString = rawString
                    .replaceAll("\r",  "|r")
                    .replaceAll("\n",  "|n")
                    .replaceAll("'",   "|'")
                    .replaceAll("|",   "||")
                    .replaceAll("\\[", "|[")
                    .replaceAll("]",   "|]")
            teamcityReport("testFailed name='$name' message='${e.getMessage()}' details='${formatedString}'")
            teamcityFinish()
            return super.error(e)
        }

        TestResult skip() {
            teamcityReport("testIgnored name='$name'")
            teamcityFinish()
            return super.skip()
        }
    }
}

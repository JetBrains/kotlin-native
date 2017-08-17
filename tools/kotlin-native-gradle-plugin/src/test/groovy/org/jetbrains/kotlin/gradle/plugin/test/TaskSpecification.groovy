package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification


class TaskSpecification extends BaseKonanSpecification {

    def 'Configs should allow user to add dependencies to them'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile("main.kt")
        }
        project.buildFile.append("""
            task beforeInterop(type: DefaultTask) { doLast { println("Before Interop") } }
            task beforeCompilation(type: DefaultTask) { doLast { println("Before compilation") } }
        """.stripIndent())
        project.addInteropSetting("dependsOn", "beforeInterop")
        project.addCompilationSetting("dependsOn", "beforeCompilation")
        def result = project.createRunner().withArguments('build').build()

        then:
        def beforeInterop = result.task(":beforeInterop")
        beforeInterop != null && beforeInterop.outcome == TaskOutcome.SUCCESS
        def beforeCompilation = result.task(":beforeCompilation")
        beforeCompilation != null && beforeCompilation.outcome == TaskOutcome.SUCCESS
    }

    def 'Compilation config should work with konanInterop from another project'() {
        when:
        def rootProject = KonanProject.create(projectDirectory) { KonanProject it ->
            it.buildFile.append("evaluationDependsOn(':interop')\n")
            it.createFile("settings.gradle", "include ':interop'")
            it.addCompilationSetting("useInterop", "project(':interop').konanInterop['interop']")
        }
        def interopProjectDir = rootProject.createSubDir("interop")
        def interopProject = KonanInteropProject.createEmpty(interopProjectDir) { KonanInteropProject it ->
            it.generateBuildFile("""
                apply plugin: 'konan'

                konanInterop {
                    interop { }
                }
            """.stripIndent())
            it.interopTasks = [":interop:genInteropInteropStubs", ":interop:compileInteropInteropStubs"]
            it.generateDefFile("interop.def")
        }
        def result = rootProject.createRunner().withArguments("build").build()

        then:
        result.taskPaths(TaskOutcome.SUCCESS).containsAll(rootProject.compilationTasks + interopProject.interopTasks)
    }

    def 'Compilation should support interop parameters changing after `useInterop` call'() {
        when:
        def project = KonanInteropProject.create(projectDirectory)
        project.addInteropSetting("linkerOpts", "'-lpthread'")
        project.buildFile.append("""
            task printArgs {
                dependsOn 'build'
                doLast {
                    println(konanArtifacts['$project.DEFAULT_ARTIFACT_NAME'].compilationTask.linkerOpts)
                    konanArtifacts['$project.DEFAULT_ARTIFACT_NAME'].compilationTask.libraries.each { println it.files }
                    konanArtifacts['$project.DEFAULT_ARTIFACT_NAME'].compilationTask.nativeLibraries.each { println it.files }
                }
            }
        """.stripIndent())
        def result = project.createRunner().withArguments('printArgs').build()

        then:
        result.task(":printArgs") != null
        result.task(":printArgs").outcome == TaskOutcome.SUCCESS
        def expectedKlibPath = project.konanBuildDir.toPath()
                .resolve("interopCompiledStubs/stdioInteropStubs/stdioInteropStubs.klib")
                .toFile().canonicalPath
        def expectedBcPath = project.konanBuildDir.toPath()
                .resolve("nativelibs/genStdioInteropStubs/stdiostubs.bc")
                .toFile().canonicalPath
        result.output.contains("""
            [-lpthread]
            [$expectedKlibPath]
            [$expectedBcPath]
        """.stripIndent().trim())
    }

}

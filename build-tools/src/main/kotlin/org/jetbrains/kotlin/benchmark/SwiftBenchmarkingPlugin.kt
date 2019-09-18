package org.jetbrains.kotlin.benchmark

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File
import javax.inject.Inject
import java.nio.file.Paths

private val NamedDomainObjectContainer<KotlinSourceSet>.commonMain
    get() = maybeCreate("commonMain")

private val NamedDomainObjectContainer<KotlinSourceSet>.nativeMain
    get() = maybeCreate("nativeMain")

private val Project.benchmark: SwiftBenchmarkExtension
    get() = extensions.getByName(SwiftBenchmarkingPlugin.BENCHMARK_EXTENSION_NAME) as SwiftBenchmarkExtension

private val Project.nativeWarmup: Int
    get() = (property("nativeWarmup") as String).toInt()

private val Project.attempts: Int
    get() = (property("attempts") as String).toInt()

private val Project.nativeBenchResults: String
    get() = property("nativeBenchResults") as String

private val Project.compilerArgs: List<String>
    get() = (findProperty("compilerArgs") as String?)?.split("\\s").orEmpty()

enum class CodeSizeEntity { FRAMEWORK, EXECUTABLE }

open class SwiftBenchmarkExtension @Inject constructor(val project: Project) {
    var applicationName: String = project.name
    var commonSrcDirs: Collection<Any> = emptyList()
    var nativeSrcDirs: Collection<Any> = emptyList()
    var swiftSources: List<String> = emptyList()
    var linkerOpts: Collection<String> = emptyList()
    var compileTasks: List<String> = emptyList()
    var useCodeSize: CodeSizeEntity = CodeSizeEntity.FRAMEWORK         // use as code size metric framework size or executable

    val dependencies: BenchmarkDependencies = BenchmarkDependencies()

    fun dependencies(action: BenchmarkDependencies.() -> Unit) =
        dependencies.action()

    fun dependencies(action: Closure<*>) {
        ConfigureUtil.configure(action, dependencies)
    }

    inner class BenchmarkDependencies  {
        private val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
            get() = project.kotlin.sourceSets

        fun project(path: String): Dependency = project.dependencies.project(mapOf("path" to path))

        fun project(path: String, configuration: String): Dependency =
            project.dependencies.project(mapOf("path" to path, "configuration" to configuration))

        fun common(notation: Any) = sourceSets.commonMain.dependencies {
            implementation(notation)
        }

        fun native(notation: Any) = sourceSets.nativeMain.dependencies {
            implementation(notation)
        }
    }
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class SwiftBenchmarkingPlugin: Plugin<Project> {
    private fun Project.determinePreset(): KotlinNativeTargetPreset = kotlin.presets.macosX64 as KotlinNativeTargetPreset

    private fun Project.configureSourceSets(kotlinVersion: String) {
        with(kotlin.sourceSets) {
            commonMain.dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinStdlibVersion")
            }

            project.configurations.getByName(nativeMain.implementationConfigurationName).apply {
                // Exclude dependencies already included into K/N distribution (aka endorsed libraries).
                exclude(mapOf("module" to "kotlinx.cli"))
            }

            repositories.maven {
                it.setUrl(kotlinStdlibRepo)
            }

            // Add sources specified by a user in the benchmark DSL.
            afterEvaluate {
                benchmark.let {
                    commonMain.kotlin.srcDirs(*it.commonSrcDirs.toTypedArray())
                    nativeMain.kotlin.srcDirs(*(it.nativeSrcDirs).toTypedArray())
                }
            }
        }
    }

    private fun Project.configureNativeTarget(hostPreset: KotlinNativeTargetPreset) {
        kotlin.targetFromPreset(hostPreset, NATIVE_TARGET_NAME) {
            compilations.getByName("main").kotlinOptions.freeCompilerArgs = project.compilerArgs + listOf("-l", "kotlinx-cli")
            binaries.framework(NATIVE_FRAMEWORK_NAME, listOf(RELEASE)) {
                // Specify settings configured by a user in the benchmark extension.
                afterEvaluate {
                    linkerOpts.addAll(benchmark.linkerOpts)
                }
            }
        }
    }

    private fun Project.configureMPPExtension() {
        configureSourceSets(kotlinVersion)
        configureNativeTarget(determinePreset())
    }

    private fun Project.configureTasks() {
        val nativeTarget = kotlin.targets.getByName(NATIVE_TARGET_NAME) as KotlinNativeTarget
        val executable = Paths.get(buildDir.absolutePath, benchmark.applicationName)
        // Build executable from swift code.
        val framework = nativeTarget.binaries.getFramework(NATIVE_FRAMEWORK_NAME, NativeBuildType.RELEASE)
        val buildSwift = tasks.create("buildSwift") { task ->
            task.dependsOn(framework.linkTaskName)
            task.doLast {
                val frameworkParentDirPath = framework.outputDirectory.absolutePath
                val options = listOf("-Xlinker", "-rpath", "-Xlinker", frameworkParentDirPath, "-F", frameworkParentDirPath)
                compileSwift(project, nativeTarget.konanTarget, benchmark.swiftSources, options,
                        Paths.get(buildDir.absolutePath, benchmark.applicationName), false)
            }
        }
        framework.linkTask.finalizedBy(buildSwift)

        // Native run task.
        val konanRun = createRunTask(this, "konanRun", buildSwift, executable.toString(),
                buildDir.resolve(nativeBenchResults).absolutePath).apply {
            group = BENCHMARKING_GROUP
            description = "Runs the benchmark for Kotlin/Native."
        }
        afterEvaluate {
            (konanRun as RunKotlinNativeTask).args(
                    "-w", nativeWarmup.toString(),
                    "-r", attempts.toString(),
                    "-p", "${benchmark.applicationName}::"
            )
        }

        // Native report task.
        val konanJsonReport = tasks.create("konanJsonReport") {

            it.group = BENCHMARKING_GROUP
            it.description = "Builds the benchmarking report for Kotlin/Native."

            it.doLast {
                val applicationName = benchmark.applicationName
                val nativeCompileTime = getNativeCompileTime(applicationName, benchmark.compileTasks)
                val benchContents = buildDir.resolve(nativeBenchResults).readText()
                val frameworkPath = framework.outputFile.absolutePath
                val codeSizeEntity = if (benchmark.useCodeSize == CodeSizeEntity.FRAMEWORK)
                    File("$frameworkPath/$NATIVE_FRAMEWORK_NAME").canonicalPath
                else
                    executable.toString()

                val properties = commonBenchmarkProperties + mapOf(
                    "type" to "native",
                    "compilerVersion" to konanVersion,
                    "flags" to nativeTarget.compilations.main.kotlinOptions.freeCompilerArgs.map { "\"$it\"" },
                    "benchmarks" to benchContents,
                    "compileTime" to listOf(nativeCompileTime),
                    "codeSize" to getCodeSizeBenchmark(applicationName, codeSizeEntity)
                )

                val output = createJsonReport(properties)
                buildDir.resolve(nativeJson).writeText(output)
            }
        }

        // JVM run task.
        val jvmRun = tasks.create("jvmRun", RunJvmTask::class.java) {
            it.doLast {
                println("JVM run is unsupported")
            }
        }

        // JVM report task.
        val jvmJsonReport = tasks.create("jvmJsonReport") {

            it.group = BENCHMARKING_GROUP
            it.description = "Builds the benchmarking report for Kotlin/JVM."

            it.doLast {
                println("JVM run is unsupported")
            }

            jvmRun.finalizedBy(it)
        }
    }

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kotlin-multiplatform")

        // Use Kotlin compiler version specified by the project property.
        dependencies.add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

        extensions.create(BENCHMARK_EXTENSION_NAME, SwiftBenchmarkExtension::class.java, this)
        configureMPPExtension()
        addTimeListener(this)
        configureTasks()
    }

    companion object {
        const val NATIVE_TARGET_NAME = "native"
        const val NATIVE_FRAMEWORK_NAME = "benchmark"
        const val BENCHMARK_EXTENSION_NAME = "swiftBenchmark"

        const val BENCHMARKING_GROUP = "benchmarking"
    }
}

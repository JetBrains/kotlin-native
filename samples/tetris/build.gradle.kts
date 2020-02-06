import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

val hostOs = System.getProperty("os.name")
val isWindows = hostOs.startsWith("Windows")

// If RaspberryPi target is activated.
val isRaspberryPiBuild =
    project.findProperty("tetris.raspberrypi.build")?.toString()?.toBoolean() == true

// If host platform is Windows and x86 target is activated.
val isMingwX86Build =
    isWindows && project.findProperty("tetris.mingwX86.build")?.toString()?.toBoolean() == true

val winCompiledResourceFile = buildDir.resolve("compiledWindowsResources/Tetris.res")

val kotlinNativeDataPath = System.getenv("KONAN_DATA_DIR")?.let { File(it) }
    ?: File(System.getProperty("user.home")).resolve(".konan")

val mingwPath = if (isMingwX86Build)
    File(System.getenv("MINGW32_DIR") ?: "C:/msys32/mingw32")
else
    File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64")

kotlin {
    when {
        isRaspberryPiBuild -> linuxArm32Hfp("tetris") // aka RaspberryPi
        isMingwX86Build -> mingwX86("tetris")
        else -> when {
            hostOs == "Mac OS X" -> macosX64("tetris")
            hostOs == "Linux" -> linuxX64("tetris")
            hostOs.startsWith("Windows") -> mingwX64("tetris")
            else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
        }
    }.also {
        println("$project has been configured for ${it.preset?.name} platform.")
    }.apply {
        binaries {
            executable {
                entryPoint = "sample.tetris.main"

                // Compile Windows Resources
                if (konanTarget.family == org.jetbrains.kotlin.konan.target.Family.MINGW) {
                    val taskName = linkTaskName.replaceFirst("link", "windres")
                    val inFile = File("src/tetrisMain/resources/Tetris.rc")
                    val outFile = buildDir.resolve("processedResources/$taskName.res")
                    val windresTask = tasks.register<Exec>(taskName) {
                        val llvmDir = when (target.konanTarget.architecture.bitness) {
                            32 -> kotlinNativeDataPath.resolve(
                                    "dependencies/msys2-mingw-w64-i686-clang-llvm-lld-compiler_rt-8.0.1/bin")
                            64 -> kotlinNativeDataPath.resolve(
                                    "dependencies/msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1/bin")
                            else -> throw GradleException("Unsupported bitness")
                        }.toString()
                        inputs.file(inFile)
                        outputs.file(outFile)
                        commandLine("$llvmDir/windres", inFile, "-O", "coff", "-o", outFile)
                        environment("PATH", "$llvmDir;${System.getenv("PATH")}")
                        dependsOn(compilation.compileKotlinTask)
                    }
                    linkTask.dependsOn(windresTask)
                    linkerOpts(outFile.toString())
                }

                when (preset) {
                    presets["macosX64"] -> linkerOpts("-L/opt/local/lib", "-L/usr/local/lib", "-lSDL2")
                    presets["linuxX64"] -> linkerOpts("-L/usr/lib64", "-L/usr/lib/x86_64-linux-gnu", "-lSDL2")
                    presets["mingwX64"], presets["mingwX86"] -> linkerOpts(
                        "-L${mingwPath.resolve("lib")}",
                        "-Wl,-Bstatic",
                        "-lstdc++",
                        "-static",
                        "-lSDL2",
                        "-limm32",
                        "-lole32",
                        "-loleaut32",
                        "-lversion",
                        "-lwinmm",
                        "-lsetupapi",
                        "-mwindows"
                    )
                    presets["linuxArm32Hfp"] -> linkerOpts("-lSDL2")
                }
                runTask?.workingDir(project.provider {
                    val tetris: KotlinNativeTarget by kotlin.targets
                    tetris.binaries.getExecutable(buildType).outputDirectory
                })
            }
        }
        
        compilations["main"].cinterops {
            val sdl by creating {
                when (preset) {
                    presets["macosX64"] -> includeDirs("/opt/local/include/SDL2", "/usr/local/include/SDL2")
                    presets["linuxX64"] -> includeDirs("/usr/include/SDL2")
                    presets["mingwX64"], presets["mingwX86"] -> includeDirs(mingwPath.resolve("include/SDL2"))
                    presets["linuxArm32Hfp"] -> includeDirs(kotlinNativeDataPath.resolve("dependencies/target-sysroot-2-raspberrypi/usr/include/SDL2"))
                }
            }
        }

        compilations["main"].enableEndorsedLibs = true
    }
}

afterEvaluate {
    val tetris: KotlinNativeTarget by kotlin.targets
    val linkTasks = NativeBuildType.values().mapNotNull { tetris.binaries.getExecutable(it).linkTask }

    linkTasks.forEach { linkTask ->
        linkTask.doLast {
            copy {
                from(kotlin.sourceSets["tetrisMain"].resources)
                into(linkTask.outputFile.get().parentFile)
                exclude("*.rc")
            }
        }
    }
}

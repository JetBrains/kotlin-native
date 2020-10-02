package org.jetbrains.kotlin.native.test.external

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class KonanExternalCompiler(private val enableKonanAssertions: Boolean,
                            private val twoStageCompilation: Boolean) {
    private fun runCompiler(filesToCompile: List<String>, output: String, moreArgs: List<String>) {
        println("Compilation args: files=$filesToCompile  output=$output args=$moreArgs")
        val sources = filesToCompile.writeToArgFile("${output}_argfile")
        val args = mutableListOf("-output", output,
                "@${sources.absolutePath}")
        args.addAll(moreArgs)
        if (enableKonanAssertions) {
            args += "-ea"
        }
        val platformManager = PlatformManager(KonanHomeProvider.determineKonanHome())
        val targetProp = System.getProperty("test.target")
        val testTarget = if (targetProp.isNotEmpty())
            platformManager.targetByName(System.getProperty("test.target"))
        else HostManager.host
        compileKotlinNative(args.toList(), Paths.get(output), testTarget)
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

    fun compileKotlinNative(arguments: List<String>, output: Path, target: KonanTarget? = HostManager.host) {
        val dist = KonanHomeProvider.determineKonanHome()
        val konancDriver = if (HostManager.hostIsMingw) "konanc.bat" else "konanc"
        val konanc = File("$dist/bin/$konancDriver")

        output.toFile().parentFile.mkdirs()

        val args = mutableListOf("-output", output.toString()).apply {
            add("-target")
            add(target?.name ?: HostManager.host.name)
            addAll(arguments)
        }

        // run konanc compiler locally
        val process = subprocess(konanc.toPath(), *args.toTypedArray())
        process.let {
            File("$output.compilation.log").run {
                writeText(it.stdout)
                writeText(it.stderr)
            }
            if (it.process.exitValue() != 0) {
                System.err.println(File("$output.compilation.log").readText())
                throw RuntimeException("Kotlin/Native compiler failed with exit code ${it.process.exitValue()}")
            }
        }
    }

    /**
     * Writes the list of file paths to a temporary argument file to be compiled.
     */
    fun List<String>.writeToArgFile(name: String): File =
            File.createTempFile(name, ".lst").also {
                it.deleteOnExit()
                it.writeText(this.joinToString("\n") { s ->
                    if (s.toCharArray().any { c -> Character.isWhitespace(c) }) {
                        "\"${s.replace("\\", "\\\\")}\"" // escape file name
                    } else s
                })
            }
}
// FIXME: copy from debugger tests

data class ProcessOutput(
        val program: Path,
        val process: Process,
        val stdout: String,
        val stderr: String,
        val durationMs: Long
) {
    fun thrownIfFailed(): ProcessOutput {
        fun renderStdStream(name: String, text: String): String =
                if (text.isBlank()) "$name is empty" else "$name:\n$text"

        check(process.exitValue() == 0) {
            """$program exited with non-zero value: ${process.exitValue()}
              |${renderStdStream("stdout", stdout)}
              |${renderStdStream("stderr", stderr)}
            """.trimMargin()
        }
        return this
    }
}

fun subprocess(program: Path, vararg args: String): ProcessOutput {
    val start = System.currentTimeMillis()
    val process = ProcessBuilder(program.toString(), *args).start()
    val out = GlobalScope.async(Dispatchers.IO) {
        readStream(process, process.inputStream.buffered())
    }

    val err = GlobalScope.async(Dispatchers.IO) {
        readStream(process, process.errorStream.buffered())
    }

    return runBlocking {
        try {
            val status = process.waitFor(5L, TimeUnit.MINUTES)
            if (!status) {
                out.cancel()
                err.cancel()
                error("$program timeouted")
            }
        }catch (e:Exception) {
            out.cancel()
            err.cancel()
            error(e)
        }
        ProcessOutput(program, process, out.await(), err.await(), System.currentTimeMillis() - start)
    }
}

private fun readStream(process: Process, stream: InputStream): String {
    var size = 4096
    val buffer = ByteArray(size) { 0 }
    val sunk = ByteArrayOutputStream()
    while (true) {
        size = stream.read(buffer, 0, buffer.size)
        if (size < 0 && !process.isAlive)
            break
        if (size > 0)
            sunk.write(buffer, 0, size)
    }
    return String(sunk.toByteArray())
}
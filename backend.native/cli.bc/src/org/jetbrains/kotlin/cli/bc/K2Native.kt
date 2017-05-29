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

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.CompilerOutputKind.*
import org.jetbrains.kotlin.backend.konan.util.profile
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import java.util.*
import kotlin.reflect.KFunction

// TODO: Don't use reflection?
private fun maybeExecuteHelper(configuration: CompilerConfiguration) {
    try {
        val kClass = Class.forName("org.jetbrains.kotlin.konan.Helper0").kotlin
        val ctor = kClass.constructors.single() as KFunction<Runnable>
        val distribution = Distribution(configuration)
        val result = ctor.call(
                distribution.dependenciesDir,
                distribution.properties.properties,
                distribution.dependencies
        )
        result.run()
    } catch (notFound: ClassNotFoundException) {
        // Just ignore, no helper.
    } catch (e: Throwable) {
        throw IllegalStateException("Cannot download dependencies.", e)
    }
}

private fun suffixIfNot(name: String, suffix: String) =
        if (name.endsWith(suffix)) name else "$name$suffix"

class K2Native : CLICompiler<K2NativeCompilerArguments>() {

    override fun doExecute(arguments : K2NativeCompilerArguments,
                           configuration : CompilerConfiguration,
                           rootDisposable: Disposable
                          ): ExitCode {

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
            configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)
        val project = environment.project
        val konanConfig = KonanConfig(project, configuration)

        try {
            runTopLevelPhases(konanConfig, environment)
        } catch (e: KonanCompilationException) {
            return ExitCode.COMPILATION_ERROR
        }
        // TODO: catch Errors and IllegalStateException.

        return ExitCode.OK
    }

    fun Array<String>?.toNonNullList(): List<String> {
        return this?.asList<String>() ?: listOf<String>()
    }


    // It is executed before doExecute().
    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration,
            arguments    : K2NativeCompilerArguments,
            services     : Services) {

        configuration.addKotlinSourceRoots(arguments.freeArgs)

        with(KonanConfigKeys) {
            with(configuration) {

                put(NOSTDLIB, arguments.nostdlib)
                put(NOPACK, arguments.nopack)
                put(NOMAIN, arguments.nomain)
                put(LIBRARY_FILES,
                        arguments.libraries.toNonNullList())

                put(LINKER_ARGS, arguments.linkerArguments.toNonNullList())
                if (arguments.target != null)
                    put(TARGET, arguments.target)

                put(NATIVE_LIBRARY_FILES,
                        arguments.nativeLibraries.toNonNullList())
                put(REPOSITORIES,
                        arguments.repositories.toNonNullList())

                // TODO: Collect all the explicit file names into an object
                // and teach the compiler to work with temporaries and -save-temps.

                val outputKind = CompilerOutputKind.valueOf(
                    (arguments.produce ?: "program").toUpperCase())

                put(PRODUCE, outputKind)
                val (defaultName, suffix) = when (outputKind) {
                    CompilerOutputKind.LIBRARY -> {
                        put(NOLINK, true)
                        Pair("library", ".klib")
                    }
                    CompilerOutputKind.PROGRAM -> {
                        put(NOLINK, false)
                        Pair("program", ".kexe")
                    }
                    CompilerOutputKind.BITCODE -> {
                        put(NOLINK, true)
                        Pair("output", ".bc")
                    }
                }
                val output = arguments.outputFile ?: defaultName
                put(OUTPUT_NAME, output)
                put(OUTPUT_FILE, suffixIfNot(output, suffix))

                // This is a decision we could change
                put(CommonConfigurationKeys.MODULE_NAME, output)
                put(ABI_VERSION, 1)

                if (arguments.runtimeFile != null)
                    put(RUNTIME_FILE, arguments.runtimeFile)
                if (arguments.propertyFile != null)
                    put(PROPERTY_FILE, arguments.propertyFile)
                put(LIST_TARGETS, arguments.listTargets)
                put(OPTIMIZATION, arguments.optimization)
                put(DEBUG, arguments.debug)

                put(PRINT_IR, arguments.printIr)
                put(PRINT_IR_WITH_DESCRIPTORS, arguments.printIrWithDescriptors)
                put(PRINT_DESCRIPTORS, arguments.printDescriptors)
                put(PRINT_LOCATIONS, arguments.printLocations)
                put(PRINT_BITCODE, arguments.printBitCode)

                put(VERIFY_IR, arguments.verifyIr)
                put(VERIFY_DESCRIPTORS, arguments.verifyDescriptors)
                put(VERIFY_BITCODE, arguments.verifyBitCode)

                put(ENABLED_PHASES,
                        arguments.enablePhases.toNonNullList())
                put(DISABLED_PHASES,
                        arguments.disablePhases.toNonNullList())
                put(VERBOSE_PHASES,
                        arguments.verbosePhases.toNonNullList())
                put(LIST_PHASES, arguments.listPhases)
                put(TIME_PHASES, arguments.timePhases)

                put(ENABLE_ASSERTIONS, arguments.enableAssertions)
            }
        }

        maybeExecuteHelper(configuration)
    }

    override fun createArguments(): K2NativeCompilerArguments {
        return K2NativeCompilerArguments().apply { coroutinesState = "enable" }
    }

    override fun executableScriptFileName() = "kotlinc-native"

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            profile("Total compiler main()") {
                CLICompiler.doMain(K2Native(), args)
            }
        }
    }
}
fun main(args: Array<String>) = K2Native.main(args)


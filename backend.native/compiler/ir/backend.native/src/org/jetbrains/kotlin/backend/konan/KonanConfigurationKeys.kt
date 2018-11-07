/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

class KonanConfigKeys {
    companion object {
        // Keep the list lexically sorted.
        val CHECK_DEPENDENCIES: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("check dependencies and download the missing ones")
        val COMPATIBLE_COMPILER_VERSIONS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("compatible compiler versions")
        val DEBUG: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("add debug information")
        val DISABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("disable backend phases")
        val BITCODE_EMBEDDING_MODE: CompilerConfigurationKey<BitcodeEmbedding.Mode>
                = CompilerConfigurationKey.create("bitcode embedding mode")
        val ENABLE_ASSERTIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("enable runtime assertions in generated code")
        val ENABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("enable backend phases")
        val ENTRY: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("fully qualified main() name")
        val EXPORTED_LIBRARIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("libraries included into produced framework API")
        val FRIEND_MODULES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("friend module paths")
        val GENERATE_TEST_RUNNER: CompilerConfigurationKey<TestRunnerKind>
                = CompilerConfigurationKey.create("generate test runner") 
        val INCLUDED_BINARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("included binary file paths")
        val LIBRARY_FILES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("library file paths")
        val LIBRARY_VERSION: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("library version")
        val LINKER_ARGS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("additional linker arguments")
        val LIST_PHASES: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("list backend phases")
        val LIST_TARGETS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("list available targets")
        val MANIFEST_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("provide manifest addend file")
        val META_INFO: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate metadata")
        val MODULE_KIND: CompilerConfigurationKey<ModuleKind> 
                = CompilerConfigurationKey.create("module kind")
        val MODULE_NAME: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("module name")
        val NATIVE_LIBRARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("native library file paths")
        val NODEFAULTLIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("don't link with the default libraries")
        val NOMAIN: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("assume 'main' entry point to be provided by external libraries")
        val NOSTDLIB: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't link with stdlib")
        val NOPACK: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't the library into a klib file")
        val OPTIMIZATION: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("optimized compilation")
        val OUTPUT: CompilerConfigurationKey<String> 
                = CompilerConfigurationKey.create("program or library name")
        val PRINT_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print bitcode")
        val PRINT_DESCRIPTORS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print descriptors")
        val PRINT_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print ir")
        val PRINT_IR_WITH_DESCRIPTORS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print ir with descriptors")
        val PRINT_LOCATIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print locations")
        val PRODUCE: CompilerConfigurationKey<CompilerOutputKind>
                = CompilerConfigurationKey.create("compiler output kind")
        val PURGE_USER_LIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("purge user-specified libs too")
        val REPOSITORIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("library search path repositories")
        val RUNTIME_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("override default runtime file path")
        val SOURCE_MAP: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate source map")
        val TARGET: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("target we compile for")
        val TEMPORARY_FILES_DIR: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("directory for temporary files")
        val TIME_PHASES: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("time backend phases")
        val VERIFY_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify bitcode")
        val VERIFY_DESCRIPTORS: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify descriptors")
        val VERIFY_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify ir")
        val VERBOSE_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("verbose backend phases")
        val DEBUG_INFO_VERSION: CompilerConfigurationKey<Int>
                = CompilerConfigurationKey.create("debug info format version")

    }
}


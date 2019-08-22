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

package org.jetbrains.kotlin.native.interop.tool

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.*

const val HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX = "headerFilterAdditionalSearchPrefix"
const val NODEFAULTLIBS = "nodefaultlibs"
const val NOENDORSEDLIBS = "noendorsedlibs"
const val PURGE_USER_LIBS = "Xpurge-user-libs"
const val TEMP_DIR = "Xtemporary-files-dir"

// TODO: unify camel and snake cases.
// Possible solution is to accept both cases
open class CommonInteropArguments(val argParser: ArgParser) {
    val verbose by argParser.option(ArgType.Boolean, description = "Enable verbose logging output", defaultValue = false)
    val flavor by argParser.option(ArgType.Choice(listOf("jvm", "native", "wasm")), description = "Interop target",
            defaultValue = "jvm")
    val pkg by argParser.option(ArgType.String, description = "place generated bindings to the package")
    val output by argParser.option(ArgType.String, shortName = "o", description = "specifies the resulting library file",
            defaultValue = "nativelib")
    val libraryPath by argParser.options(ArgType.String,  description = "add a library search path",
            multiple = true, delimiter = ",")
    val staticLibrary by argParser.options(ArgType.String, description = "embed static library to the result",
            multiple = true, delimiter = ",")
    val generated by argParser.option(ArgType.String, description = "place generated bindings to the directory",
            defaultValue = System.getProperty("user.dir"))
    val natives by argParser.option(ArgType.String, description = "where to put the built native files",
            defaultValue = System.getProperty("user.dir"))
    val library by argParser.options(ArgType.String, shortName = "l", description = "library to use for building",
            multiple = true)
    val repo by argParser.options(ArgType.String, shortName = "r", description = "repository to resolve dependencies",
            multiple = true)
    val nodefaultlibs by argParser.option(ArgType.Boolean, NODEFAULTLIBS,
            description = "don't link the libraries from dist/klib automatically",
            defaultValue = false)
    val noendorsedlibs by argParser.option(ArgType.Boolean, NOENDORSEDLIBS,
            description = "don't link the endorsed libraries from dist automatically",
            defaultValue = false)
    val purgeUserLibs by argParser.option(ArgType.Boolean, PURGE_USER_LIBS,
            description = "don't link unused libraries even explicitly specified", defaultValue = false)
    val tempDir by argParser.option(ArgType.String, TEMP_DIR,
            description = "save temporary files to the given directory")
}

class CInteropArguments(argParser: ArgParser =
                                ArgParser("cinterop", useDefaultHelpShortName = false,
                                        prefixStyle = ArgParser.OPTION_PREFIX_STYLE.JVM)): CommonInteropArguments(argParser) {
    val target by argParser.option(ArgType.String, description = "native target to compile to", defaultValue = "host")
    val def by argParser.option(ArgType.String, description = "the library definition file")
    val header by argParser.options(ArgType.String, description = "header file to produce kotlin bindings for",
            multiple = true, delimiter = ",")
    val shortHeaderForm by argParser.options(ArgType.String, "h", description = "header file to produce kotlin bindings for",
            multiple = true, delimiter = ",", deprecatedWarning = "Option -h is deprecated. Please use -header.")
    val headerFilterPrefix by argParser.options(ArgType.String, HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX, "hfasp",
            "header file to produce kotlin bindings for", multiple = true, delimiter = ",")
    val compilerOpts by argParser.options(ArgType.String,
            description = "additional compiler options (allows to add several options separated by spaces)",
            multiple = true, delimiter = " ")
    val compilerOptions by argParser.options(ArgType.String, "compiler-options",
            description = "additional compiler options (allows to add several options separated by spaces)",
            multiple = true, delimiter = " ")
    val linkerOpts by argParser.options(ArgType.String, "linkerOpts",
            description = "additional linker options (allows to add several options separated by spaces)",
            multiple = true, delimiter = " ")
    val linkerOptions by argParser.options(ArgType.String, "linker-options",
            description = "additional linker options (allows to add several options separated by spaces)",
            multiple = true, delimiter = " ")
    val compilerOption by argParser.options(ArgType.String, "compiler-option",
            description = "additional compiler option", multiple = true)
    val linkerOption by argParser.options(ArgType.String, "linker-option",
            description = "additional linker option", multiple = true)
    val copt by argParser.options(ArgType.String,
            description = "additional compiler options (allows to add several options separated by spaces)",
            multiple = true, delimiter = " ",
            deprecatedWarning = "Option -copt is deprecated. Please use -compiler-options.")
    val lopt by argParser.options(ArgType.String,
            description = "additional linker options (allows to add several options separated by spaces)",
            multiple = true, delimiter = " ",
            deprecatedWarning = "Option -lopt is deprecated. Please use -linker-options.")
    val linker by argParser.option(ArgType.String, description = "use specified linker")
}

class JSInteropArguments(argParser: ArgParser = ArgParser("jsinterop", useDefaultHelpShortName = false,
        prefixStyle = ArgParser.OPTION_PREFIX_STYLE.JVM)): CommonInteropArguments(argParser) {
    val target by argParser.option(ArgType.Choice(listOf("wasm32")),
            description = "wasm target to compile to", defaultValue = "wasm32")
}

internal fun warn(msg: String) {
    println("warning: $msg")
}

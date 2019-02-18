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

import org.jetbrains.kliopt.*

// TODO: unify camel and snake cases.
// Possible solution is to accept both cases
fun getCommonInteropArguments() = listOf(
        OptionDescriptor(ArgType.Boolean(), "verbose", description = "Enable verbose logging output", defaultValue = "false"),
        OptionDescriptor(ArgType.Choice(listOf("jvm", "native", "wasm")),
                "flavor", description = "Interop target", defaultValue = "jvm"),
        OptionDescriptor(ArgType.String(), "pkg", description = "place generated bindings to the package"),
        OptionDescriptor(ArgType.String(), "generated", description = "place generated bindings to the directory",
                defaultValue = System.getProperty("user.dir")),
        OptionDescriptor(ArgType.String(), "libraryPath", description = "add a library search path",
                isMultiple = true, delimiter = ","),
        OptionDescriptor(ArgType.String(), "manifest", description = "library manifest addend"),
        OptionDescriptor(ArgType.String(), "natives", description = "where to put the built native files",
                defaultValue = System.getProperty("user.dir")),
        OptionDescriptor(ArgType.String(), "staticLibrary", description = "embed static library to the result",
                isMultiple = true, delimiter = ","),
        OptionDescriptor(ArgType.String(), "temporaryFilesDir", description = "save temporary files to the given directory")
    )

fun getCInteropArguments(): List<OptionDescriptor> {
    val options = listOf(
            OptionDescriptor(ArgType.String(), "import", description = "a semicolon separated list of headers, prepended with the package name",
                    isMultiple = true, delimiter = ","),
            OptionDescriptor(ArgType.String(), "target", description = "native target to compile to"),
            OptionDescriptor(ArgType.String(), "def", description = "the library definition file"),
            OptionDescriptor(ArgType.String(), "header", "hd", "header file to produce kotlin bindings for",
                    isMultiple = true, delimiter = ","),
            OptionDescriptor(ArgType.String(), HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX, "hfasp",
                    "header file to produce kotlin bindings for", isMultiple = true, delimiter = ","),
            OptionDescriptor(ArgType.String(), "compilerOpts", "copt",
                    "additional compiler options", isMultiple = true, delimiter = " "),
            OptionDescriptor(ArgType.String(), "linkerOpts", "lopt",
                    "additional linker options", isMultiple = true, delimiter = " "),
            OptionDescriptor(ArgType.Boolean(), "shims", description = "wrap bindings by a tracing layer", defaultValue = "false"),
            OptionDescriptor(ArgType.String(), "linker", description = "use specified linker"),
            OptionDescriptor(ArgType.String(), "cstubsname", description = "provide a name for the generated c stubs file")
    )
    return (options + getCommonInteropArguments())
}

const val HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX = "headerFilterAdditionalSearchPrefix"

internal fun warn(msg: String) {
    println("warning: $msg")
}

fun ArgParser.getValuesAsArray(propertyName: String) =
        (getAll<String>(propertyName) ?: listOf<String>()).toTypedArray()

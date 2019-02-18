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

package org.jetbrains.kotlin.konan.util

import kotlin.system.measureTimeMillis
import org.jetbrains.kotlin.konan.file.*
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this function:
fun <T> printMillisec(message: String, body: () -> T): T {
    var result: T? = null
    val msec = measureTimeMillis{
        result = body()
    }
    println("$message: $msec msec")
    return result!!
}

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this function:
fun profile(message: String, body: () -> Unit) = profileIf(
    System.getProperty("konan.profile")?.equals("true") ?: false,
    message, body)

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this function:
fun profileIf(condition: Boolean, message: String, body: () -> Unit) =
    if (condition) printMillisec(message, body) else body()

fun nTabs(amount: Int): String {
    return String.format("%1$-${(amount+1)*4}s", "") 
}

fun String.prefixIfNot(prefix: String) =
    if (this.startsWith(prefix)) this else "$prefix$this"

fun String.prefixBaseNameIfNot(prefix: String): String {
    val file = File(this).absoluteFile
    val name = file.name
    val directory = file.parent
    return "$directory/${name.prefixIfNot(prefix)}"
}

fun String.suffixIfNot(suffix: String) =
    if (this.endsWith(suffix)) this else "$this$suffix"

fun String.removeSuffixIfPresent(suffix: String) =
    if (this.endsWith(suffix)) this.dropLast(suffix.length) else this

fun <T> Lazy<T>.getValueOrNull(): T? = if (isInitialized()) value else null



/// we use the fake delimiter to support the feature
/// of the [@Argument] parser to merge several arguments
/// into the same argument (it works only for [Array<String>]
/// type, but we do not need the separator
///
/// the right fix is to update the [@Argument] to support paths separation
const val ARGUMENT_NO_DELIMITER = "\\n\\t\\t\\n\\t\\t\\n\\ue000\\ue001\\ue002\\n\\t\\t\\t\\t\\n"

fun parseCommandLineString(value: () -> Array<String>) = object: ReadOnlyProperty<Any, Array<String>> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Array<String> {
        return value().flatMap { parseCommandLineString(it) }.toTypedArray()
    }
}

fun escapeToCommandLineString(args: Iterable<String>) : String {
    return args.joinToString(separator = " ", transform = {
        when {
            it.startsWith("\'") && it.endsWith("\'") -> it
            it.startsWith("\"") && it.endsWith("\"") -> it
            it.contains(" ") -> "\"$it\""
            it.isEmpty() -> "\"\""
            //TODO: possible incorrect case for "a b c\" (slash escapes ")
            else -> it
        }
    })
}

fun parseCommandLineString(cmdString: String): List<String> {
    //inspired by IntelliJ IDEA source code (Apache 2.0 by JetBrains)
    //com.intellij.openapi.util.text.StringUtilRt#splitHonorQuotes

    val result = mutableListOf<String>()
    val builder = StringBuilder(cmdString.length)

    var inQuotes = false
    for (i in 0 until cmdString.length) {
        val c = cmdString[i]
        if (c == ' ' && !inQuotes) {
            if (builder.isNotEmpty()) {
                result.add(builder.toString())
                builder.setLength(0)
            }
            continue
        }
        if ((c == '"' || c == '\'') && !(i > 0 && cmdString[i - 1] == '\\')) {
            inQuotes = !inQuotes
            continue
        }

        builder.append(c)
    }

    if (builder.isNotEmpty()) {
        result.add(builder.toString())
    }

    return result
}

fun Properties.getParsedCommandLineString(name: String): List<String> {
    val s = getProperty(name) ?: return listOf()
    return parseCommandLineString(s)
}

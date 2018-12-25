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

package org.jetbrains.benchmarksAnalyzer

import platform.posix.*
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.*

actual fun readFile(fileName: String): String {
    val file = fopen(fileName, "r") ?: error("Cannot write file '$fileName'")
    var str = ByteArray(1024)
    var text = StringBuilder()
    try {
        while (true) {
            val nextLine = fgets(str.refTo(0), str.size, file)?.toKString()
            if (nextLine == null) break
            text.append(nextLine)
        }
    } finally {
        fclose(file)
    }
    return text.toString()
}

actual fun format(number: Double, decimalNumber: Int): String {
    var str = ByteArray(1024)
    snprintf(str.refTo(0), str.size.toULong(), "%.${decimalNumber}f", number)
    return str.stringFromUtf8()
}

actual fun writeToFile(fileName: String, text: String) {
    val file = fopen(fileName, "wt") ?: error("Cannot write file '$fileName'")
    try {
        if (fputs(text, file) == EOF) throw Error("File write error")
    } finally {
        fclose(file)
    }
}

actual fun exitProcess(status: Int) {
    kotlin.system.exitProcess(status)
}

actual fun assert(value: Boolean, lazyMessage: () -> Any) {
    kotlin.assert(value, lazyMessage)
}

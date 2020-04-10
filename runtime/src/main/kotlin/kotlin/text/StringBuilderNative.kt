/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
fun StringBuilder.appendLine(value: Byte): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
fun StringBuilder.appendLine(value: Short): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
fun StringBuilder.appendLine(value: Int): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
fun StringBuilder.appendLine(value: Long): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
fun StringBuilder.appendLine(value: Float): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
fun StringBuilder.appendLine(value: Double): StringBuilder = append(value).appendLine()


@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: String) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Boolean) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Byte) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Short) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Int) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Long) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Float) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Double) = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine"), level = DeprecationLevel.WARNING)
fun StringBuilder.appendln(it: Any?) = appendLine(it)

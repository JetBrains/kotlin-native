/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.io

/** Prints the given [message] to the standard output stream. */
@SymbolName("Kotlin_io_Console_print")
external public fun print(message: String)

/* TODO: use something like that.
public fun<T> print(message: T) {
    print(message.toString())
} */

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Byte) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Short) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Char) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Int) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Long) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Float) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Double) {
    print(message.toString())
}

/** Prints the given [message] to the standard output stream. */
public fun print(message: Boolean) {
    print(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
@SymbolName("Kotlin_io_Console_println")
external public fun println(message: String)

/** Prints the given [message] and newline to the standard output stream. */
public actual fun println(message: Any?) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Byte) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Short) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Char) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Int) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Long) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Float) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Double) {
    println(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
public fun println(message: Boolean) {
    println(message.toString())
}
/* TODO: use something like that.
public fun<T> println(message: T) {
    print(message.toString())
} */

/** Prints newline to the standard output stream. */
@SymbolName("Kotlin_io_Console_println0")
external public actual fun println()

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or `null` if the input stream is redirected to a file and the end of file has been reached.
 */
@SymbolName("Kotlin_io_Console_readLine")
external public fun readLine(): String?

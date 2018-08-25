/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.io

@SymbolName("Kotlin_io_Console_print")
external public fun print(message: String)

/* TODO: use something like that.
public fun<T> print(message: T) {
    print(message.toString())
} */

public actual fun print(message: Any?) {
    print(message.toString())
}

public fun print(message: Byte) {
    print(message.toString())
}

public fun print(message: Short) {
    print(message.toString())
}

public fun print(message: Char) {
    print(message.toString())
}

public fun print(message: Int) {
    print(message.toString())
}

public fun print(message: Long) {
    print(message.toString())
}

public fun print(message: Float) {
    print(message.toString())
}

public fun print(message: Double) {
    print(message.toString())
}

public fun print(message: Boolean) {
    print(message.toString())
}

@SymbolName("Kotlin_io_Console_println")
external public fun println(message: String)

public actual fun println(message: Any?) {
    println(message.toString())
}

public fun println(message: Byte) {
    println(message.toString())
}

public fun println(message: Short) {
    println(message.toString())
}

public fun println(message: Char) {
    println(message.toString())
}

public fun println(message: Int) {
    println(message.toString())
}

public fun println(message: Long) {
    println(message.toString())
}

public fun println(message: Float) {
    println(message.toString())
}

public fun println(message: Double) {
    println(message.toString())
}

public fun println(message: Boolean) {
    println(message.toString())
}
/* TODO: use something like that.
public fun<T> println(message: T) {
    print(message.toString())
} */

@SymbolName("Kotlin_io_Console_println0")
external public actual fun println()

@SymbolName("Kotlin_io_Console_readLine")
external public fun readLine(): String?

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import platform.Foundation.*
import kotlin.test.*

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.system.exitProcess

fun sigabrt_handler(signum: Int) : Unit {
    println("Exception handled successfully, signum = $signum")
    exitProcess(0)
}

class A : NSJSONSerialization()

fun main() {
    signal(SIGABRT, staticCFunction(::sigabrt_handler))

    // The following should fail with exception; if exception handled we'll trap SIGABRT and exit gracefully.
    // Otherwise, unhandled exception cause Segmentation fault with exit code 139
    println(A())
}

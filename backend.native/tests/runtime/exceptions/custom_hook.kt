/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

fun main(args : Array<String>) {
    val x = 42
    val old = setUnhandledExceptionHook({ throwable -> println("value $x: ${throwable::class.simpleName}")})
    assertNull(old)
    throw Error("an error")
}
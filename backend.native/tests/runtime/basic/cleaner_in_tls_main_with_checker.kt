/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.*
import kotlin.native.Platform

@ThreadLocal
var tlsCleaner: Cleaner? = null

fun main() {
    Platform.isCleanersLeakCheckerActive = true
    // This cleaner won't be run
    tlsCleaner = createCleaner(42) {
        println(it)
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

// Must match GCComponentLog in Runtime.h
enum class GCComponentLog(val value: Int) {
    SAFE_POINT(1)
}

val List<GCComponentLog>.value: Int
    get() = fold(0) { acc, component ->
        acc or component.value
    }

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

// Implementation of functions that are absent on JS backend.
inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (!value) error(lazyMessage())
}

fun Double.format(value: Int = 4) = toString()
/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen

@Frozen
public class SharedRef<T> private constructor(private val value: T) {
    init {
        // SharedRef can be shared across threads.
        share()
    }

    companion object {
        fun <T> create(value: T) = SharedRef<T>(value)
    }

    private val createdOn = Worker.current.id

    fun get(): T {
        if (createdOn != Worker.current.id) {
            // TODO: Better message.
            throw IncorrectDereferenceException("illegal attempt to access non-shared <object> from other thread")
        }
        return value
    }
}

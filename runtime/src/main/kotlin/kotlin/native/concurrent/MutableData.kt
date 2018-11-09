/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*

@SymbolName("Kotlin_Any_share")
external internal fun Any.share()

@Frozen
/**
 * Class that could be used to store mutable data in concurrent scenarious.
 */
public class MutableData {
    private var data = ByteArray(16).apply { share() }
    private var size = 0
    private val lock = Lock()

    private fun resizeDataLocked(newSize: Int) {
        if (newSize > data.size) {
            val newData = ByteArray(newSize)
            data.copyRangeTo(newData, 0, size, 0)
            data.share()
            data = newData
        }
    }

    fun append(data: MutableData) = locked(lock) {
    }

    fun append(data: ByteArray) = locked(lock) {
    }
}
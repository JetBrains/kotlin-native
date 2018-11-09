/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*
import kotlinx.cinterop.*

@SymbolName("Kotlin_Any_share")
external private fun Any.share()

@SymbolName("Kotlin_CPointer_CopyMemory")
external private fun CopyMemory(to: COpaquePointer?, from: COpaquePointer?, count: Int)

/**
 * Mutable concurrently accessible data buffer. Could be accessed from several workers simulteniously.
 */
@Frozen
public class MutableData {
    init {
        // Instance of MutableData is shared.
        share()
    }

    private var buffer = ByteArray(16).apply { share() }
    private var size_ = 0
    private val lock = Lock()

    private fun resizeDataLocked(newSize: Int): Int {
        if (newSize > buffer.size) {
            val actualSize = maxOf(buffer.size * 3 / 2 + 1, newSize)
            val newBuffer = ByteArray(actualSize)
            buffer.copyRangeTo(newBuffer, 0, size, 0)
            newBuffer.share()
            buffer = newBuffer
        }
        val position = size
        size_ = newSize
        return position
    }

    /**
     * Current data size, may concurrently change later on.
     */
    public val size: Int
        get() = size_

    /**
     * Reset the data buffer, makings its size 0.
     */
    public fun reset() = locked(lock) {
        size_ = 0
    }

    /**
     * Appends data to the buffer.
     */
    public fun append(data: MutableData) = locked(lock) {
        val toCopy = data.size
        val where = resizeDataLocked(size + toCopy)
        data.copyRangeTo(buffer, 0, size, where)
    }

    /**
     * Appends byte array to the buffer.
     */
    public fun append(data: ByteArray, start: Int = 0, count: Int = data.size - start): Unit = locked(lock) {
        val where = resizeDataLocked(this.size + count)
        data.copyRangeTo(buffer, start, count, where)
    }

    /**
     * Appends C data to the buffer.
     */
    public fun append(data: COpaquePointer?, count: Int): Unit = locked(lock) {
        if (data == null || count == 0) return
        val where = resizeDataLocked(this.size + count)
        var index = 0
        val pointer = data.reinterpret<ByteVar>()
        buffer.usePinned {
            it -> CopyMemory(it.addressOf(where), pointer, count)
        }
    }

    /**
     * Copies range of mutable data to the byte array.
     */
    public fun copyRangeTo(output: ByteArray, fromIndex: Int, toIndex: Int, destinationIndex: Int): Unit = locked(lock) {
        buffer.copyRangeTo(output, fromIndex, toIndex, destinationIndex)
    }

    /**
     * Get a byte from the mutable data.
     *
     * @Throws IndexOutOfBoundsException if index is beyound range.
     */
    public operator fun <R> get(index: Int): Byte = locked(lock) {
        // index < 0 is checked below by array access.
        if (index > size)
            throw IndexOutOfBoundsException()
        buffer[index]
    }

    /**
     * Executes provided block under lock with raw pointer to the data stored in the buffer.
     * Block is executed under the spinlock, and must be short.
     */
    public fun <R> asPointerLocked(block: (COpaquePointer, dataSize: Int) -> R) = locked(lock) {
        buffer.usePinned {
            it -> block(it.addressOf(0), size)
        }
    }

    /**
     * Executes provided block under lock with the raw data buffer.
     * Block is executed under the spinlock, and must be short.
     */
    public fun <R> asBufferLocked(block: (array: ByteArray, dataSize: Int) -> R) = locked(lock) {
        block(buffer, size)
    }
}
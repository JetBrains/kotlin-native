/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

@Intrinsic external fun getNativeNullPtr(): NativePtr

class NativePtr @PublishedApi internal constructor(private val value: NonNullNativePtr?) {
    companion object {
        val NULL = getNativeNullPtr()
    }

    @Intrinsic external operator fun plus(offset: Long): NativePtr

    @Intrinsic external fun toLong(): Long

    override fun equals(other: Any?) = (other is NativePtr) && kotlin.native.internal.areEqualByValue(this, other)

    override fun hashCode() = this.toLong().hashCode()

    override fun toString() = "0x${this.toLong().toString(16)}"
}

@PublishedApi
internal inline class NonNullNativePtr(val value: NotNullPointerValue) { // TODO: refactor to use this type widely.
    @Suppress("NOTHING_TO_INLINE")
    inline fun toNativePtr() = NativePtr(this)
    // TODO: fixme.
    override fun toString() = ""

    override fun hashCode() = 0

    override fun equals(other: Any?) = false
}

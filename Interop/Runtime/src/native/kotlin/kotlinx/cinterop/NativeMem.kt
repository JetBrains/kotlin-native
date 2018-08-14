/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.cinterop

import kotlin.native.*
import kotlin.native.internal.Intrinsic

@PublishedApi
internal inline val pointerSize: Int
    get() = getPointerSize()

@PublishedApi
@Intrinsic internal external fun getPointerSize(): Int

// TODO: do not use singleton because it leads to init-check on any access.
@PublishedApi
internal object nativeMemUtils {
    @Intrinsic external fun getByte(mem: NativePointed): Byte
    @Intrinsic external fun putByte(mem: NativePointed, value: Byte)

    @Intrinsic external fun getShort(mem: NativePointed): Short
    @Intrinsic external fun putShort(mem: NativePointed, value: Short)

    @Intrinsic external fun getInt(mem: NativePointed): Int
    @Intrinsic external fun putInt(mem: NativePointed, value: Int)

    @Intrinsic external fun getLong(mem: NativePointed): Long
    @Intrinsic external fun putLong(mem: NativePointed, value: Long)

    @Intrinsic external fun getFloat(mem: NativePointed): Float
    @Intrinsic external fun putFloat(mem: NativePointed, value: Float)

    @Intrinsic external fun getDouble(mem: NativePointed): Double
    @Intrinsic external fun putDouble(mem: NativePointed, value: Double)

    @Intrinsic external fun getNativePtr(mem: NativePointed): NativePtr
    @Intrinsic external fun putNativePtr(mem: NativePointed, value: NativePtr)

    // TODO: optimize
    fun getByteArray(source: NativePointed, dest: ByteArray, length: Int) {
        val sourceArray = source.reinterpret<ByteVar>().ptr
        var index = 0
        while (index < length) {
            dest[index] = sourceArray[index]
            ++index
        }
    }

    // TODO: optimize
    fun putByteArray(source: ByteArray, dest: NativePointed, length: Int) {
        val destArray = dest.reinterpret<ByteVar>().ptr
        var index = 0
        while (index < length) {
            destArray[index] = source[index]
            ++index
        }
    }

    // TODO: optimize
    fun getCharArray(source: NativePointed, dest: CharArray, length: Int) {
        val sourceArray = source.reinterpret<ShortVar>().ptr
        var index = 0
        while (index < length) {
            dest[index] = sourceArray[index].toChar()
            ++index
        }
    }

    // TODO: optimize
    fun putCharArray(source: CharArray, dest: NativePointed, length: Int) {
        val destArray = dest.reinterpret<ShortVar>().ptr
        var index = 0
        while (index < length) {
            destArray[index] = source[index].toShort()
            ++index
        }
    }

    // TODO: optimize
    fun zeroMemory(dest: NativePointed, length: Int): Unit {
        val destArray = dest.reinterpret<ByteVar>().ptr
        var index = 0
        while (index < length) {
            destArray[index] = 0
            ++index
        }
    }

    fun alloc(size: Long, align: Int): NativePointed {
        val ptr = malloc(size, align)
        if (ptr == nativeNullPtr) {
            throw OutOfMemoryError("unable to allocate native memory")
        }
        return interpretOpaquePointed(ptr)
    }

    fun free(mem: NativePtr) {
        cfree(mem)
    }
}

fun CPointer<ShortVar>.toKString(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toShort()) {
        ++length
    }
    val bytes = CharArray(length)
    var index = 0
    while (index < length) {
        bytes[index] = nativeBytes[index].toChar()
        ++index
    }
    return String(bytes)
}


@SymbolName("Kotlin_interop_malloc")
private external fun malloc(size: Long, align: Int): NativePtr

@SymbolName("Kotlin_interop_free")
private external fun cfree(ptr: NativePtr)

@Intrinsic external fun readBits(ptr: NativePtr, offset: Long, size: Int, signed: Boolean): Long
@Intrinsic external fun writeBits(ptr: NativePtr, offset: Long, size: Int, value: Long)
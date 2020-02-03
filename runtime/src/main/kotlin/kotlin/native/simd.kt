/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

public final class Vector128 private constructor() {
    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getByteAt(index: Int): Byte

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getIntAt(index: Int): Int

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getLongAt(index: Int): Long

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getFloatAt(index: Int): Float

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getDoubleAt(index: Int): Double

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getUByteAt(index: Int): UByte

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getUIntAt(index: Int): UInt

    @TypedIntrinsic(IntrinsicType.EXTRACT_ELEMENT)
    external public fun getULongAt(index: Int): ULong

    public override fun toString() =
            "(0x${getUIntAt(0).toString(16)}, 0x${getUIntAt(1).toString(16)}, 0x${getUIntAt(2).toString(16)}, 0x${getUIntAt(3).toString(16)})"

    // Not as good for floating types
    public fun equals(other: Vector128): Boolean =
            getLongAt(0) == other.getLongAt(0) && getLongAt(1) == other.getLongAt(1)

    public override fun equals(other: Any?): Boolean =
            other is Vector128 && this.equals(other)

    override public fun hashCode(): Int {
        val x0 = getLongAt(0)
        val x1 = getLongAt(1)
        return 31 * (x0 xor (x0 shr 32)).toInt() + (x1 xor (x1 shr 32)).toInt()
    }
}

@SinceKotlin("1.4")
public inline class VectorFloat3(internal val data: Vector128) {
    public operator fun get(index: Int): Float = data.getFloatAt(index)

    public fun getIntAt(index: Int): Int = data.getIntAt(index)
    public fun getUIntAt(index: Int): UInt = data.getUIntAt(index)

    public override fun toString(): String =
            "(${this[0]}, ${this[1]}, ${this[2]})"
}

@SinceKotlin("1.4")
public inline class VectorFloat4(internal val data: Vector128) {
    public operator fun get(index: Int): Float = data.getFloatAt(index)

    public fun getIntAt(index: Int): Int = data.getIntAt(index)
    public fun getUIntAt(index: Int): UInt = data.getUIntAt(index)

    public override fun toString(): String =
            "(${this[0]}, ${this[1]}, ${this[2]}, ${this[3]})"
}

@SinceKotlin("1.4")
public inline class VectorInt3(internal val data: Vector128){
    public operator fun get(index: Int): Int = data.getIntAt(index)

    public fun getUIntAt(index: Int): UInt = data.getUIntAt(index)

    public override fun toString(): String =
            "(0x${getUIntAt(0).toString(16)}, 0x${getUIntAt(1).toString(16)}, 0x${getUIntAt(2).toString(16)}})"
}

@SinceKotlin("1.4")
public inline class VectorInt4(internal val data: Vector128) {
    public operator fun get(index: Int): Int = data.getIntAt(index)

    public fun getUIntAt(index: Int): UInt = data.getUIntAt(index)

    public override fun toString() =
            "(0x${getUIntAt(0).toString(16)}, 0x${getUIntAt(1).toString(16)}, 0x${getUIntAt(2).toString(16)}, 0x${getUIntAt(3).toString(16)})"
}

@SymbolName("Kotlin_Vector4f_of")
public external fun genericVectorOf(f0: Float, f1: Float, f2: Float, f3: Float): Vector128

@SinceKotlin("1.4")
public fun vectorOf(f0: Float, f1: Float, f2: Float): VectorFloat3
        = VectorFloat3(genericVectorOf(f0, f1, f2, 0.0f))
@SinceKotlin("1.4")
public fun vectorOf(f0: Float, f1: Float, f2: Float, f3: Float): VectorFloat4
        = VectorFloat4(genericVectorOf(f0, f1, f2, f3))

@SymbolName("Kotlin_Vector4i32_of")
public external fun genericVectorOf(i0: Int, i1: Int, i2: Int, i3: Int): Vector128

@SinceKotlin("1.4")
public fun vectorOf(i0: Int, i1: Int, i2: Int): VectorInt3
        = VectorInt3(genericVectorOf(i0, i1, i2, 0))
@SinceKotlin("1.4")
public fun vectorOf(i0: Int, i1: Int, i2: Int, i3: Int): VectorInt4
        = VectorInt4(genericVectorOf(i0, i1, i2, i3))
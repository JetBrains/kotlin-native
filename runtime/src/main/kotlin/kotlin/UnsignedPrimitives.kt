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

package kotlin

/**
 * Represents a 8-bit usigned integer.
 */
public final class UByte : Number(), Comparable<UByte> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Byte can have.
         */
        // public const val MIN_VALUE: UByte = 0

        /**
         * A constant holding the maximum value an instance of Byte can have.
         */
        // public const val MAX_VALUE: UByte = 0xff
    }

    // Methods from Number.
    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public override fun toDouble(): Double = TODO()

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public override fun toFloat(): Float = TODO()

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public override fun toLong(): Long = TODO()

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public override fun toInt(): Int = TODO()

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    public override fun toChar(): Char = TODO()

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public override fun toShort(): Short = TODO()

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public override fun toByte(): Byte = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: UByte): Int = TODO()

    // Equality.
    public fun equals(other: UByte): Boolean = konan.internal.areEqualByValue(this, other)

    // Maybe check if it is Number and compare toInt()/toLong() representations?
    public override fun equals(other: Any?): Boolean =
            other is UByte && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_UByte_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        TODO()
        // return this.toInt()
    }
}

/**
 * Represents a 16-bit unsigned integer.
 */
public final class UShort : Number(), Comparable<UShort> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Short can have.
         */
        // public const val MIN_VALUE: UShort = 0

        /**
         * A constant holding the maximum value an instance of Short can have.
         */
        // public const val MAX_VALUE: UShort = 0xffff
    }

    // Methods from Number.
    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public override fun toDouble(): Double = TODO()

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public override fun toFloat(): Float = TODO()

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public override fun toLong(): Long = TODO()

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public override fun toInt(): Int = TODO()

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    public override fun toChar(): Char = TODO()

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public override fun toShort(): Short = TODO()

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public override fun toByte(): Byte = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: UShort): Int = TODO()

    public fun equals(other: UShort): Boolean = konan.internal.areEqualByValue(this, other)

    // Maybe check if it is Number and compare toInt()/toLong() representations?
    public override fun equals(other: Any?): Boolean =
            other is UShort && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_UShort_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        TODO()
        //return this.toInt()
    }
}

/**
 * Represents a 32-bit unsigned integer.
 */
public final class UInt : Number(), Comparable<UInt> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Int can have.
         */
        //public const val MIN_VALUE: UInt = 0

        /**
         * A constant holding the maximum value an instance of Int can have.
         */
        //public const val MAX_VALUE: UInt = 0xffffffff
    }

    // Methods from Number.
    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public override fun toDouble(): Double = TODO()

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public override fun toFloat(): Float = TODO()

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public override fun toLong(): Long = TODO()

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public override fun toInt(): Int = TODO()

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    public override fun toChar(): Char = TODO()

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public override fun toShort(): Short = TODO()

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public override fun toByte(): Byte = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: UInt): Int = TODO()

    public fun equals(other: UInt): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is UInt && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_UInt_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        TODO()
        // return this.toInt()
    }
}

/**
 * Represents a 64-bit unsigned integer.
 */
public final class ULong : Number(), Comparable<ULong> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Long can have.
         */
        //public const val MIN_VALUE: ULong = 0

        /**
         * A constant holding the maximum value an instance of Long can have.
         */
        //public const val MAX_VALUE: ULong = 0xffffffffffffffffL
    }

    // Methods from Number.
    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public override fun toDouble(): Double = TODO()

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public override fun toFloat(): Float = TODO()

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public override fun toLong(): Long = TODO()

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public override fun toInt(): Int = TODO()

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    public override fun toChar(): Char = TODO()

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public override fun toShort(): Short = TODO()

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public override fun toByte(): Byte = TODO()

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: ULong): Int = TODO()

    public fun equals(other: ULong): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is ULong && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_ULong_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        TODO()
        // return ((this ushr 32) xor this).toInt()
    }
}

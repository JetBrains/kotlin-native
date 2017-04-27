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

    // Generated overrides.
    @SymbolName("Kotlin_UByte_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_UByte_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_UByte_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_UByte_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_UByte_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_UByte_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_UByte_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_UByte_compareTo_UByte")
    external public override operator fun compareTo(arg0: UByte): Int

    // Equality.
    public fun equals(other: UByte): Boolean = konan.internal.areEqualByValue(this, other)

    // Maybe check if it is Number and compare toInt()/toLong() representations?
    public override fun equals(other: Any?): Boolean =
            other is UByte && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_UByte_toString")
    external public override fun toString(): String

    public override fun hashCode() = this.toInt()
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

    // Generated overrides.
    @SymbolName("Kotlin_UShort_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_UShort_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_UShort_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_UShort_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_UShort_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_UShort_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_UShort_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_UShort_compareTo_UShort")
    external public override operator fun compareTo(arg0: UShort): Int

    public fun equals(other: UShort): Boolean = konan.internal.areEqualByValue(this, other)

    // Maybe check if it is Number and compare toInt()/toLong() representations?
    public override fun equals(other: Any?): Boolean =
            other is UShort && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_UShort_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int = this.toInt()
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

    // Generated overrides.
    @SymbolName("Kotlin_UInt_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_UInt_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_UInt_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_UInt_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_UInt_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_UInt_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_UInt_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_UInt_compareTo_UInt")
    external public override operator fun compareTo(arg0: UInt): Int

    public fun equals(other: UInt): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is UInt && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_UInt_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int = this.toInt()
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

    // Generated overrides.
    @SymbolName("Kotlin_ULong_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_ULong_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_ULong_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_ULong_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_ULong_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_ULong_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_ULong_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_ULong_compareTo_ULong")
    external public override operator fun compareTo(arg0: ULong): Int

    public fun equals(other: ULong): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is ULong && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_ULong_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return ((this shr 32) xor this).toInt()
    }
}

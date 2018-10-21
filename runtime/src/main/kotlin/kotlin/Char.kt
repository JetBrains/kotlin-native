/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

/**
 * Represents a 16-bit Unicode character.
 */
public class Char private constructor(
        private val value: kotlin.native.internal.ShortValue) : Comparable<Char> {
    /**
     * Compares this value with the specified value for order.
     *
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @SymbolName("Kotlin_Char_compareTo_Char")
    external public override fun compareTo(other: Char): Int

    /** Adds the other Int value to this value resulting a Char. */
    @SymbolName("Kotlin_Char_plus_Int")
    external public operator fun plus(other: Int): Char

    /** Subtracts the other Char value from this value resulting an Int. */
    @SymbolName("Kotlin_Char_minus_Char")
    external public operator fun minus(other: Char): Int
    /** Subtracts the other Int value from this value resulting a Char. */
    @SymbolName("Kotlin_Char_minus_Int")
    external public operator fun minus(other: Int): Char

    /** Increments this value. */
    @SymbolName("Kotlin_Char_inc")
    external public operator fun inc(): Char
    /** Decrements this value. */
    @SymbolName("Kotlin_Char_dec")
    external public operator fun dec(): Char

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Char): CharRange {
        return CharRange(this, other)
    }

    /** Returns the value of this character as a `Byte`. */
    @SymbolName("Kotlin_Char_toByte")
    external public fun toByte(): Byte
    /** Returns the value of this character as a `Char`. */
    @SymbolName("Kotlin_Char_toChar")
    external public fun toChar(): Char
    /** Returns the value of this character as a `Short`. */
    @SymbolName("Kotlin_Char_toShort")
    external public fun toShort(): Short
    /** Returns the value of this character as a `Int`. */
    @SymbolName("Kotlin_Char_toInt")
    external public fun toInt(): Int
    /** Returns the value of this character as a `Long`. */
    @SymbolName("Kotlin_Char_toLong")
    external public fun toLong(): Long
    /** Returns the value of this character as a `Float`. */
    @SymbolName("Kotlin_Char_toFloat")
    external public fun toFloat(): Float
    /** Returns the value of this character as a `Double`. */
    @SymbolName("Kotlin_Char_toDouble")
    external public fun toDouble(): Double

    companion object {
        /**
         * The minimum value of a character code unit.
         */
        @SinceKotlin("1.3")
        public const val MIN_VALUE: Char = '\u0000'

        /**
         * The maximum value of a character code unit.
         */
        @SinceKotlin("1.3")
        public const val MAX_VALUE: Char = '\uFFFF'

        /**
         * The number of bytes used to represent a Char in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BYTES: Int = 2
        /**
         * The number of bits used to represent a Char in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BITS: Int = 16

        /**
         * The minimum value of a Unicode high-surrogate code unit.
         */
        public const val MIN_HIGH_SURROGATE: Char = '\uD800'

        /**
         * The maximum value of a Unicode high-surrogate code unit.
         */
        public const val MAX_HIGH_SURROGATE: Char = '\uDBFF'

        /**
         * The minimum value of a Unicode low-surrogate code unit.
         */
        public const val MIN_LOW_SURROGATE: Char = '\uDC00'

        /**
         * The maximum value of a Unicode low-surrogate code unit.
         */
        public const val MAX_LOW_SURROGATE: Char = '\uDFFF'

        /**
         * The minimum value of a Unicode surrogate code unit.
         */
        public const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE

        /**
         * The maximum value of a Unicode surrogate code unit.
         */
        public const val MAX_SURROGATE: Char = MAX_LOW_SURROGATE

        /**
         * The minimum value of a supplementary code point, `\u0x10000`. Kotlin/Native specific.
         */
        public const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

        /**
         * The minimum value of a Unicode code point. Kotlin/Native specific.
         */
        public const val MIN_CODE_POINT = 0x000000

        /**
         * The maximum value of a Unicode code point. Kotlin/Native specific.
         */
        public const val MAX_CODE_POINT = 0X10FFFF

        /**
         * The minimum radix available for conversion to and from strings.
         */
        public const val MIN_RADIX: Int = 2

        /**
         * The maximum radix available for conversion to and from strings.
         */
        public const val MAX_RADIX: Int = 36
    }

    public fun equals(other: Char): Boolean = this == other

    public override fun equals(other: Any?): Boolean =
            other is Char && this.equals(other)

    @SymbolName("Kotlin_Char_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt();
    }
}


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
 * Represents a 8-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `byte`.
 */
public final class Byte : Number(), Comparable<Byte> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Byte can have.
         */
        public const val MIN_VALUE: Byte = -128

        /**
         * A constant holding the maximum value an instance of Byte can have.
         */
        public const val MAX_VALUE: Byte = 127
    }

    // Generated overrides.
    @SymbolName("Kotlin_Byte_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Byte_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Byte_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Byte_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Byte_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Byte_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_Byte_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Byte_compareTo_Byte")
    external public override operator fun compareTo(arg0: Byte): Int
    public operator fun rangeTo(other: Byte) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Short) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Int) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Long) = LongRange(this.toLong(), other.toLong())

    // Konan-specific.
    public fun equals(other: Byte): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Byte && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Byte_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt()
    }
}

/**
 * Represents a 16-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `short`.
 */
public final class Short : Number(), Comparable<Short> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Short can have.
         */
        public const val MIN_VALUE: Short = -32768

        /**
         * A constant holding the maximum value an instance of Short can have.
         */
        public const val MAX_VALUE: Short = 32767
    }

    // Generated overrides.
    @SymbolName("Kotlin_Short_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Short_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Short_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Short_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Short_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Short_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_Short_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Short_compareTo_Short")
    external public override operator fun compareTo(arg0: Short): Int
    public operator fun rangeTo(other: Byte) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Short) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Int) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Long) = LongRange(this.toLong(), other.toLong())

    // Konan-specific.
    public fun equals(other: Short): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
        other is Short && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Short_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this.toInt()
    }
}

/**
 * Represents a 32-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `int`.
 */
public final class Int : Number(), Comparable<Int> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Int can have.
         */
        public const val MIN_VALUE: Int = -2147483648

        /**
         * A constant holding the maximum value an instance of Int can have.
         */
        public const val MAX_VALUE: Int = 2147483647
    }

    // Generated overrides.
    @SymbolName("Kotlin_Int_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Int_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Int_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Int_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Int_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Int_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_Int_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Int_compareTo_Int")
    external public override operator fun compareTo(arg0: Int): Int
    public operator fun rangeTo(other: Byte) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Short) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Int) = IntRange(this.toInt(), other.toInt())
    public operator fun rangeTo(other: Long) = LongRange(this.toLong(), other.toLong())

    // To workaround FE misfeature.
    @SymbolName("Kotlin_Int_plus_Byte")
    external public operator fun plus(arg0: Byte): Int
    @SymbolName("Kotlin_Int_plus_Short")
    external public operator fun plus(arg0: Short): Int
    @SymbolName("Kotlin_Int_plus_Int")
    external public operator fun plus(arg0: Int): Int
    @SymbolName("Kotlin_Int_plus_Long")
    external public operator fun plus(arg0: Long): Long
    @SymbolName("Kotlin_Int_plus_Float")
    external public operator fun plus(arg0: Float): Float
    @SymbolName("Kotlin_Int_plus_Double")
    external public operator fun plus(arg0: Double): Double
    @SymbolName("Kotlin_Int_times_Byte")
    external public operator fun times(arg0: Byte): Int
    @SymbolName("Kotlin_Int_times_Short")
    external public operator fun times(arg0: Short): Int
    @SymbolName("Kotlin_Int_times_Int")
    external public operator fun times(arg0: Int): Int
    @SymbolName("Kotlin_Int_times_Long")
    external public operator fun times(arg0: Long): Long
    @SymbolName("Kotlin_Int_times_Float")
    external public operator fun times(arg0: Float): Float
    @SymbolName("Kotlin_Int_times_Double")
    external public operator fun times(arg0: Double): Double
    // Default arguments generator uses this one.
    @SymbolName("Kotlin_Int_and_Int")
    external public infix fun and(arg0: Int): Int

    // Konan-specific.
    public fun equals(other: Int): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
         other is Int && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Int_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return this
    }
}

/**
 * Represents a 64-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `long`.
 */
public final class Long : Number(), Comparable<Long> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Long can have.
         */
        public const val MIN_VALUE: Long = -9223372036854775807L - 1L

        /**
         * A constant holding the maximum value an instance of Long can have.
         */
        public const val MAX_VALUE: Long = 9223372036854775807L
    }

    // Generated overrides.
    @SymbolName("Kotlin_Long_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Long_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Long_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Long_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Long_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Long_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_Long_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Long_compareTo_Long")
    external public override operator fun compareTo(arg0: Long): Int
    public operator fun rangeTo(other: Byte) = LongRange(this.toLong(), other.toLong())
    public operator fun rangeTo(other: Short) = LongRange(this.toLong(), other.toLong())
    public operator fun rangeTo(other: Int) = LongRange(this.toLong(), other.toLong())
    public operator fun rangeTo(other: Long) = LongRange(this.toLong(), other.toLong())

    // Konan-specific.
    public fun equals(other: Long): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Long && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Long_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
       return ((this ushr 32) xor this).toInt()
    }
}

/**
 * Represents a single-precision 32-bit IEEE 754 floating point number.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `float`.
 */
public final class Float : Number(), Comparable<Float> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Float.
         */
        public const val MIN_VALUE: Float = 1.17549435E-38f

        /**
         * A constant holding the largest positive finite value of Float.
         */
        public const val MAX_VALUE: Float = 3.4028235E+38f

        /**
         * A constant holding the positive infinity value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val POSITIVE_INFINITY: Float = 1.0f / 0.0f

        /**
         * A constant holding the negative infinity value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NEGATIVE_INFINITY: Float = -1.0f / 0.0f

        /**
         * A constant holding the "not a number" value of Float.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NaN: Float = 0.0f / 0.0f
    }

    // Generated overrides.
    @SymbolName("Kotlin_Float_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Float_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Float_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Float_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Float_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Float_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_Float_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Float_compareTo_Float")
    external public override operator fun compareTo(arg0: Float): Int

    // Konan-specific.
    // We intentionally provide this overload to equals() to avoid artifical boxing.
    // Note that here we intentionally deviate from JVM Kotlin, where this method would be
    // this.bits() == other.bits().
    public fun equals(other: Float): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Float && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Float_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return bits()
    }

    @SymbolName("Kotlin_Float_bits")
    external public fun bits(): Int
}

/**
 * Represents a double-precision 64-bit IEEE 754 floating point number.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `double`.
 */
public final class Double : Number(), Comparable<Double> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Double.
         */
        public const val MIN_VALUE: Double = 4.9e-324

        /**
         * A constant holding the largest positive finite value of Double.
         */
        public const val MAX_VALUE: Double = 1.7976931348623157e+308

        /**
         * A constant holding the positive infinity value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val POSITIVE_INFINITY: Double = 1.0 / 0.0

        /**
         * A constant holding the negative infinity value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NEGATIVE_INFINITY: Double = -1.0 / 0.0

        /**
         * A constant holding the "not a number" value of Double.
         */
        @Suppress("DIVISION_BY_ZERO")
        public val NaN: Double = 0.0 / 0.0
    }

    // Generated overrides.
    @SymbolName("Kotlin_Double_toByte")
    external public override fun toByte(): Byte
    @SymbolName("Kotlin_Double_toShort")
    external public override fun toShort(): Short
    @SymbolName("Kotlin_Double_toInt")
    external public override fun toInt(): Int
    @SymbolName("Kotlin_Double_toLong")
    external public override fun toLong(): Long
    @SymbolName("Kotlin_Double_toFloat")
    external public override fun toFloat(): Float
    @SymbolName("Kotlin_Double_toDouble")
    external public override fun toDouble(): Double
    @SymbolName("Kotlin_Double_toChar")
    external public override fun toChar(): Char
    @SymbolName("Kotlin_Double_compareTo_Double")
    external public override operator fun compareTo(arg0: Double): Int

    // Konan-specific.
    // Note that here we intentionally deviate from JVM Kotlin, where this method would be
    // this.bits() == other.bits().
    public fun equals(other: Double): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
            other is Double && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Double_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return bits().hashCode()
    }

    @SymbolName("Kotlin_Double_bits")
    external public fun bits(): Long
}

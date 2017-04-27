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

package konan.internal

// TODO: cache some boxes.

class BooleanBox(val value: Boolean) : Comparable<Boolean> {
    override fun equals(other: Any?): Boolean {
        if (other !is BooleanBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Boolean): Int = value.compareTo(other)
}

fun boxBoolean(value: Boolean) = BooleanBox(value)

class CharBox(val value: Char) : Comparable<Char> {
    override fun equals(other: Any?): Boolean {
        if (other !is CharBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Char): Int = value.compareTo(other)
}

fun boxChar(value: Char) = CharBox(value)

class ByteBox(val value: Byte) : Number(), Comparable<Byte> {
    override fun equals(other: Any?): Boolean {
        if (other !is ByteBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Byte): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxByte(value: Byte) = ByteBox(value)

class UByteBox(val value: UByte) : Number(), Comparable<UByte> {
    override fun equals(other: Any?): Boolean {
        if (other !is UByteBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: UByte): Int = TODO() // value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxUByte(value: UByte) = UByteBox(value)

class ShortBox(val value: Short) : Number(), Comparable<Short> {
    override fun equals(other: Any?): Boolean {
        if (other !is ShortBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Short): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxShort(value: Short) = ShortBox(value)

class UShortBox(val value: UShort) : Number(), Comparable<UShort> {
    override fun equals(other: Any?): Boolean {
        if (other !is UShortBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: UShort): Int = TODO() // value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxUShort(value: UShort) = UShortBox(value)

class IntBox(val value: Int) : Number(), Comparable<Int> {
    override fun equals(other: Any?): Boolean {
        if (other !is IntBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Int): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxInt(value: Int) = IntBox(value)

class UIntBox(val value: UInt) : Number(), Comparable<UInt> {
    override fun equals(other: Any?): Boolean {
        if (other !is UIntBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: UInt): Int = TODO() // value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxUInt(value: UInt) = UIntBox(value)

class LongBox(val value: Long) : Number(), Comparable<Long> {
    override fun equals(other: Any?): Boolean {
        if (other !is LongBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Long): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxLong(value: Long) = LongBox(value)

class ULongBox(val value: ULong) : Number(), Comparable<ULong> {
    override fun equals(other: Any?): Boolean {
        if (other !is ULongBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: ULong): Int = TODO() // value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxULong(value: ULong) = ULongBox(value)

class FloatBox(val value: Float) : Number(), Comparable<Float> {
    override fun equals(other: Any?): Boolean {
        if (other !is FloatBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Float): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxFloat(value: Float) = FloatBox(value)

class DoubleBox(val value: Double) : Number(), Comparable<Double> {
    override fun equals(other: Any?): Boolean {
        if (other !is DoubleBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()

    override fun compareTo(other: Double): Int = value.compareTo(other)

    override fun toByte() = value.toByte()
    override fun toChar() = value.toChar()
    override fun toShort() = value.toShort()
    override fun toInt() = value.toInt()
    override fun toLong() = value.toLong()
    override fun toFloat() = value.toFloat()
    override fun toDouble() = value.toDouble()
}

fun boxDouble(value: Double) = DoubleBox(value)

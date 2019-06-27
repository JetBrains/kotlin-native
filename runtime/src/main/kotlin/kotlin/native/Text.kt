/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native


/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use decodeToString or decodeZeroTerminatedToString instead")
public fun ByteArray.stringFromUtf8() : String {
    @Suppress("DEPRECATION")
    return this.stringFromUtf8(0, this.size)
}

/**
 * Converts an UTF-8 array into a [String]. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use decodeToString or decodeZeroTerminatedToString instead")
public fun ByteArray.stringFromUtf8(start: Int = 0, size: Int = this.size) : String {
    val endIndex = start + size
    checkBoundsIndexes(start, endIndex, this.size)

    val realSize = realEndIndex(this, start, endIndex) - start
    return unsafeStringFromUtf8Impl(start, realSize)
}

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array.
 * Bytes following the first occurrence of `0` byte, if it occurs, are not decoded.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
@SinceKotlin("1.3")
public fun ByteArray.decodeZeroTerminatedToString() : String {
    return unsafeStringFromUtf8Impl(0, realEndIndex(this, 0, this.size))
}

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array or its subrange.
 * Bytes following the first occurrence of `0` byte, if it occurs, are not decoded.
 *
 * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to decode, size of this array by default.
 * @param throwOnInvalidSequence specifies whether to throw an exception on malformed byte sequence or replace it by the replacement char `\uFFFD`.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the size of this array.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 * @throws CharacterCodingException if the byte array contains malformed UTF-8 byte sequence and [throwOnInvalidSequence] is true.
 */
@SinceKotlin("1.3")
public fun ByteArray.decodeZeroTerminatedToString(
        startIndex: Int = 0,
        endIndex: Int = this.size,
        throwOnInvalidSequence: Boolean = false
) : String {
    checkBoundsIndexes(startIndex, endIndex, this.size)

    val realSize = realEndIndex(this, startIndex, endIndex) - startIndex
    if (throwOnInvalidSequence) {
        return unsafeStringFromUtf8OrThrowImpl(startIndex, realSize)
    }
    return unsafeStringFromUtf8Impl(startIndex, realSize)
}

@SymbolName("Kotlin_ByteArray_unsafeStringFromUtf8")
internal external fun ByteArray.unsafeStringFromUtf8Impl(start: Int, size: Int) : String

private fun realEndIndex(byteArray: ByteArray, startIndex: Int, endIndex: Int): Int {
    var index = startIndex
    while (index < endIndex && byteArray[index] != 0.toByte()) {
        index++
    }
    return index
}

/**
 * Converts an UTF-8 array into a [String].
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use decodeToString or decodeZeroTerminatedToString instead")
public fun ByteArray.stringFromUtf8OrThrow() : String {
    @Suppress("DEPRECATION")
    return this.stringFromUtf8OrThrow(0, this.size)
}

/**
 * Converts an UTF-8 array into a [String].
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use decodeToString or decodeZeroTerminatedToString instead")
public fun ByteArray.stringFromUtf8OrThrow(start: Int = 0, size: Int = this.size) : String {
    val endIndex = start + size
    checkBoundsIndexes(start, endIndex, this.size)

    val realSize = realEndIndex(this, start, endIndex) - start
    try {
        return unsafeStringFromUtf8OrThrowImpl(start, realSize)
    } catch (e: CharacterCodingException) {
        @Suppress("DEPRECATION")
        throw IllegalCharacterConversionException()
    }
}

@SymbolName("Kotlin_ByteArray_unsafeStringFromUtf8OrThrow")
internal external fun ByteArray.unsafeStringFromUtf8OrThrowImpl(start: Int, size: Int) : String

/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray()"))
public fun String.toUtf8() : ByteArray {
    @Suppress("DEPRECATION")
    return this.toUtf8(0, this.length)
}

/**
 * Converts a [String] into an UTF-8 array. Replaces invalid input sequences with a default character.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray(start, start + size)"))
public fun String.toUtf8(start: Int = 0, size: Int = this.length) : ByteArray {
    checkBoundsIndexes(start, start + size, this.length)
    return unsafeToUtf8Impl(start, size)
}

@SymbolName("Kotlin_String_unsafeToUtf8")
internal external fun String.unsafeToUtf8Impl(start: Int, size: Int) : ByteArray

/**
 * Converts a [String] into an UTF-8 array.
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray(throwOnInvalidSequence = true)"))
public fun String.toUtf8OrThrow() : ByteArray {
    @Suppress("DEPRECATION")
    return this.toUtf8OrThrow(0, this.length)
}

/**
 * Converts a [String] into an UTF-8 array.
 * @throws [IllegalCharacterConversionException] if the input is invalid.
 */
@Deprecated("Use encodeToByteArray instead", ReplaceWith("encodeToByteArray(start, start + size, throwOnInvalidSequence = true)"))
public fun String.toUtf8OrThrow(start: Int = 0, size: Int = this.length) : ByteArray {
    checkBoundsIndexes(start, start + size, this.length)
    try {
        return unsafeToUtf8OrThrowImpl(start, size)
    } catch (e: CharacterCodingException) {
        @Suppress("DEPRECATION")
        throw IllegalCharacterConversionException()
    }
}

internal fun checkBoundsIndexes(startIndex: Int, endIndex: Int, size: Int) {
    if (startIndex < 0 || endIndex > size) {
        throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")
    }
    if (startIndex > endIndex) {
        throw IllegalArgumentException("startIndex: $startIndex > endIndex: $endIndex")
    }
}

@SymbolName("Kotlin_String_unsafeToUtf8OrThrow")
internal external fun String.unsafeToUtf8OrThrowImpl(start: Int, size: Int) : ByteArray

@SymbolName("Kotlin_String_unsafeFromCharArray")
internal external fun unsafeFromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_StringBuilder_insertString")
internal external fun insertString(array: CharArray, start: Int, value: String): Int

@SymbolName("Kotlin_StringBuilder_insertInt")
internal external fun insertInt(array: CharArray, start: Int, value: Int): Int
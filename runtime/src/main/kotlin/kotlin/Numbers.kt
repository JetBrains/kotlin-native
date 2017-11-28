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
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Double_isNaN")
external public fun Double.isNaN(): Boolean

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Float_isNaN")
external public fun Float.isNaN(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Double_isInfinite")
external public fun Double.isInfinite(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Float_isInfinite")
external public fun Float.isInfinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Double_isFinite")
external public fun Double.isFinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Float_isFinite")
external public fun Float.isFinite(): Boolean

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Double.toBits(): Long = if (isNaN()) Double.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Double.toRawBits(): Long = bits()

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Double.Companion.fromBits(bits: Long): Double = kotlin.fromBits(bits)

@PublishedApi
@SymbolName("Kotlin_Double_fromBits")
external internal fun fromBits(bits: Long): Double

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Float.toBits(): Int = if (isNaN()) Float.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Float.toRawBits(): Int = bits()

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Float.Companion.fromBits(bits: Int): Float = kotlin.fromBits(bits)

@PublishedApi
@SymbolName("Kotlin_Float_fromBits")
external internal fun fromBits(bits: Int): Float

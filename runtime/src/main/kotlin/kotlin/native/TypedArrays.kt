/*
 * Copyright 2010-2018 JetBrains s.r.o.
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
package kotlin.native

import kotlin.native.SymbolName

@ExperimentalUnsignedTypes
public fun ByteArray.ubyteAt(index: Int): UByte = UByte(get(index))

@SymbolName("Kotlin_ByteArray_getCharAt")
public external fun ByteArray.charAt(index: Int): Char

@SymbolName("Kotlin_ByteArray_getShortAt")
public external fun ByteArray.shortAt(index: Int): Short

@SymbolName("Kotlin_ByteArray_getShortAt")
@ExperimentalUnsignedTypes
public external fun ByteArray.ushortAt(index: Int): UShort

@SymbolName("Kotlin_ByteArray_getIntAt")
public external fun ByteArray.intAt(index: Int): Int

@SymbolName("Kotlin_ByteArray_getIntAt")
@ExperimentalUnsignedTypes
public external fun ByteArray.uintAt(index: Int): UInt

@SymbolName("Kotlin_ByteArray_getLongAt")
public external fun ByteArray.longAt(index: Int): Long

@SymbolName("Kotlin_ByteArray_getLongAt")
@ExperimentalUnsignedTypes
public external fun ByteArray.ulongAt(index: Int): ULong

@SymbolName("Kotlin_ByteArray_getFloatAt")
public external fun ByteArray.floatAt(index: Int): Float

@SymbolName("Kotlin_ByteArray_getDoubleAt")
public external fun ByteArray.doubleAt(index: Int): Double

@SymbolName("Kotlin_ByteArray_setCharAt")
public external fun ByteArray.setCharAt(index: Int, value: Char)

@SymbolName("Kotlin_ByteArray_setShortAt")
public external fun ByteArray.setShortAt(index: Int, value: Short)

@SymbolName("Kotlin_ByteArray_setShortAt")
@ExperimentalUnsignedTypes
public external fun ByteArray.setUShortAt(index: Int, value: UShort)

@SymbolName("Kotlin_ByteArray_setIntAt")
public external fun ByteArray.setIntAt(index: Int, value: Int)

@SymbolName("Kotlin_ByteArray_setIntAt")
public external fun ByteArray.setUIntAt(index: Int, value: UInt)

@SymbolName("Kotlin_ByteArray_setLongAt")
public external fun ByteArray.setLongAt(index: Int, value: Long)

@SymbolName("Kotlin_ByteArray_setLongAt")
@ExperimentalUnsignedTypes
public external fun ByteArray.setULongAt(index: Int, value: ULong)

@SymbolName("Kotlin_ByteArray_setFloatAt")
public external fun ByteArray.setFloatAt(index: Int, value: Float)

@SymbolName("Kotlin_ByteArray_setDoubleAt")
public external fun ByteArray.setDoubleAt(index: Int, value: Double)

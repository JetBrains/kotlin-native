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

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.toByte
import llvm.*
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.types.KotlinType

internal fun KonanSymbols.getTypeConversion(
        actualType: KotlinType,
        expectedType: KotlinType
): IrSimpleFunctionSymbol? {
    val actualValueType = actualType.correspondingValueType
    val expectedValueType = expectedType.correspondingValueType

    return when {
        actualValueType == expectedValueType -> null

        actualValueType == null && expectedValueType != null -> {
            // This may happen in the following cases:
            // 1.  `actualType` is `Nothing`;
            // 2.  `actualType` is incompatible.

            this.getUnboxFunction(expectedValueType)
        }

        actualValueType != null && expectedValueType == null -> {
            this.boxFunctions[actualValueType]!!
        }

        else -> throw IllegalArgumentException("actual type is $actualType, expected $expectedType")
    }
}

internal fun KonanSymbols.getUnboxFunction(valueType: ValueType): IrSimpleFunctionSymbol =
        this.unboxFunctions[valueType]
                ?: this.boxClasses[valueType]!!.getPropertyGetter("value")!! as IrSimpleFunctionSymbol


/**
 * Initialize static boxing.
 * If output target is native binary then the cache is created.
 */
internal fun initializeCachedBoxes(context: Context) {
    if (context.config.produce.isNativeBinary) {
        val cachedTypes = listOf(ValueType.BOOLEAN, ValueType.BYTE, ValueType.CHAR,
                ValueType.SHORT, ValueType.INT, ValueType.LONG)
        cachedTypes.forEach { valueType ->
            val cacheName = "${valueType.name}_CACHE"
            val rangeStart = "${valueType.name}_RANGE_FROM"
            val rangeEnd = "${valueType.name}_RANGE_TO"
            valueType.initCache(context, cacheName, rangeStart, rangeEnd)
        }
    }
}

/**
 * Adds global that refers to the cache.
 */
private fun ValueType.initCache(context: Context, cacheName: String,
                                rangeStartName: String, rangeEndName: String) {
    val kotlinType = context.ir.symbols.boxClasses[this]!!.owner
    val (start, end) = context.config.target.getBoxCacheRange(this)
    // Constancy of these globals allows LLVM's constant propagation and DCE
    // to remove fast path of boxing function in case of empty range.
    context.llvm.staticData.placeGlobal(rangeStartName, createConstant(start), true)
            .setConstant(true)
    context.llvm.staticData.placeGlobal(rangeEndName, createConstant(end), true)
            .setConstant(true)
    val staticData = context.llvm.staticData
    val values = (start..end).map { staticData.createInitializer(kotlinType, createConstant(it)) }
    val llvmBoxType = structType(context.llvm.runtime.objHeaderType, this.llvmMemoryType)
    staticData.placeGlobalConstArray(cacheName, llvmBoxType, values, true).llvm
}

private fun ValueType.createConstant(value: Int): ConstValue {
    val (longValue, signExtend) = when (this) {
        ValueType.BOOLEAN   -> (value > 0).toByte().toLong()    to 0
        ValueType.BYTE      -> value.toByte().toLong()          to 1
        ValueType.CHAR      -> value.toChar().toLong()          to 0
        ValueType.SHORT     -> value.toShort().toLong()         to 1
        ValueType.INT       -> value.toLong()                   to 1
        ValueType.LONG      -> value.toLong()                   to 1
        else                -> error("Cannot box value of type $this")
    }
    return constValue(LLVMConstInt(this.llvmMemoryType, longValue, signExtend)!!)
}

// When start is greater than end then `inRange` check is always false
// and can be eliminated by LLVM.
private val emptyRange = 1 to 0

// Memory usage is around 20kb.
private val defaultCacheRanges = mapOf(
        ValueType.BOOLEAN to (0 to 1),
        ValueType.BYTE  to (-128 to 127),
        ValueType.SHORT to (-128 to 127),
        ValueType.CHAR  to (0 to 255),
        ValueType.INT   to (-128 to 127),
        ValueType.LONG  to (-128 to 127)
)

fun KonanTarget.getBoxCacheRange(valueType: ValueType): Pair<Int, Int> = when (this) {
    is KonanTarget.ZEPHYR   -> emptyRange
    else                    -> defaultCacheRanges[valueType]!!
}
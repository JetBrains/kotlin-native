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

package org.jetbrains.kotlin.native.interop.gen

/**
 * The type which has exact counterparts on both Kotlin and native side and can be directly passed through bridges.
 */
enum class BridgedType(val kotlinType: String, val convertor: String? = null) {
    BYTE("kotlin.Byte", "toByte"),
    SHORT("kotlin.Short", "toShort"),
    INT("kotlin.Int",  "toInt"),
    LONG("kotlin.Long", "toLong"),
    FLOAT("kotlin.Float", "toFloat"),
    DOUBLE("kotlin.Double", "toDouble"),
    NATIVE_PTR("NativePtr"),
    OBJC_POINTER("NativePtr"),
    VOID("kotlin.Unit")
}

data class BridgeTypedKotlinValue(val type: BridgedType, val value: KotlinExpression)
data class BridgeTypedNativeValue(val type: BridgedType, val value: NativeExpression)

/**
 * The entity which depends on native bridges.
 */
interface NativeBacked

/**
 * Generates simple bridges between Kotlin and native, passing [BridgedType] values.
 */
interface SimpleBridgeGenerator {

    /**
     * Generates the expression to convert given Kotlin values to native counterparts, pass through the bridge,
     * use inside the native code produced by [block] and then return the result back.
     *
     * @param block produces native code lines into the builder and returns the expression to be used as the result.
     */
    fun kotlinToNative(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinValue>,
            block: NativeCodeBuilder.(nativeValues: List<NativeExpression>) -> NativeExpression
    ): KotlinExpression

    /**
     * Generates the expression to convert given native values to Kotlin counterparts, pass through the bridge,
     * use inside the Kotlin code produced by [block] and then return the result back.
     */
    fun nativeToKotlin(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeValue>,
            block: KotlinCodeBuilder.(kotlinValues: List<KotlinExpression>) -> KotlinExpression
    ): NativeExpression

    fun insertNativeBridge(
            nativeBacked: NativeBacked,
            kotlinLines: List<String>,
            nativeLines: List<String>
    )

    /**
     * Prepares all requested native bridges.
     */
    fun prepare(): NativeBridges
}

interface NativeBridges {
    /**
     * @return `true` iff given entity is supported by these bridges,
     * i.e. all bridges it depends on can be successfully generated.
     */
    fun isSupported(nativeBacked: NativeBacked): Boolean

    val kotlinLines: Sequence<String>
    val nativeLines: Sequence<String>
}


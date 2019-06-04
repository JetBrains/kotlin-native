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
enum class BridgedType(val kotlinType: KotlinClassifierType) {
    BYTE(KotlinTypes.byte),
    SHORT(KotlinTypes.short),
    INT(KotlinTypes.int),
    LONG(KotlinTypes.long),
    UBYTE(KotlinTypes.uByte),
    USHORT(KotlinTypes.uShort),
    UINT(KotlinTypes.uInt),
    ULONG(KotlinTypes.uLong),
    FLOAT(KotlinTypes.float),
    DOUBLE(KotlinTypes.double),
    NATIVE_PTR(KotlinTypes.nativePtr),
    OBJC_POINTER(KotlinTypes.nativePtr),
    VOID(KotlinTypes.unit)
}

interface BridgeTypedKotlinValue<KotlinExprTy> {
    val type: BridgedType
    val value: KotlinExprTy
}

interface BridgeTypedNativeValue<NativeExprTy> {
    val type: BridgedType
    val value: NativeExprTy
}

data class BridgeTypedKotlinTextValue(
        override val type: BridgedType,
        override val value: KotlinTextExpression
) : BridgeTypedKotlinValue<KotlinTextExpression>

data class BridgeTypedNativeTextValue(
        override val type: BridgedType,
        override val value: NativeTextExpression
) : BridgeTypedNativeValue<NativeTextExpression>

/**
 * The entity which depends on native bridges.
 */
interface NativeBacked

interface KotlinToNativeBridgeGenerator<CallbackTy, KotlinExprTy, BridgeNativeTy, BridgeKotlinTy> {
    /**
     * Generates the expression to convert given Kotlin values to native counterparts, pass through the bridge,
     * use inside the native code produced by [block] and then return the result back.
     *
     * @param block produces native code lines into the builder and returns the expression to be used as the result.
     */
    fun generateBridge(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinValue<KotlinExprTy>>,
            independent: Boolean,
            block: CallbackTy
    ): KotlinExprTy

    fun kotlinPart(
            kotlinFunctionName: String,
            symbolName: String,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinValue<KotlinExprTy>>,
            independent: Boolean
    ): BridgeKotlinTy

    fun nativePart(
            kotlinFunctionName: String,
            symbolName: String,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinValue<KotlinExprTy>>,
            block: CallbackTy
    ): BridgeNativeTy
}

interface NativeToKotlinBridgeGenerator<CallbackTy, NativeExprTy, BridgeNativeTy, BridgeKotlinTy> {
    /**
     * Generates the expression to convert given native values to Kotlin counterparts, pass through the bridge,
     * use inside the Kotlin code produced by [block] and then return the result back.
     */
    fun generateBridge(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeValue<NativeExprTy>>,
            block: KotlinCodeBuilder.(kotlinValues: List<KotlinTextExpression>) -> KotlinTextExpression
    ): NativeExprTy

    fun nativePart(
            symbolName: String,
            nativeValues: List<BridgeTypedNativeValue<NativeExprTy>>,
            returnType: BridgedType
    ): BridgeNativeTy

    fun kotlinPart(
            kotlinFunctionName: String,
            symbolName: String,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeValue<NativeExprTy>>,
            block: CallbackTy
    ): BridgeKotlinTy
}

interface NativeBridges<BridgeKotlinTy, BridgeNativeTy> {
    val kotlinParts: Sequence<BridgeKotlinTy>
    val nativeParts: Sequence<BridgeNativeTy>
}

typealias TextualStub = List<String>

interface NativeTextBridges : NativeBridges<TextualStub, TextualStub> {
    /**
     * @return `true` iff given entity is supported by these bridges,
     * i.e. all bridges it depends on can be successfully generated.
     */
    fun isSupported(nativeBacked: NativeBacked): Boolean

    override val kotlinParts: Sequence<TextualStub>
    override val nativeParts: Sequence<TextualStub>
}

interface NativeBridgesManager<BridgeNativeTy, BridgeKotlinTy> {
    fun insertNativeBridge(
            nativeBacked: NativeBacked,
            kotlinPart: BridgeKotlinTy,
            nativePart: BridgeNativeTy
    )

    /**
     * Prepares all requested native bridges.
     */
    fun prepare(): NativeTextBridges
}

/**
 * Generates simple bridges between Kotlin and native, passing [BridgedType] values.
 */

// TODO: better naming
typealias NativeTextCallback = NativeCodeBuilder.(nativeValues: List<NativeTextExpression>) -> NativeTextExpression
typealias KotlinTextCallback = KotlinCodeBuilder.(kotlinValues: List<KotlinTextExpression>) -> KotlinTextExpression

interface SimpleBridgeGenerator : NativeBridgesManager<List<String>, List<String>> {

    val topLevelNativeScope: NativeScope

    fun kotlinToNative(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinTextValue>,
            independent: Boolean,
            block: NativeTextCallback
    ): KotlinTextExpression

    fun nativeToKotlin(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeTextValue>,
            block: KotlinTextCallback
    ): NativeTextExpression
}


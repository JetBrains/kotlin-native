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

import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.Language
import org.jetbrains.kotlin.native.interop.indexer.NativeLibrary
import org.jetbrains.kotlin.native.interop.indexer.mapFragmentIsCompilable

typealias TextualStub = List<String>
typealias NativeTextBridges = NativeBridges<TextualStub, TextualStub>

// TODO: better naming
typealias NativeTextCallback = NativeCodeBuilder.(nativeValues: List<NativeTextExpression>) -> NativeTextExpression
typealias KotlinTextCallback = KotlinCodeBuilder.(kotlinValues: List<KotlinTextExpression>) -> KotlinTextExpression

/**
 * Generates simple bridges between Kotlin and native, passing [BridgedType] values.
 */
interface SimpleBridgeGenerator : NativeBridgesManager<TextualStub, TextualStub> {

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


class SimpleBridgeGeneratorImpl(
        private val platform: KotlinPlatform,
        private val pkgName: String,
        private val jvmFileClassName: String,
        private val libraryForCStubs: NativeLibrary,
        override val topLevelNativeScope: NativeScope,
        private val topLevelKotlinScope: KotlinScope
) : SimpleBridgeGenerator {

    private val kotlinToNative = object : KotlinToNativeBridgeGenerator<NativeTextCallback, KotlinTextExpression, TextualStub, TextualStub> {
        override fun generateBridge(
                nativeBacked: NativeBacked,
                returnType: BridgedType,
                kotlinValues: List<BridgeTypedKotlinValue<String>>,
                independent: Boolean,
                block: NativeTextCallback
        ): KotlinTextExpression {
            val kotlinFunctionName = "kniBridge${nextUniqueId++}"

            val symbolName by lazy {
                pkgName.replace(INVALID_CLANG_IDENTIFIER_REGEX, "_") + "_$kotlinFunctionName"
            }

            val nativeLines = nativePart(kotlinFunctionName, symbolName, returnType, kotlinValues, block)
            val kotlinLines = kotlinPart(kotlinFunctionName, symbolName, returnType, kotlinValues, independent)
            insertNativeBridge(nativeBacked, kotlinLines, nativeLines)
            return "$kotlinFunctionName(${kotlinValues.joinToString { it.value }})"
        }

        override fun kotlinPart(
                kotlinFunctionName: String,
                symbolName: String,
                returnType: BridgedType,
                kotlinValues: List<BridgeTypedKotlinValue<String>>,
                independent: Boolean
        ): TextualStub {
            val kotlinLines = mutableListOf<String>()
            val kotlinReturnType = returnType.kotlinType.render(topLevelKotlinScope)
            val kotlinParameters = kotlinValues.withIndex().joinToString {
                "p${it.index}: ${it.value.type.kotlinType.render(topLevelKotlinScope)}"
            }
            if (platform == KotlinPlatform.NATIVE) {
                if (independent) kotlinLines.add("@" + topLevelKotlinScope.reference(KotlinTypes.independent))
                kotlinLines.add("@SymbolName(${symbolName.quoteAsKotlinLiteral()})")
            }
            kotlinLines.add("private external fun $kotlinFunctionName($kotlinParameters): $kotlinReturnType")

            return kotlinLines
        }

        override fun nativePart(
                kotlinFunctionName: String,
                symbolName: String,
                returnType: BridgedType,
                kotlinValues: List<BridgeTypedKotlinValue<String>>,
                block: NativeTextCallback
        ): TextualStub {
            val nativeLines = mutableListOf<String>()

            val cFunctionParameters = when (platform) {
                KotlinPlatform.JVM -> mutableListOf(
                        "jniEnv" to "JNIEnv*",
                        "jclss" to "jclass"
                )
                KotlinPlatform.NATIVE -> mutableListOf()
            }

            kotlinValues.withIndex().mapTo(cFunctionParameters) {
                "p${it.index}" to it.value.type.nativeType
            }
            val joinedCParameters = cFunctionParameters.joinToString { (name, type) -> "$type $name" }
            val cReturnType = returnType.nativeType
            val cFunctionHeader = when (platform) {
                KotlinPlatform.JVM -> {
                    val funcFullName = buildString {
                        if (pkgName.isNotEmpty()) {
                            append(pkgName)
                            append('.')
                        }
                        append(jvmFileClassName)
                        append('.')
                        append(kotlinFunctionName)
                    }

                    val functionName = "Java_" + funcFullName.replace("_", "_1").replace('.', '_').replace("$", "_00024")
                    "JNIEXPORT $cReturnType JNICALL $functionName ($joinedCParameters)"
                }
                KotlinPlatform.NATIVE -> {
                    "$cReturnType $symbolName ($joinedCParameters)"
                }
            }

            nativeLines.add(cFunctionHeader + " {")

            buildNativeCodeLines(topLevelNativeScope) {
                val cExpr = block(cFunctionParameters.takeLast(kotlinValues.size).map { (name, _) -> name })
                if (returnType != BridgedType.VOID) {
                    out("return ($cReturnType)$cExpr;")
                }
            }.forEach {
                nativeLines.add("    $it")
            }
            if (libraryForCStubs.language == Language.OBJECTIVE_C) {
                // Prevent Objective-C exceptions from passing to Kotlin:
                nativeLines.add(1, "@try {")
                nativeLines.add("} @catch (id e) { objc_terminate(); }")
                // 'objc_terminate' will report the exception.
                // TODO: consider implementing this in bitcode generator.
            }
            nativeLines.add("}")
            return nativeLines
        }
    }

    private val nativeToKotlin = object : NativeToKotlinBridgeGenerator<KotlinTextCallback, NativeTextExpression, TextualStub, TextualStub> {
        override fun generateBridge(
                nativeBacked: NativeBacked,
                returnType: BridgedType,
                nativeValues: List<BridgeTypedNativeValue<String>>,
                block: KotlinCodeBuilder.(kotlinValues: List<KotlinTextExpression>) -> KotlinTextExpression
        ): NativeTextExpression {
            if (platform != KotlinPlatform.NATIVE) TODO()
            val kotlinFunctionName = "kniBridge${nextUniqueId++}"
            val symbolName = pkgName.replace(INVALID_CLANG_IDENTIFIER_REGEX, "_") + "_$kotlinFunctionName"
            val kotlinPart = kotlinPart(kotlinFunctionName, symbolName, returnType, nativeValues, block)
            val nativePart = nativePart(symbolName, nativeValues, returnType)
            insertNativeBridge(nativeBacked, kotlinPart, nativePart)
            return "$symbolName(${nativeValues.joinToString { it.value }})"
        }

        override fun nativePart(
                symbolName: String,
                nativeValues: List<BridgeTypedNativeValue<String>>,
                returnType: BridgedType
        ): TextualStub {
            val nativeLines = mutableListOf<String>()
            val cFunctionParameters = nativeValues.withIndex().map {
                "p${it.index}" to it.value.type.nativeType
            }
            val joinedCParameters = cFunctionParameters.joinToString { (name, type) -> "$type $name" }
            val cReturnType = returnType.nativeType
            val cFunctionHeader = "$cReturnType $symbolName($joinedCParameters)"
            nativeLines.add("$cFunctionHeader;")

            return nativeLines
        }

        override fun kotlinPart(
                kotlinFunctionName: String,
                symbolName: String,
                returnType: BridgedType,
                nativeValues: List<BridgeTypedNativeValue<String>>,
                block: KotlinTextCallback
        ): TextualStub {
            val kotlinLines = mutableListOf<String>()
            val kotlinReturnType = returnType.kotlinType.render(topLevelKotlinScope)
            val kotlinParameters = nativeValues.withIndex().map {
                "p${it.index}" to it.value.type.kotlinType
            }

            val joinedKotlinParameters = kotlinParameters.joinToString {
                "${it.first}: ${it.second.render(topLevelKotlinScope)}"
            }
            kotlinLines.add("@kotlin.native.internal.ExportForCppRuntime(${symbolName.quoteAsKotlinLiteral()})")
            kotlinLines.add("private fun $kotlinFunctionName($joinedKotlinParameters): $kotlinReturnType {")
            buildKotlinCodeLines(topLevelKotlinScope) {
                var kotlinExpr = block(kotlinParameters.map { (name, _) -> name })
                if (returnType == BridgedType.OBJC_POINTER) {
                    // The Kotlin code may lose the ownership on this pointer after returning from the bridge,
                    // so retain the pointer and autorelease it:
                    kotlinExpr = "objc_retainAutoreleaseReturnValue($kotlinExpr)"
                    // (Objective-C does the same for returned pointers).
                }
                returnResult(kotlinExpr)
            }.forEach {
                kotlinLines.add("    $it")
            }
            kotlinLines.add("}")
            return kotlinLines
        }
    }

    private var nextUniqueId = 0

    private val BridgedType.nativeType: String get() = when (platform) {
        KotlinPlatform.JVM -> when (this) {
            BridgedType.BYTE -> "jbyte"
            BridgedType.SHORT -> "jshort"
            BridgedType.INT -> "jint"
            BridgedType.LONG -> "jlong"
            BridgedType.UBYTE -> "jbyte"
            BridgedType.USHORT -> "jshort"
            BridgedType.UINT -> "jint"
            BridgedType.ULONG -> "jlong"
            BridgedType.FLOAT -> "jfloat"
            BridgedType.DOUBLE -> "jdouble"
            BridgedType.NATIVE_PTR -> "jlong"
            BridgedType.OBJC_POINTER -> TODO()
            BridgedType.VOID -> "void"
        }
        KotlinPlatform.NATIVE -> when (this) {
            BridgedType.BYTE -> "int8_t"
            BridgedType.SHORT -> "int16_t"
            BridgedType.INT -> "int32_t"
            BridgedType.LONG -> "int64_t"
            BridgedType.UBYTE -> "uint8_t"
            BridgedType.USHORT -> "uint16_t"
            BridgedType.UINT -> "uint32_t"
            BridgedType.ULONG -> "uint64_t"
            BridgedType.FLOAT -> "float"
            BridgedType.DOUBLE -> "double"
            BridgedType.NATIVE_PTR -> "void*"
            BridgedType.OBJC_POINTER -> "id"
            BridgedType.VOID -> "void"
        }
    }

    private inner class NativeBridge(val kotlinLines: List<String>, val nativeLines: List<String>)

    override fun kotlinToNative(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinTextValue>,
            independent: Boolean,
            block: NativeCodeBuilder.(nativeValues: List<NativeTextExpression>) -> NativeTextExpression
    ): KotlinTextExpression = kotlinToNative.generateBridge(nativeBacked, returnType, kotlinValues, independent, block)

    override fun nativeToKotlin(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeTextValue>,
            block: KotlinCodeBuilder.(arguments: List<KotlinTextExpression>) -> KotlinTextExpression
    ): NativeTextExpression = nativeToKotlin.generateBridge(nativeBacked, returnType, nativeValues, block)

    override fun insertNativeBridge(nativeBacked: NativeBacked, kotlinPart: List<String>, nativePart: List<String>) {
        val nativeBridge = NativeBridge(kotlinPart, nativePart)
        nativeBridges.add(nativeBacked to nativeBridge)
    }

    private val nativeBridges = mutableListOf<Pair<NativeBacked, NativeBridge>>()

    override fun prepare(): NativeTextBridges {
        val includedBridges = mutableListOf<NativeBridge>()
        val excludedClients = mutableSetOf<NativeBacked>()

        nativeBridges.map { it.second.nativeLines }
                .mapFragmentIsCompilable(libraryForCStubs)
                .forEachIndexed { index, isCompilable ->
                    if (!isCompilable) {
                        excludedClients.add(nativeBridges[index].first)
                    }
                }

        nativeBridges.mapNotNullTo(includedBridges) { (nativeBacked, nativeBridge) ->
            if (nativeBacked in excludedClients) {
                null
            } else {
                nativeBridge
            }
        }

        // TODO: exclude unused bridges.

        return object : NativeTextBridges {

            override val kotlinParts: Sequence<TextualStub>
                get() = includedBridges.asSequence().map { it.kotlinLines }

            override val nativeParts: Sequence<TextualStub>
                get() = includedBridges.asSequence().map { it.nativeLines }

            override fun isSupported(nativeBacked: NativeBacked): Boolean =
                    nativeBacked !in excludedClients
        }
    }

    companion object {
        private val INVALID_CLANG_IDENTIFIER_REGEX = "[^a-zA-Z1-9_]".toRegex()
    }
}

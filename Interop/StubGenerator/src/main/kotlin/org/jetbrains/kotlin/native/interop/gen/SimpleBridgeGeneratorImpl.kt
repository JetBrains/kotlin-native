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

class SimpleBridgeGeneratorImpl(
        private val platform: KotlinPlatform,
        private val pkgName: String,
        private val jvmFileClassName: String,
        private val libraryForCStubs: NativeLibrary,
        override val topLevelNativeScope: NativeScope,
        private val topLevelKotlinScope: KotlinScope
) : SimpleBridgeGenerator {

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
            kotlinValues: List<BridgeTypedKotlinValue>,
            independent: Boolean,
            block: NativeCodeBuilder.(nativeValues: List<NativeTextExpression>) -> NativeTextExpression
    ): KotlinTextExpression {

        val kotlinFunctionName = "kniBridge${nextUniqueId++}"

        val symbolName by lazy {
            pkgName.replace(INVALID_CLANG_IDENTIFIER_REGEX, "_") + "_$kotlinFunctionName"
        }

        val nativeLines = kotlinToNativeNativePart(kotlinFunctionName, symbolName, returnType, kotlinValues, block)
        val kotlinLines = kotlinToNativeKotlinPart(kotlinFunctionName, symbolName, returnType, kotlinValues, independent)
        val nativeBridge = NativeBridge(kotlinLines, nativeLines)
        nativeBridges.add(nativeBacked to nativeBridge)
        return "$kotlinFunctionName(${kotlinValues.joinToString { it.value }})"
    }

    private fun kotlinToNativeKotlinPart(
            kotlinFunctionName: String,
            symbolName: String,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinValue>,
            independent: Boolean
    ): List<String> {
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

    private fun kotlinToNativeNativePart(
            kotlinFunctionName: String,
            symbolName: String,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinValue>,
            block: NativeCodeBuilder.(nativeValues: List<NativeTextExpression>) -> NativeTextExpression
    ): List<String> {
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

    override fun nativeToKotlin(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeValue>,
            block: KotlinCodeBuilder.(arguments: List<KotlinTextExpression>) -> KotlinTextExpression
    ): NativeTextExpression {
        if (platform != KotlinPlatform.NATIVE) TODO()
        val kotlinFunctionName = "kniBridge${nextUniqueId++}"
        val symbolName = pkgName.replace(INVALID_CLANG_IDENTIFIER_REGEX, "_") + "_$kotlinFunctionName"
        val kotlinPart = buildNativeToKotlinKotlinPart(kotlinFunctionName, symbolName, returnType, nativeValues, block)
        val nativePart = buildNativeToKotlinNativePart(symbolName, nativeValues, returnType)
        insertNativeBridge(nativeBacked, kotlinPart, nativePart)
        return "$symbolName(${nativeValues.joinToString { it.value }})"
    }

    private fun buildNativeToKotlinNativePart(
            symbolName: String,
            nativeValues: List<BridgeTypedNativeValue>,
            returnType: BridgedType
    ): List<String> {
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

    private fun buildNativeToKotlinKotlinPart(
            kotlinFunctionName: String,
            symbolName: String,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeValue>,
            block: KotlinCodeBuilder.(arguments: List<KotlinTextExpression>) -> KotlinTextExpression
    ): List<String> {
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

    override fun insertNativeBridge(nativeBacked: NativeBacked, kotlinLines: List<String>, nativeLines: List<String>) {
        val nativeBridge = NativeBridge(kotlinLines, nativeLines)
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

            override val kotlinLines: Sequence<String>
                get() = includedBridges.asSequence().flatMap { it.kotlinLines.asSequence() }

            override val nativeLines: Sequence<String>
                get() = includedBridges.asSequence().flatMap { it.nativeLines.asSequence() }

            override fun isSupported(nativeBacked: NativeBacked): Boolean =
                    nativeBacked !in excludedClients
        }
    }

    companion object {
        private val INVALID_CLANG_IDENTIFIER_REGEX = "[^a-zA-Z1-9_]".toRegex()
    }
}

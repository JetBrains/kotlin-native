package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.ObjCIdType
import org.jetbrains.kotlin.native.interop.indexer.ObjCPointer
import org.jetbrains.kotlin.native.interop.indexer.PointerType
import org.jetbrains.kotlin.native.interop.indexer.VoidType

interface TextualContext {
    /**
     * The conversion from [TypeMirror.argType] to [bridgedType].
     */
    fun TypeInfo.argToBridged(expr: KotlinTextExpression): KotlinTextExpression = when (this) {
        is TypeInfo.Primitive -> expr
        is TypeInfo.Boolean -> "$expr.toByte()"
        is TypeInfo.Enum -> "$expr.value"
        is TypeInfo.Pointer -> "$expr.rawValue"
        is TypeInfo.ObjCPointerInfo -> "$expr.objcPtr()"
        // When passing Kotlin function as block pointer from Kotlin to native,
        // it first gets wrapped by a holder in [argToBridged],
        // and then converted to block in [cFromBridged].
        is TypeInfo.ObjCBlockPointerInfo -> "createKotlinObjectHolder($expr)"
        is TypeInfo.ByRef -> error(pointed)
    }

    /**
     * The conversion from [bridgedType] to [TypeMirror.argType].
     */
    fun TypeInfo.argFromBridged(
            expr: KotlinTextExpression,
            scope: KotlinScope,
            nativeBacked: NativeBacked
    ): KotlinTextExpression = when (this) {
        is TypeInfo.Primitive -> expr
        is TypeInfo.Boolean -> "$expr.toBoolean()"
        is TypeInfo.Enum -> scope.reference(clazz) + ".byValue($expr)"
        is TypeInfo.Pointer -> "interpretCPointer<${pointee.render(scope)}>($expr)"
        is TypeInfo.ObjCPointerInfo ->
            "interpretObjCPointerOrNull<${kotlinType.render(scope)}>($expr)" +
                if (type.isNullable) "" else "!!"
        is TypeInfo.ObjCBlockPointerInfo -> argFromBridged(expr, scope, nativeBacked)
        is TypeInfo.ByRef -> error(pointed)
    }

    fun TypeInfo.ObjCBlockPointerInfo.argFromBridged(
            expr: KotlinTextExpression,
            scope: KotlinScope,
            nativeBacked: NativeBacked
    ): KotlinTextExpression {
        val mappingBridgeGenerator = scope.mappingBridgeGenerator

        val funParameters = type.parameterTypes.mapIndexed { index, _ ->
            "p$index" to kotlinType.parameterTypes[index]
        }.joinToString { "${it.first}: ${it.second.render(scope)}" }

        val funReturnType = kotlinType.returnType.render(scope)

        val codeBuilder = KotlinCodeBuilder(scope)
        val kniBlockPtr = "kniBlockPtr"


        // Build the anonymous function expression:
        val anonymousFun = buildString {
            append("fun($funParameters): $funReturnType {\n") // Anonymous function begins.

            // As function body, generate the code which simply bridges to native and calls the block:
            mappingBridgeGenerator.kotlinToNative(
                    codeBuilder,
                    nativeBacked,
                    type.returnType,
                    type.parameterTypes.mapIndexed { index, it ->
                        TypedKotlinValue(it, "p$index")
                    } + TypedKotlinValue(PointerType(VoidType), "interpretCPointer<COpaque>($kniBlockPtr)"),
                    independent = true

            ) { nativeValues ->
                val type = type
                val blockType = blockTypeStringRepresentation(type)
                val objCBlock = "((__bridge $blockType)${nativeValues.last()})"
                "$objCBlock(${nativeValues.dropLast(1).joinToString()})"
            }.let {
                codeBuilder.returnResult(it)
            }

            codeBuilder.build().joinTo(this, separator = "\n")
            append("}") // Anonymous function ends.
        }

        val nullOutput = if (type.isNullable) "null" else "throw NullPointerException()"

        return "$expr.let { $kniBlockPtr -> if (kniBlockPtr == nativeNullPtr) $nullOutput else $anonymousFun }"
    }

    fun TypeInfo.cFromBridged(
            expr: NativeTextExpression,
            scope: NativeScope,
            nativeBacked: NativeBacked
    ): NativeTextExpression = when (this) {
        is TypeInfo.Primitive -> expr
        is TypeInfo.Boolean -> "($expr) ? 1 : 0"
        is TypeInfo.Enum -> expr
        is TypeInfo.Pointer -> "(${getPointerTypeStringRepresentation(cPointee)})$expr"
        is TypeInfo.ObjCPointerInfo -> expr
        is TypeInfo.ObjCBlockPointerInfo -> cFromBridged(expr, scope, nativeBacked)
        is TypeInfo.ByRef -> error(pointed)
    }

    fun TypeInfo.ObjCBlockPointerInfo.cFromBridged(
            expr: NativeTextExpression,
            scope: NativeScope,
            nativeBacked: NativeBacked
    ): NativeTextExpression {
        val mappingBridgeGenerator = scope.mappingBridgeGenerator

        val blockParameters = type.parameterTypes.mapIndexed { index, it ->
            "p$index" to it.getStringRepresentation()
        }.joinToString { "${it.second} ${it.first}" }

        val blockReturnType = type.returnType.getStringRepresentation()

        val kniFunction = "kniFunction"

        val codeBuilder = NativeCodeBuilder(scope)

        return buildString {
            append("({ ") // Statement expression begins.
            append("id $kniFunction = $expr; ") // Note: it gets captured below.
            append("($kniFunction == nil) ? nil : ")
            append("(id)") // Cast the block to `id`.
            append("^$blockReturnType($blockParameters) {") // Block begins.

            // As block body, generate the code which simply bridges to Kotlin and calls the Kotlin function:
            mappingBridgeGenerator.nativeToKotlin(
                    codeBuilder,
                    nativeBacked,
                    type.returnType,
                    type.parameterTypes.mapIndexed { index, it ->
                        TypedNativeValue(it, "p$index")
                    } + TypedNativeValue(ObjCIdType(ObjCPointer.Nullability.Nullable, emptyList()), kniFunction)
            ) { kotlinValues ->
                val kotlinFunctionType = kotlinType.render(this.scope)
                val kotlinFunction = "unwrapKotlinObjectHolder<$kotlinFunctionType>(${kotlinValues.last()})"
                "$kotlinFunction(${kotlinValues.dropLast(1).joinToString()})"
            }.let {
                codeBuilder.out("return $it;")
            }

            codeBuilder.lines.joinTo(this, separator = " ")

            append(" };") // Block ends.
            append(" })") // Statement expression ends.
        }
    }

    fun TypeInfo.cToBridged(expr: NativeTextExpression): NativeTextExpression = when (this) {
        is TypeInfo.Primitive -> expr
        is TypeInfo.Boolean -> "($expr) ? 1 : 0"
        is TypeInfo.Enum -> expr
        is TypeInfo.Pointer -> expr
        is TypeInfo.ObjCPointerInfo -> expr
        // When passing block pointer as Kotlin function from native to Kotlin,
        // it is converted to Kotlin function in [cFromBridged].
        is TypeInfo.ObjCBlockPointerInfo -> expr
        is TypeInfo.ByRef -> error(pointed)
    }
}
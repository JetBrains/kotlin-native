package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.FunctionDecl
import org.jetbrains.kotlin.native.interop.indexer.GlobalDecl
import org.jetbrains.kotlin.native.interop.indexer.VoidType
import org.jetbrains.kotlin.native.interop.indexer.unwrapTypedefs

internal data class CCalleeWrapper(val lines: List<String>)

/**
 * Some functions don't have an address (e.g. macros-based or builtins).
 * To solve this problem we generate a wrapper function.
 */
internal class CWrappersGenerator(private val context: StubIrContext) {

    private var currentFunctionWrapperId = 0

    private val packageName =
            context.configuration.pkgName.replace(INVALID_CLANG_IDENTIFIER_REGEX, "_")

    private fun generateFunctionWrapperName(functionName: String): String {
        return "${packageName}_${functionName}_wrapper${currentFunctionWrapperId++}"
    }

    private fun bindSymbolToFunction(symbol: String, function: String): List<String> = listOf(
            "const void* $symbol __asm(${symbol.quoteAsKotlinLiteral()});",
            "const void* $symbol = &$function;"
    )

    private fun createWrapper(
            symbolName: String,
            wrapperName: String,
            returnType: String,
            parameters: List<Pair<String, String>>,
            body: String
    ): List<String> = listOf(
            "__attribute__((always_inline))",
            "$returnType $wrapperName(${parameters.joinToString { "${it.second} ${it.first}" }}) {",
            body,
            "}",
            *bindSymbolToFunction(symbolName, wrapperName).toTypedArray()
    )

    fun generateCCalleeWrapper(function: FunctionDecl, symbolName: String): CCalleeWrapper =
            if (function.isVararg) {
                CCalleeWrapper(bindSymbolToFunction(symbolName, function.name))
            } else {
                val wrapperName = generateFunctionWrapperName(function.name)

                val returnType = function.returnType.getStringRepresentation()
                val parameters = function.parameters.mapIndexed { index, parameter ->
                    "p$index" to parameter.type.getStringRepresentation()
                }
                val callExpression = "${function.name}(${parameters.joinToString { it.first }});"
                val wrapperBody = if (function.returnType.unwrapTypedefs() is VoidType) {
                    callExpression
                } else {
                    "return $callExpression"
                }
                val wrapper = createWrapper(symbolName, wrapperName, returnType, parameters, wrapperBody)
                CCalleeWrapper(wrapper)
            }

    fun generateCGlobalGetter(globalDecl: GlobalDecl, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${globalDecl.name}_getter")
        val returnType = globalDecl.type.getStringRepresentation()
        val wrapperBody = "return ${globalDecl.name};"
        val wrapper = createWrapper(symbolName, wrapperName, returnType, emptyList(), wrapperBody)
        return CCalleeWrapper(wrapper)
    }

    fun generateCGlobalSetter(globalDecl: GlobalDecl, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${globalDecl.name}_setter")
        val wrapperBody = "${globalDecl.name} = p1;"
        val globalType = globalDecl.type.getStringRepresentation()
        val wrapper = createWrapper(symbolName, wrapperName, "void", listOf(globalType to "p1"), wrapperBody)
        return CCalleeWrapper(wrapper)
    }
}

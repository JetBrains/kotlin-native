package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.FunctionDecl
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
            "const void* $symbolName __asm(${symbolName.quoteAsKotlinLiteral()});",
            "const void* $symbolName = &$wrapperName;"
    )

    fun generateCCalleeWrapper(function: FunctionDecl, symbolName: String): CCalleeWrapper =
            if (function.isVararg) {
                CCalleeWrapper(emptyList())
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

    fun generateCGlobalGetter(getterInfo: BridgeGenerationComponents.GlobalGetterBridgeInfo, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${getterInfo.cGlobalName}_getter")
        val returnType = getterInfo.typeInfo.bridgedType.getNativeType(context.platform)
        val wrapperBody = "return ${getterInfo.cGlobalName};"
        val wrapper = createWrapper(symbolName, wrapperName, returnType, emptyList(), wrapperBody)
        return CCalleeWrapper(wrapper)
    }

    fun generateCGlobalSetter(setterInfo: BridgeGenerationComponents.GlobalSetterBridgeInfo, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${setterInfo.cGlobalName}_setter")
        val wrapperBody = "${setterInfo.cGlobalName} = p1;"
        val globalType = setterInfo.typeInfo.bridgedType.getNativeType(context.platform)
        val wrapper = createWrapper(symbolName, wrapperName, "void", listOf(globalType to "p1"), wrapperBody)
        return CCalleeWrapper(wrapper)
    }
}

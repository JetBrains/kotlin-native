package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.ir.classIfConstructor
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

/**
 * Encodes information about what type parameters were replaced with primitive types
 * and injects it into specialization name.
 *
 * The mapping is as follows:
 * - type parameter wasn't replaced   -> 1
 * - type parameter was replaced      -> 2 to 9 depending on concrete type (there are 8 at all).
 */
internal class SpecializationEncoder(val context: Context) {
    private val primitiveTypes = context.irBuiltIns.primitiveIrTypes
    private val primitiveTypeEncodings = context.irBuiltIns.primitiveIrTypes
            .mapIndexed { index, type -> type to (index + 2) }
            .toMap()
    private val radix = primitiveTypeEncodings.size + 2

    fun encode(declaration: IrTypeParametersContainer, mapping: Map<IrTypeParameterSymbol, IrType>): String? {
        return getCode(declaration, mapping)?.let { "${declaration.nameForIrSerialization}-$it" }
    }

    fun encode(declaration: IrTypeParametersContainer, types: List<IrType?>): String? {
        var num = 0
        types.forEach { type ->
            num *= radix
            num += if (type == null) {
                1
            } else {
                primitiveTypeEncodings.getOrElse(type) { return null }
            }
        }
        return "${declaration.nameForIrSerialization}-$num"
    }

    private fun getCode(declaration: IrTypeParametersContainer, mapping: Map<IrTypeParameterSymbol, IrType>): Int? {
        var num = 0
        for (typeParameter in declaration.typeParameters) {
            num *= radix
            mapping[typeParameter.symbol]?.let { type ->
                num += primitiveTypeEncodings.getOrElse(type) { return null }
            } ?: num++
        }
        return num
    }

    /*
     * null as element means type parameter with that index were not replaced.
     * Other elements are expected to be primitives.
     */
    fun decode(name: String): List<IrType?> {
        var num = name.split("-").last().toIntOrNull() ?: return emptyList()
        val result = mutableListOf<IrType?>()
        while (num > 0) {
            when (val nextDecodedNumber = num % radix) {
                1 -> result.add(null)
                else -> result.add(primitiveTypes[nextDecodedNumber - 2])
            }
            num /= radix
        }
        require(result.all { it == null || it in primitiveTypes })
        return result.reversed()
    }
}

class RequestedSpecializationData(
        val annotatedTypeParameters: List<IrTypeParameter>,
        val possibleCombinations: List<List<IrType?>>
)

fun IrClass.extractSpecializationTypes(): RequestedSpecializationData {
    val annotation = annotations.find { it.symbol.owner.classIfConstructor.nameForIrSerialization.asString() == "SpecializedClass" }
            ?: return RequestedSpecializationData(emptyList(), emptyList())
    val types = (annotation.getValueArgument(0) as IrVararg).elements.map { (it as IrClassReference).classType }
    return RequestedSpecializationData(typeParameters, getAllPossibleTypeCombinations(List(typeParameters.size) { types }))
}

fun IrFunction.extractSpecializationTypes(): RequestedSpecializationData {
    val annotatedTypeParameters = mutableListOf<IrTypeParameter>()
    fun IrTypeParameter.getRequestedSpecializationTypes(): List<IrType?> {
        val annotation = annotations.find { it.symbol.owner.classIfConstructor.nameForIrSerialization.asString() == "Specialized" }
                ?: return emptyList()
        annotatedTypeParameters += this
        val types: List<IrType?> = (annotation.getValueArgument(0) as IrVararg).elements.map { (it as IrClassReference).classType }
        return types.plus(null as IrType?)
    }
    return RequestedSpecializationData(annotatedTypeParameters, getAllPossibleTypeCombinations(List(typeParameters.size) { typeParameters[it].getRequestedSpecializationTypes() }.filter { it.isNotEmpty() }))
}

// Cross product of given lists:
// [[A, null, B], [null, C]] --> [[A, null], [A, C], [null, null], [null, C], [B, null], [B, C]]
private fun getAllPossibleTypeCombinations(lists: List<List<IrType?>>): List<List<IrType?>> {
    fun getTypeCombinations(list1: List<List<IrType?>>, list2: List<IrType?>): List<List<IrType?>> {
        val result = mutableListOf<List<IrType?>>()
        for (types in list1) {
            for (newType in list2) {
                result += types + newType
            }
        }
        return result
    }
    return when (lists.size) {
        0 -> emptyList()
        else -> {
            var result = lists.first().map { listOf(it) }
            lists.drop(1).forEach {
                result = getTypeCombinations(result, it)
            }
            result
        }
    }
}
package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

/**
 * Encodes information about what type parameters were replaced with primitive types
 * and injects it into specialization name.
 *
 * The mapping is as follows:
 * - type parameter weren't replaced    -> 1
 * - type parameter were replaced       -> 2 to 9 depending on concrete type (there are 8 at all).
 */
internal class SpecializationEncoder(val context: Context) {
    private val primitiveTypes = context.irBuiltIns.primitiveIrTypes
    private val primitiveTypeEncodings = context.irBuiltIns.primitiveIrTypes
            .mapIndexed { index, type -> type to (index + 2) }
            .toMap()
    private val radix = primitiveTypeEncodings.size + 2

    fun encode(declaration: IrTypeParametersContainer, mapping: Map<IrTypeParameterSymbol, IrType>): String {
        var num = 0
        for (typeParameter in declaration.typeParameters) {
            num *= radix
            mapping[typeParameter.symbol]?.let { type ->
                num += primitiveTypeEncodings.getOrElse(type) { throw AssertionError() }
            } ?: num++
        }
        return "${declaration.nameForIrSerialization}-$num"
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
        return result
    }
}
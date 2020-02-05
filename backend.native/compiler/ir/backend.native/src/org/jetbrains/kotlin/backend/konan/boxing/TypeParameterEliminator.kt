package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.lower.DeepCopyIrTreeWithSymbolsForInliner
import org.jetbrains.kotlin.backend.konan.lower.SpecializationTransformer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.name.Name

// Expected that transformer will not change entities of given IR elements, only internals,
// i.e. for receiver of type T it will return an element of type T.
@Suppress("UNCHECKED_CAST")
internal fun <T : IrElement> T.eliminateTypeParameters(transformer: TypeParameterEliminator): T = transformer.copy(this) as T

/**
 * Replaces certain type parameters with concrete types. Type parameters that must be removed
 * stored in [typeParameterMapping], where each parameter is associated with corresponding concrete type.
 *
 * This class reuses copy algorithms for inliner (@see [DeepCopyIrTreeWithSymbolsForInliner]),
 * because inliner does the same work (it must replace type parameters too).
 *
 * But it has two differences.
 *
 * First, it provides its own [SymbolRenamer] to add concrete type names to the names of generated functions.
 *
 * Second, it is intended to be executed many times. Single non-recursive elimination pass can make
 * some calls fit for further specialization, so it is reasonable to track such calls and pass it back to
 * [specializationTransformer].
 * Consider the following example:
 *
 * ```
 * fun <T> id(x: T) = x
 * fun <U> id2(x: U) = id(x)
 *
 * // ...
 *
 * id2(41)
 * ```
 *
 * The first pass will result in the following code:
 *
 * ```
 * fun <T> id(x: T) = x
 * fun <U> id2(x: U) = id(x)
 * fun id2-Int(x: Int) = id(x)
 * // ...
 *
 * id2-Int(41)
 * ```
 *
 * where the `id` call from `id2-Int` is likely to be specialized.
 * So it's up to specialization transformer to do something with that call.
 *
 * The recursive approach (handle call right after it was encountered) is feasible
 * but error prone due to rigorous workaround for descriptors, symbols, and parents.
 */
internal class TypeParameterEliminator(private val specializationTransformer: SpecializationTransformer,
                                       private val typeParameterMapping: Map<IrTypeParameterSymbol, IrType>,
                                       context: Context,
                                       parent: IrDeclarationParent)
    : DeepCopyIrTreeWithSymbolsForInliner(context, typeParameterMapping, parent) {

    private val callsFitForSpecialization = mutableSetOf<IrCall>()

    override fun copy(irElement: IrElement): IrElement {
        val result = super.copy(irElement)
        specializationTransformer.handleTransformedCalls(callsFitForSpecialization)
        return result
    }

    private val newFunctionNames = mutableMapOf<IrSymbol, Name>()
    fun addNewFunctionName(function: IrFunction, name: String) {
        newFunctionNames[function.symbol] = Name.identifier(name)
    }

    private inner class TypeParameterEliminatorSymbolRenamer : SymbolRenamer {
        override fun getClassName(symbol: IrClassSymbol) = symbol.owner.name
        override fun getFunctionName(symbol: IrSimpleFunctionSymbol) = newFunctionNames[symbol] ?: symbol.owner.name
        override fun getFieldName(symbol: IrFieldSymbol) = symbol.owner.name
        override fun getFileName(symbol: IrFileSymbol) = symbol.owner.fqName
        override fun getExternalPackageFragmentName(symbol: IrExternalPackageFragmentSymbol) = symbol.owner.fqName
        override fun getEnumEntryName(symbol: IrEnumEntrySymbol) = symbol.owner.name
        override fun getVariableName(symbol: IrVariableSymbol) = symbol.owner.name
        override fun getTypeParameterName(symbol: IrTypeParameterSymbol) = symbol.owner.name
        override fun getValueParameterName(symbol: IrValueParameterSymbol) = symbol.owner.name
    }

    override val copier: DeepCopyIrTreeWithSymbols = object : DeepCopyIrTreeWithSymbols(
            symbolRemapper,
            typeRemapper,
            TypeParameterEliminatorSymbolRenamer()
    ) {
        override fun visitClass(declaration: IrClass): IrClass {
            return super.visitClass(declaration).withEliminatedTypeParameters(declaration)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
            return super.visitSimpleFunction(declaration).withEliminatedTypeParameters(declaration)
        }

        override fun visitCall(expression: IrCall): IrCall {
            return super.visitCall(expression).also {
                if (expression.satisfiesSpecializationCondition(typeParameterMapping)) {
                    callsFitForSpecialization.add(it)
                }
            }
        }

        private fun <E : IrTypeParametersContainer> E.withEliminatedTypeParameters(declaration: E): E = apply {
            require(declaration.typeParameters.size == typeParameters.size) {
                "Must be called only for origin and its copy," +
                        " but origin has ${declaration.typeParameters.size} type parameters," +
                        " and copy has ${typeParameters.size}"
            }
            val typeParametersToEliminate = typeParameters.filterIndexed { index, _ ->
                declaration.typeParameters[index].symbol in typeParameterMapping
            }
            typeParameters.removeAll(typeParametersToEliminate)
        }

        private fun IrCall.satisfiesSpecializationCondition(typeParameterMapping: Map<IrTypeParameterSymbol, IrType?>): Boolean {
            if (typeArgumentsCount != 1) return false
            val symbol = getTypeArgument(0)?.classifierOrNull
            return symbol in typeParameterMapping && typeParameterMapping[symbol]?.isPrimitiveType() == true
        }
    }
}
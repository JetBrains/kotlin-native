package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.lower.DeepCopyIrTreeWithSymbolsForInliner
import org.jetbrains.kotlin.backend.konan.lower.SpecializationTransformer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
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
                                       private val specializationEncoder: SpecializationEncoder,
                                       context: Context,
                                       parent: IrDeclarationParent)
    : DeepCopyIrTreeWithSymbolsForInliner(context, typeParameterMapping, parent) {

    private val callsFitForSpecialization = mutableSetOf<IrCall>()
    private val specializingClasses = mutableSetOf<IrClass>()

    override fun copy(irElement: IrElement): IrElement {
        if (irElement is IrClass) {
            specializingClasses += irElement
        }
        val result = super.copy(irElement)
        specializationTransformer.handleTransformedCalls(callsFitForSpecialization)
        return result
    }

    private val newFunctionNames = mutableMapOf<IrSymbol, Name>()
    private val newClassNames = mutableMapOf<IrSymbol, Name>()
    fun <T : IrTypeParametersContainer> addNewDeclarationName(declaration: T, name: String) {
        when (declaration) {
            is IrClass -> newClassNames[declaration.symbol] = Name.identifier(name)
            is IrFunction -> newFunctionNames[declaration.symbol] = Name.identifier(name)
        }
    }

    private inner class TypeParameterEliminatorSymbolRenamer : SymbolRenamer {
        override fun getClassName(symbol: IrClassSymbol) = newClassNames[symbol] ?: symbol.owner.name
        override fun getFunctionName(symbol: IrSimpleFunctionSymbol) = newFunctionNames[symbol] ?: symbol.owner.name
        override fun getFieldName(symbol: IrFieldSymbol) = symbol.owner.name
        override fun getFileName(symbol: IrFileSymbol) = symbol.owner.fqName
        override fun getExternalPackageFragmentName(symbol: IrExternalPackageFragmentSymbol) = symbol.owner.fqName
        override fun getEnumEntryName(symbol: IrEnumEntrySymbol) = symbol.owner.name
        override fun getVariableName(symbol: IrVariableSymbol) = symbol.owner.name
        override fun getTypeParameterName(symbol: IrTypeParameterSymbol) = symbol.owner.name
        override fun getValueParameterName(symbol: IrValueParameterSymbol) = symbol.owner.name
    }

    /*
     * Removes obsolete type arguments from types that refer to specialized declarations
     * (e.g. type arguments in `this` type and constructors return types).
     */
    private inner class EliminatorTypeRemapper(symbolRemapper: SymbolRemapper) : InlinerTypeRemapper(symbolRemapper, typeParameterMapping) {
        override fun remapType(type: IrType): IrType {
            val newType = super.remapType(type)
            if (newType !is IrSimpleType || newType.classOrNull?.isBound != true) return newType

            val newTypeOwner = newType.classOrNull?.owner!!
            if (type.classOrNull?.owner in specializingClasses) {
                val types = specializationEncoder.decode(newTypeOwner.nameForIrSerialization.asString())
                return IrSimpleTypeImpl(
                        newType.classifier,
                        newType.hasQuestionMark,
                        remapTypeArguments(newType.arguments, types),
                        newType.annotations,
                        newType.abbreviation
                )
            }
            return newType
        }

        private fun remapTypeArguments(arguments: List<IrTypeArgument>, types: List<IrType?>): List<IrTypeArgument> {
            require(arguments.size == types.size) {
                "Expected ${arguments.size} elements but specialization has ${types.size}"
            }
            return arguments.filterIndexed { index, _ -> types[index] == null }
                    .map { argument ->
                        (argument as? IrTypeProjection)?.let { makeTypeProjection(remapType(it.type), it.variance) }
                                ?: argument
                    }
        }
    }

    internal inner class EliminatingCopier : DeepCopyIrTreeWithSymbols(
            symbolRemapper,
            EliminatorTypeRemapper(symbolRemapper),
            TypeParameterEliminatorSymbolRenamer()
    ) {
        private val constructorsCopier = ConstructorsCopier()

        override fun visitClass(declaration: IrClass): IrClass {
            constructorsCopier.prepare(declaration)
            return super.visitClass(declaration).withEliminatedTypeParameters(declaration)
        }

        override fun visitConstructor(declaration: IrConstructor): IrConstructor {
            return constructorsCopier.getCopied(declaration)
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

        private inner class ConstructorsCopier {
            private val constructorsInDelegationOrder = mutableListOf<IrConstructor>()
            private val copiedConstructors = mutableMapOf<IrConstructor, IrConstructor>()

            fun prepare(declaration: IrClass) {
                with (constructorsInDelegationOrder) {
                    clear()
                    addAll(declaration.constructors.sortedWith(ConstructorDelegationPartialOrder(declaration)))
                    copiedConstructors.clear()
                }
            }

            fun getCopied(oldConstructor: IrConstructor): IrConstructor {
                if (copiedConstructors.isEmpty()) {
                    copiedConstructors.putAll(constructorsInDelegationOrder.map {
                        it to super@EliminatingCopier.visitConstructor(it).also { ctor ->
                            (ctor.descriptor as WrappedClassConstructorDescriptor).bind(ctor)
                        }
                    })
                }
                return copiedConstructors[oldConstructor]!!
            }
        }
    }

    override val copier = EliminatingCopier()
}

class ConstructorDelegationPartialOrder(clazz: IrClass) : Comparator<IrConstructor> {
    private val constructorsAndDelegates = clazz.constructors
            .map {
                it to it.body?.statements?.filterIsInstance<IrDelegatingConstructorCall>()?.single()?.symbol?.owner
            }
            .toMap()

    // c1 < c2  => c2 transitively delegates to c1
    // c1 == c2 => neither constructor delegates to another (or they're the same)
    override fun compare(c1: IrConstructor?, c2: IrConstructor?): Int {
        if (c1 == null || c2 == null || c1 === c2) return 0
        if (c1.constructedClass !== c2.constructedClass) return 0
        val delegate1 = constructorsAndDelegates[c1]
        val delegate2 = constructorsAndDelegates[c2]
        if (delegate1 == null && delegate2 == null) return 0
        if (delegate1 === c2) return 1
        if (delegate2 === c1) return -1
        return 0
    }
}
package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.descriptors.WrappedPropertyGetterDescriptor
import org.jetbrains.kotlin.backend.konan.lower.NewSymbolRemapper
import org.jetbrains.kotlin.backend.konan.lower.SafeWrappedDescriptorPatcher
import org.jetbrains.kotlin.backend.konan.lower.SpecializationTransformer
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import kotlin.collections.set

/**
 * Replaces type parameters with concrete types in scope which this visitor was called on.
 */
internal class TypeParameterEliminator(
        private val specializationTransformer: SpecializationTransformer,
        private val symbolRemapper: NewSymbolRemapper,
        symbolRenamer: SymbolRenamer,
        private val typeParametersMapping: MutableMap<IrTypeParameterSymbol, IrType>,
        typeRemapper: TypeRemapper = TypeRemapperWithParametersElimination(symbolRemapper, typeParametersMapping)
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {

    init {
        // TODO refactor
        (typeRemapper as? TypeRemapperWithParametersElimination)?.let {
            it.deepCopy = this
        }
    }

    fun addMapping(typeParameter: IrTypeParameter, concreteType: IrType) {
        typeParametersMapping[typeParameter.symbol] = concreteType
    }

    private fun <E : IrTypeParametersContainer> E.withEliminatedTypeParameters(): E = apply {
        typeParameters.removeIf { it.symbol in typeParametersMapping }
    }

    /**
     * After elimination, it may turn out that this call satisfies condition for the callee to get specialization.
     *
     * For example:
     *
     * ```
     * fun <T> id(x: T) = x
     * fun <U> id2(y: U) = id(y)
     * ...
     * id2(42)
     * ```
     *
     * After elimination of type parameter U in `id2-Int` it turns out that `id(y)` is a call to generic function
     * with primitive type argument, so it makes sense to specialize function `id` too, recursively.
     */
    override fun visitCall(expression: IrCall): IrCall {
        val newCall = if (expression.satisfiesSpecializationCondition(typeParametersMapping)) {
            symbolRemapper.inIgnoreFunctionMappingMode(expression.symbol.owner) { super.visitCall(expression) }
        } else {
            super.visitCall(expression)
        }
        return newCall.transform(specializationTransformer, null) as IrCall
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        return super.visitSimpleFunction(declaration).withEliminatedTypeParameters().apply {
            // Little crutch -- assuming that function does not have parent iff it's new (e.g. a specialization).
            // TODO elaborate on it later
            try {
                parent
            } catch (_: UninitializedPropertyAccessException) {
                parent = declaration.parent
            }
            acceptVoid(SafeWrappedDescriptorPatcher)
            if (descriptor is WrappedPropertyGetterDescriptor) {
                this.correspondingPropertySymbol = declaration.correspondingPropertySymbol
            }
        }
    }

    private fun IrCall.satisfiesSpecializationCondition(typeParameterMapping: MutableMap<IrTypeParameterSymbol, IrType>): Boolean {
        if (typeArgumentsCount != 1) return false
        val symbol = getTypeArgument(0)?.classifierOrNull
        return symbol in typeParameterMapping && typeParameterMapping[symbol]?.isPrimitiveType() == true
    }
}

/**
 * Copy of [DeepCopyTypeRemapper] with additional remapping for eliminated type parameters.
 * TODO provide delegate
 */
class TypeRemapperWithParametersElimination(
        private val symbolRemapper: SymbolRemapper,
        private val typeParametersMapping: MutableMap<IrTypeParameterSymbol, IrType>
) : TypeRemapper {

    lateinit var deepCopy: DeepCopyIrTreeWithSymbols

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        // TODO
    }

    override fun leaveScope() {
        // TODO
    }

    override fun remapType(type: IrType): IrType =
            if (type !is IrSimpleType)
                type.substitute(typeParametersMapping)
            else
                with (type) {
                    IrSimpleTypeImpl(
                            null,
                            symbolRemapper.getReferencedClassifier(type.classifier),
                            hasQuestionMark,
                            arguments.map { remapTypeArgument(it) },
                            annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
                            abbreviation?.remapTypeAbbreviation()
                    ).run {
                        val originalClassifier = type.classifier
                        if (classifier !is IrTypeParameterSymbol || originalClassifier !is IrTypeParameterSymbol || !typeParametersMapping.containsKey(originalClassifier)) {
                            return@run this
                        }
                        typeParametersMapping[classifier as IrTypeParameterSymbol] = typeParametersMapping[originalClassifier]!!
                        substitute(typeParametersMapping)
                    }
                }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
            if (typeArgument is IrTypeProjection)
                makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
            else
                typeArgument

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
            IrTypeAbbreviationImpl(
                    symbolRemapper.getReferencedTypeAlias(typeAlias),
                    hasQuestionMark,
                    arguments.map { remapTypeArgument(it) },
                    annotations
            )
}
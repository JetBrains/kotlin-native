package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.konan.lower.SafeWrappedDescriptorPatcher
import org.jetbrains.kotlin.backend.konan.lower.SpecializationTransformer
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
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
        symbolRemapper: SymbolRemapper,
        symbolRenamer: SymbolRenamer,
        private val typeParametersMapping: MutableMap<IrTypeParameterSymbol, IrType>
) : DeepCopyIrTreeWithSymbols(symbolRemapper, TypeRemapperWithParametersElimination(symbolRemapper, typeParametersMapping), symbolRenamer) {

    private fun <E : IrTypeParametersContainer> E.withEliminatedTypeParameters(): E = apply {
        typeParameters.removeIf { it.symbol in typeParametersMapping }
    }

    override fun visitCall(expression: IrCall): IrCall {
        return super.visitCall(expression).transform(specializationTransformer, null) as IrCall
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
        }
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
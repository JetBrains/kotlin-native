package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.SpecializationEncoder
import org.jetbrains.kotlin.backend.konan.boxing.TypeParameterEliminator
import org.jetbrains.kotlin.backend.konan.boxing.eliminateTypeParameters
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.Variance
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

internal class GenericsSpecialization(val context: Context) : FileLoweringPass {

    private val transformer = SpecializationTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transform(transformer, null)
        while (transformer.checkTransformedCalls()) {
            irFile.transform(transformer, null)
        }
    }
}

/**
 * Finds calls to generic functions with primitive type arguments and creates specialized versions
 * of these functions (i.e. copies of functions which have some type parameters erased and replaced
 * with concrete primitive types) to try to reduce extra boxings.
 *
 * It will reduce boxings in some cases. For example:
 *
 * ```
 * fun <T> foo(x: T) = x
 * ```
 *
 * Specialized version for type [Int] can be the following:
 *
 * ```
 * fun foo(x: Int) = x
 * ```
 *
 * Therefore, calls `foo(1)` or `foo(4103934)` will not require box their arguments.
 *
 * On the other hand, specialization for the following function:
 *
 * ```
 * fun <T> foo(x: T) {
 *     when (x) {
 *         is String -> {}
 *         is List<*> -> {}
 *         is Int -> {}
 *     }
 * }
 * ```
 *
 * and the type [Int] will require 1 box for each `is` call.
 * Generic function requires 1 box at all -- variable `x` must be boxed before the function will be called.
 * Therefore, specialization here can significantly increase total number of box operations.
 */
internal class SpecializationTransformer(val context: Context): IrBuildingTransformer(context) {

    companion object {
        private val currentSpecializations = mutableMapOf<Pair<IrTypeParametersContainer, IrType>, IrTypeParametersContainer>()

        // scope -> specialization -> origin
        private val newSpecializations = mutableMapOf<IrDeclarationParent, MutableMap<IrTypeParametersContainer, IrTypeParametersContainer>>()

        private val typeParametersMapping = mutableMapOf<IrTypeParameterSymbol, IrType>()
    }

    // Dumb way to process calls that are likely to be specialized.
    // It causes to pass the whole file every time the previous pass ended with this flag being set.
    // TODO find smarter way to handle such calls, or decide the current way is smart enough
    private var hasCallsFitForSpecialization: Boolean = false

    private val encoder = SpecializationEncoder(context)

    fun checkTransformedCalls(): Boolean {
        val result = hasCallsFitForSpecialization
        hasCallsFitForSpecialization = false
        return result
    }

    fun handleTransformedCalls(transformedCalls: Set<IrCall>) {
        hasCallsFitForSpecialization = hasCallsFitForSpecialization or transformedCalls.isNotEmpty()
    }

    override fun visitFile(declaration: IrFile): IrFile {
        return super.visitFile(declaration).also {
            addNewFunctionsToIr(declaration)
            newSpecializations.forEach { (scope, _) ->
                addNewFunctionsToIr(scope)
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration).also {
            addNewFunctionsToIr(declaration)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.typeArgumentsCount != 1 || expression.getTypeArgument(0) !in context.irBuiltIns.primitiveIrTypes) {
            return super.visitCall(expression)
        }
        val primitiveTypeArgument = expression.getTypeArgument(0)!!
        val owner: IrFunction = expression.symbol.owner
        if (owner.isInline || owner.typeParameters.size != 1) {
            return super.visitCall(expression)
        }
        val typeParameter = owner.typeParameters.first()
        if (typeParameter.superTypes.size != 1 || typeParameter.superTypes.first() != context.irBuiltIns.anyNType || typeParameter.variance != Variance.INVARIANT) {
            return super.visitCall(expression)
        }
        val copier = TypeParameterEliminator(
                this@SpecializationTransformer,
                typeParametersMapping,
                encoder,
                context,
                owner.parent
        )
        val newFunction = owner.getSpecialization(primitiveTypeArgument, copier)
        builder.at(expression).run {
            irCall(newFunction).apply {
                extensionReceiver = expression.extensionReceiver?.eliminateTypeParameters(copier)
                dispatchReceiver = expression.dispatchReceiver
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i)?.eliminateTypeParameters(copier))
                }
                return this
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        if (!expression.symbol.owner.isPrimary) {
            return super.visitConstructorCall(expression)
        }
        val primitiveTypeSubstitutionMap = expression.typeSubstitutionMap
                .filterValues { it in context.irBuiltIns.primitiveIrTypes }
        if (primitiveTypeSubstitutionMap.size != 1) {
            return super.visitConstructorCall(expression)
        }
        val oldClass = expression.symbol.owner.constructedClass
        val copier = TypeParameterEliminator(
                this@SpecializationTransformer,
                typeParametersMapping,
                encoder,
                context,
                oldClass.parent
        )
        val newConstructor = oldClass.getSpecialization(primitiveTypeSubstitutionMap, copier).primaryConstructor!!

        builder.at(expression).run {
            irConstructorCall(expression, newConstructor).apply {
                return this
            }
        }
    }

    private inline fun <reified T : IrTypeParametersContainer> T.getSpecialization(primitiveTypeSubstitutionMap: Map<IrTypeParameterSymbol, IrType>, copier: TypeParameterEliminator): T {
        // TODO: now assuming that mapping must contain exactly 1 entry
        val (typeParameterSymbol, actualType) = primitiveTypeSubstitutionMap.entries.first()

        val existingSpecialization = currentSpecializations[this to actualType]
        if (existingSpecialization is T) {
            return existingSpecialization
        }
        copier.addNewDeclarationName(this, encoder.encode(this, primitiveTypeSubstitutionMap))
        typeParametersMapping[typeParameterSymbol] = actualType

        return eliminateTypeParameters(copier).also {
            currentSpecializations[this to actualType] = it
            newSpecializations.getOrPut(parent) { mutableMapOf() }[it] = this
        }
    }

    // TODO generalize
    private fun IrFunction.getSpecialization(type: IrType, copier: TypeParameterEliminator): IrFunction {
        if (this !is IrSimpleFunction) {
            return this
        }
        val function = currentSpecializations[this to type]
        if (function is IrFunction) { // also implies that function != null
            return function
        }

        // Include concrete primitive type to the name of specialization
        // to be able to easily spot such functions and to avoid extra overloads
        copier.addNewDeclarationName(this, encoder.encode(this, mapOf(typeParameters.first().symbol to type)))

        typeParametersMapping[typeParameters.first().symbol] = type

        return eliminateTypeParameters(copier).also {
            currentSpecializations[this to type] = it
            newSpecializations.getOrPut(parent) { mutableMapOf() }[it] = this
        }
    }

    /**
     * Adds new functions to given [parent]. This function must not be called
     * when visiting members inside the scope of [parent],
     * otherwise [ConcurrentModificationException] may be thrown.
     */
    private fun addNewFunctionsToIr(parent: IrDeclarationParent) {
        newSpecializations[parent]?.forEach { (specialization, origin) ->
            when (val scope = origin.parent) {
                is IrDeclarationContainer -> scope.addChild(specialization)
                is IrStatementContainer -> {
                    val originIndex = scope.statements.indexOf(origin)
                    scope.statements.add(originIndex + 1, specialization)
                    specialization.parent = scope
                }
                is IrFunction -> {
                    val builder = context.createIrBuilder(scope.symbol)
                    val statements = scope.body?.statements?.toMutableList()
                    if (statements == null) {
                        scope.body = builder.irBlockBody {
                            +specialization
                        }
                    } else {
                        statements.add(statements.indexOf(origin), specialization)
                        specialization.parent = scope
                    }
                    scope.body = builder.irBlockBody(scope) {
                        statements?.forEach {
                            +it
                        }
                    }
                }
                else -> TODO()
            }
        }
        newSpecializations[parent]?.clear()
    }
}
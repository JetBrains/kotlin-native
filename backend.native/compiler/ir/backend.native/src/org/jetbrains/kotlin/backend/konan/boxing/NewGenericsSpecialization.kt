package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.statements
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

internal class NewGenericsSpecialization(val context: Context) : FileLoweringPass {

    private val transformer = NewSpecializationTransformer(context)

    companion object {
        private val mappedDeclarations = mutableMapOf<Pair<IrTypeParametersContainer, List<IrType?>>, IrTypeParametersContainer>()
    }

    init {
        with (context.ir.symbols) {
            PrimitiveType.values().forEach { primitiveType ->
                val irPrimitiveType = context.irBuiltIns.primitiveTypeToIrType[primitiveType] ?: error("No type defined for $primitiveType")
                val irPrimitiveTypeArray = primitiveArrays[primitiveType] ?: error("No array defined for $primitiveType")
                val irPrimitiveTypeArrayOf = primitiveArrayOfByType[primitiveType] ?: error("No arrayOf defined for $primitiveType")
                IrOriginToSpec.newSpec(array.owner, listOf(irPrimitiveType), irPrimitiveTypeArray.owner)
                // TODO supposed that arrays have the same order of members
                array.owner.declarations.zip(irPrimitiveTypeArray.owner.declarations).forEach { (origin, spec) ->
                    IrOriginToSpec.newSpec(origin, listOf(irPrimitiveType), spec)
                }
                IrOriginToSpec.newSpec(arrayOf.owner, listOf(irPrimitiveType), irPrimitiveTypeArrayOf.owner)
            }
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(transformer, null)
    }

    class NewSpecializationTransformer(val context: Context) : IrBuildingTransformer(context) {

        companion object {
            // scope -> specialization -> origin
            private val newSpecializations = mutableMapOf<IrDeclarationParent, MutableMap<IrTypeParametersContainer, IrTypeParametersContainer>>()

            private val typeParametersMapping = mutableMapOf<IrTypeParameterSymbol, IrType>()

            private val localTypeParametersMapping = mutableMapOf<IrTypeParameterSymbol, IrType?>()
        }

        private val encoder = SpecializationEncoder(context)

        override fun visitFile(declaration: IrFile): IrFile {
            return super.visitFile(declaration).also {
                addNewFunctionsToIr(declaration)
                newSpecializations.forEach { (scope, _) ->
                    addNewFunctionsToIr(scope)
                }
            }
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.produceSpecializations()
            return super.visitClass(declaration)
        }

        override fun visitFunction(declaration: IrFunction): IrStatement {
            declaration.produceSpecializations()
            return super.visitFunction(declaration)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
            if (expression.type.getClass()?.name?.asString() == "Function1") {
                generateFunction1Spec(expression)
            }
            if (expression.type.getClass()?.name?.asString() == "Function2") {
                generateFunction2Spec(expression)
            }
            return super.visitFunctionExpression(expression)
        }

        private fun generateFunction1Spec(expression: IrFunctionExpression) {
            val declaration = expression.type.getClass()!!
            val mapping1 = mapOf(
                    declaration.typeParameters[0].symbol to context.irBuiltIns.intType,
                    declaration.typeParameters[1].symbol to context.irBuiltIns.intType
            )
            val concreteTypes1 = listOf(
                    context.irBuiltIns.intType, context.irBuiltIns.intType
            )
            val mapping2 = mapOf(
                    declaration.typeParameters[0].symbol to context.irBuiltIns.intType,
                    declaration.typeParameters[1].symbol to context.irBuiltIns.booleanType
            )
            val concreteTypes2 = listOf(
                    context.irBuiltIns.intType, context.irBuiltIns.booleanType
            )
            if (!mappedDeclarations.containsKey(declaration to concreteTypes1)) {
                // Function1 <: Function
                generateFunctionSpec(declaration.superClasses[0].owner)
                declaration.produceOneSpecialization(mapping1, concreteTypes1)
                declaration.produceOneSpecialization(mapping2, concreteTypes2)
            }
        }

        private fun generateFunction2Spec(expression: IrFunctionExpression) {
            val declaration = expression.type.getClass()!!
            val mapping = mapOf(
                    declaration.typeParameters[0].symbol to context.irBuiltIns.intType,
                    declaration.typeParameters[1].symbol to context.irBuiltIns.intType,
                    declaration.typeParameters[2].symbol to context.irBuiltIns.intType
            )
            val concreteTypes = listOf(
                    context.irBuiltIns.intType, context.irBuiltIns.intType, context.irBuiltIns.intType
            )
            if (!mappedDeclarations.containsKey(declaration to concreteTypes)) {
                // Function1 <: Function
                declaration.produceOneSpecialization(mapping, concreteTypes)
            }
        }

        private fun generateFunctionSpec(declaration: IrClass) {
            val mapping = mapOf(
                    declaration.typeParameters[0].symbol to context.irBuiltIns.intType
            )
            val concreteTypes = listOf(
                    context.irBuiltIns.intType
            )
            if (!mappedDeclarations.containsKey(declaration to concreteTypes)) {
                declaration.produceOneSpecialization(mapping, concreteTypes)
            }
        }

        // TODO implement many-type-parameters support
        private fun IrTypeParametersContainer.produceSpecializations() {
            val requestedSpecializationData = when (this) {
                is IrClass -> extractSpecializationTypes()
                is IrFunction -> extractSpecializationTypes()
                else -> throw AssertionError()
            }
            val typeParametersToAnnotate = requestedSpecializationData.annotatedTypeParameters
            // Filter purpose is: list with all nulls is equivalent to original function
            for (nextCombination in requestedSpecializationData.possibleCombinations.filter { it.any { it != null } }) {
                val mapping = typeParametersToAnnotate.zip(nextCombination)
                mapping.forEach { (parameter, type) -> localTypeParametersMapping[parameter.symbol] = type }
                produceOneSpecialization(localTypeParametersMapping, localTypeParametersMapping.values.toList())
                mapping.forEach { (parameter, _) -> localTypeParametersMapping.remove(parameter.symbol) }
            }
        }

        private fun IrTypeParametersContainer.produceOneSpecialization(mapping: Map<IrTypeParameterSymbol, IrType?>, concreteTypes: List<IrType?>) {
            val copier = NewTypeParameterEliminator(
                    typeParametersMapping,
                    mapping,
                    encoder,
                    context,
                    parent
            )
            val newDeclaration = getSpecialization(mapping, copier)
            mappedDeclarations[this to concreteTypes] = newDeclaration
        }

        // TODO implement many-type-parameters support
        private inline fun <reified T : IrTypeParametersContainer> T.getSpecialization(primitiveTypeSubstitutionMap: Map<IrTypeParameterSymbol, IrType?>, copier: NewTypeParameterEliminator): T {
            copier.addNewDeclarationName(this, encoder.encode(this, primitiveTypeSubstitutionMap.values.toList())!!)

            return (eliminateTypeParameters(copier).transform(this@NewSpecializationTransformer, data = null) as T).also {
                newSpecializations.getOrPut(parent) { mutableMapOf() }[it] = this
            }
        }

        /**
         * Adds new functions to given [parent]. This function must not be called
         * when visiting members inside the scope of [parent],
         * otherwise [ConcurrentModificationException] may be thrown.
         */
        @Suppress("DuplicatedCode")
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
}
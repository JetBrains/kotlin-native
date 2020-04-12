package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.classIfConstructor
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.statements

internal class NewGenericsSpecialization(val context: Context) : FileLoweringPass {

    private val transformer = NewSpecializationTransformer(context)

    companion object {
        private val mappedDeclarations = mutableMapOf<Pair<IrTypeParametersContainer, List<IrType?>>, IrTypeParametersContainer>()
    }

    override fun lower(irFile: IrFile) {
        irFile.transform(transformer, null)
    }

    class NewSpecializationTransformer(val context: Context) : IrBuildingTransformer(context) {

        companion object {
            // scope -> specialization -> origin
            private val newSpecializations = mutableMapOf<IrDeclarationParent, MutableMap<IrTypeParametersContainer, IrTypeParametersContainer>>()

            private val typeParametersMapping = mutableMapOf<IrTypeParameterSymbol, IrType>()
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

        // TODO implement many-type-parameters support
        private fun IrTypeParametersContainer.produceSpecializations() {
            if (typeParameters.size != 1) return
            val requestedSpecializationTypes = when (this) {
                is IrClass -> getRequestedSpecializationTypes()
                else -> typeParameters.first().getRequestedSpecializationTypes()
            }
            for (type in requestedSpecializationTypes) {
                val mapping = mapOf(typeParameters.first().symbol to type)
                produceOneSpecialization(mapping, listOf(type))
            }
        }

        private fun IrTypeParametersContainer.produceOneSpecialization(mapping: Map<IrTypeParameterSymbol, IrType>, concreteTypes: List<IrType?>) {
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
        private inline fun <reified T : IrTypeParametersContainer> T.getSpecialization(primitiveTypeSubstitutionMap: Map<IrTypeParameterSymbol, IrType>, copier: NewTypeParameterEliminator): T {
            copier.addNewDeclarationName(this, encoder.encode(this, primitiveTypeSubstitutionMap)!!)

            return (eliminateTypeParameters(copier).transform(this@NewSpecializationTransformer, data = null) as T).also {
                newSpecializations.getOrPut(parent) { mutableMapOf() }[it] = this
            }
        }

        private fun IrClass.getRequestedSpecializationTypes(): List<IrType> {
            if (typeParameters.size != 1) return emptyList()

            val annotation = annotations.find { it.symbol.owner.classIfConstructor.nameForIrSerialization.asString() == "SpecializedClass" }
                    ?: return emptyList()
            val types = annotation.getValueArgument(0) as IrVararg
            return types.elements.map { (it as IrClassReference).classType }
        }

        private fun IrTypeParameter.getRequestedSpecializationTypes(): List<IrType> {
            val annotation = annotations.find { it.symbol.owner.classIfConstructor.nameForIrSerialization.asString() == "Specialized" }
                    ?: return emptyList()
            val types = annotation.getValueArgument(0) as IrVararg
            return types.elements.map { (it as IrClassReference).classType }
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
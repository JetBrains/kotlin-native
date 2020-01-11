package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.lower.SpecializationTransformer
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

/**
 * Replaces type parameters with concrete types in scope which this visitor was called on.
 */
internal class TypeParameterEliminator(
        private val specializationTransformer: SpecializationTransformer,
        val context: Context): IrBuildingTransformer(context) {

    private val functionsNesting = mutableListOf<IrFunction>()
    private val variables = mutableMapOf<VariableDescriptor, IrVariable>()

    // available mappings from type parameters to concrete types
    // (any type parameter from domain in given scope must be replaced with associated type)
    private val typeParametersMapping = mutableMapOf<IrTypeParameter, IrType>()

    // value parameter descriptor of specialization -> value parameter of origin
    private val valueParametersMapping = mutableMapOf<ParameterDescriptor, IrValueParameter>()

    // local function in origin -> local function in specialization
    private val localFunctionsMapping = mutableMapOf<IrFunction, IrFunction>()

    fun addMapping(typeParameter: IrTypeParameter, concreteType: IrType) {
        typeParametersMapping[typeParameter] = concreteType
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return IrVariableImpl(
                declaration.startOffset,
                declaration.endOffset,
                declaration.origin,
                declaration.descriptor,
                declaration.type.substitute(typeParametersMapping.mapKeys { it.key.symbol }),
                declaration.initializer?.transform(this@TypeParameterEliminator, null)
        ).apply {
            val ini = initializer
            parent = when (ini) {
                is IrGetValue -> ini.symbol.owner.parent
                else -> functionsNesting.last()
            }
            variables[declaration.descriptor] = this
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return context.createIrBuilder(expression.symbol).run {
            val initialOwner = expression.symbol.owner
            val owner = localFunctionsMapping[initialOwner] ?: initialOwner
            irCall(owner).run {
                this.dispatchReceiver = expression.dispatchReceiver?.transform(this@TypeParameterEliminator, null)
                this.extensionReceiver = expression.extensionReceiver?.transform(this@TypeParameterEliminator, null)
                for (i in 0 until expression.typeArgumentsCount) {
                    putTypeArgument(i, expression.getTypeArgument(i)?.substitute(typeParametersMapping.mapKeys { it.key.symbol }))
                }
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i)?.transform(this@TypeParameterEliminator, null))
                }
                transform(specializationTransformer, null)
            }
        }
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        return context.createIrBuilder(functionsNesting.last().symbol).run {
            irReturn(expression.value.transform(this@TypeParameterEliminator, null))
        }
    }

    // Creates new specialization
    fun visitFunctionAsCallee(declaration: IrFunction): IrFunction {
        if (declaration.typeParameters.isEmpty()) return declaration
        return visitFunction(declaration, createSpecialization = true)
    }

    // Visit local function in specialization body
    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (!declaration.isLocal) throw AssertionError("Function must be local")
        return visitFunction(declaration, createSpecialization = false)
    }

    private fun visitFunction(declaration: IrFunction, createSpecialization: Boolean): IrFunction {
        if (declaration !is IrSimpleFunction) {
            return declaration
        }
        with(declaration) {
            val functionDescriptor = WrappedSimpleFunctionDescriptor()
            val functionSymbol = IrSimpleFunctionSymbolImpl(functionDescriptor)
            val functionName =
                    if (createSpecialization)
                        Name.identifier("$nameForIrSerialization-${typeParametersMapping[declaration.typeParameters.first()]?.toKotlinType()}")
                    else
                        name
            return IrFunctionImpl(
                    startOffset,
                    endOffset,
                    origin,
                    functionSymbol,
                    functionName,
                    visibility,
                    modality,
                    returnType.substitute(typeParametersMapping.mapKeys { it.key.symbol }),
                    isInline,
                    isExternal,
                    isTailrec,
                    isSuspend,
                    isExpect,
                    isFakeOverride
            ).also { newFunction ->
                functionDescriptor.bind(newFunction)
                if (!createSpecialization) {
                    newFunction.parent = functionsNesting.last()
                }
                functionsNesting.add(newFunction)
                newFunction.annotations.addAll(annotations)
                extensionReceiverParameter?.let {
                    newFunction.extensionReceiverParameter = it.specializedFor(newFunction)
                }
                dispatchReceiverParameter?.let {
                    newFunction.dispatchReceiverParameter = it.specializedFor(newFunction)
                }
                if (!createSpecialization) {
                    typeParameters.mapTo(newFunction.typeParameters) {
                        it
                    }
                }
                valueParameters.mapTo(newFunction.valueParameters) {
                    it.specializedFor(newFunction)
                }
                newFunction.body = body?.deepCopyWithSymbols(declaration)?.transform(this@TypeParameterEliminator, null)
                functionsNesting.removeAt(functionsNesting.lastIndex)

                if (!createSpecialization) {
                    localFunctionsMapping[declaration] = newFunction
                }
            }
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val symbol = expression.symbol
        val descriptor = symbol.descriptor
        return context.createIrBuilder(symbol).run {
            when (descriptor) {
                is ValueParameterDescriptor -> irGet(valueParametersMapping[descriptor] ?: return expression)
                is VariableDescriptor -> irGet(variables[descriptor] ?: return expression)
                is ReceiverParameterDescriptor -> {
                    irGet(functionsNesting.last().extensionReceiverParameter!!)
                }
                else -> expression
            }
        }
    }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        val symbol = expression.symbol
        val descriptor = symbol.descriptor
        return context.createIrBuilder(symbol).run {
            irSetVar(variables[descriptor] ?: return expression, expression.value.transform(this@TypeParameterEliminator, null))
        }
    }

    private fun IrValueParameter.specializedFor(function: IrFunction): IrValueParameter {
        return copyTo(function, type = type.substitute(typeParametersMapping.mapKeys { it.key.symbol }))
                .also {
                    valueParametersMapping[descriptor] = it
                }
    }
}
package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.Name

internal class TypeParameterEliminator(
        val context: Context,
        val typeParameter: IrTypeParameter,
        val concreteType: IrType): IrBuildingTransformer(context) {

    private val functionsNesting = mutableListOf<IrFunction>()
    private val variables = mutableMapOf<VariableDescriptor, IrVariable>()

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return IrVariableImpl(
                declaration.startOffset,
                declaration.endOffset,
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrVariableSymbolImpl(declaration.descriptor),
                declaration.type
        ).apply {
            initializer = declaration.initializer?.transform(this@TypeParameterEliminator, null)
            val ini = initializer
            parent = when (ini) {
                is IrGetValue -> ini.symbol.owner.parent
                else -> declaration.parent
            }
            variables[declaration.descriptor] = this
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return context.createIrBuilder(expression.symbol).run {
            val irFunction = expression.symbol.owner
            irCall(irFunction).apply {
                this.dispatchReceiver = expression.dispatchReceiver?.transform(this@TypeParameterEliminator, null)
                this.extensionReceiver = expression.extensionReceiver?.transform(this@TypeParameterEliminator, null)
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i)?.transform(this@TypeParameterEliminator, null))
                }
            }
        }
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        return context.createIrBuilder(functionsNesting.last().symbol).run {
            irReturn(expression.value.transform(this@TypeParameterEliminator, null))
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
//        println(declaration.dump())
//        functionDeclarationStack.add(declaration)
        if (declaration !is IrSimpleFunction) {
            return declaration
        }
        with(declaration) {
            val newFunctionDescriptor = WrappedSimpleFunctionDescriptor()
            val newFunctionSymbol = IrSimpleFunctionSymbolImpl(newFunctionDescriptor)

            return IrFunctionImpl(
                    startOffset,
                    endOffset,
                    origin,
                    newFunctionSymbol,
                    Name.identifier("$nameForIrSerialization-${concreteType.toKotlinType()}"),
                    visibility,
                    modality,
                    returnType.replaceIfNeeded(concreteType),
                    isInline,
                    isExternal,
                    isTailrec,
                    isSuspend,
                    isExpect,
                    isFakeOverride
            ).also { newFunction ->
                newFunctionDescriptor.bind(newFunction)
                functionsNesting.add(newFunction)
                newFunction.annotations.addAll(annotations)
                extensionReceiverParameter?.let {
                    newFunction.extensionReceiverParameter = it.copyTo(newFunction, type = it.type.replaceIfNeeded(concreteType))
                }
                dispatchReceiverParameter?.let {
                    newFunction.dispatchReceiverParameter = it.copyTo(newFunction, type = it.type.replaceIfNeeded(concreteType))
                }
                valueParameters.mapTo(newFunction.valueParameters) {
                    it.copyTo(newFunction, type = it.type.replaceIfNeeded(concreteType))
                }
                newFunction.body = body?.deepCopyWithSymbols(declaration)?.transform(this@TypeParameterEliminator, null)
                functionsNesting.removeAt(functionsNesting.lastIndex)
            }
        }

    }


    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val symbol = expression.symbol
        val descriptor = symbol.descriptor
        return context.createIrBuilder(symbol).run {
            when (descriptor) {
                is ValueParameterDescriptor -> irGet(functionsNesting.last().getIrValueParameter(descriptor))
                is VariableDescriptor -> irGet(variables[descriptor] ?: return expression)
                is ReceiverParameterDescriptor -> {
                    irGet(functionsNesting.last().extensionReceiverParameter!!)
                }
                else -> expression
            }
        }
    }

    private fun IrType.replaceIfNeeded(newType: IrType): IrType {
        // TODO reconsider condition when there will be elimination for functions with 2+ type parameters
        return if (isTypeParameter()) newType else this
    }
}
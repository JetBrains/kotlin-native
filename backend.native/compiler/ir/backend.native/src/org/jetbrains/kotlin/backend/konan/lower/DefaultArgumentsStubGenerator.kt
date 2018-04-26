/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.irasdescriptors.KotlinBuiltIns
import org.jetbrains.kotlin.backend.konan.irasdescriptors.defaultType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg

open class DefaultArgumentStubGenerator constructor(val context: CommonBackendContext): DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lower(memberDeclaration).also { functions ->
                    functions.forEach {
                        it.parent = irDeclarationContainer
                    }
                }
            else
                null
        }

    }

    private val symbols = context.ir.symbols

    private fun lower(irFunction: IrFunction): List<IrFunction> {
        val functionDescriptor = irFunction.descriptor

        if (!functionDescriptor.needsDefaultArgumentsLowering)
            return listOf(irFunction)

        val bodies = functionDescriptor.valueParameters
                .mapNotNull{irFunction.getDefault(it)}


        log { "detected ${functionDescriptor.name.asString()} has got #${bodies.size} default expressions" }
        functionDescriptor.overriddenDescriptors.forEach { context.log{"DEFAULT-REPLACER: $it"} }
        if (bodies.isNotEmpty()) {
            val newIrFunction = irFunction.generateDefaultsFunction(context)
            newIrFunction.parent = irFunction.parent
            val descriptor = newIrFunction.descriptor
            log { "$functionDescriptor -> $descriptor" }
            val builder = context.createIrBuilder(newIrFunction.symbol)
            newIrFunction.body = builder.irBlockBody(newIrFunction) {
                val params = mutableListOf<IrVariable>()
                val variables = mutableMapOf<ValueDescriptor, IrValueDeclaration>()

                irFunction.dispatchReceiverParameter?.let {
                    variables[it.descriptor] = newIrFunction.dispatchReceiverParameter!!
                }

                if (descriptor.extensionReceiverParameter != null) {
                    variables[functionDescriptor.extensionReceiverParameter!!] =
                            newIrFunction.extensionReceiverParameter!!
                }

                for (valueParameter in functionDescriptor.valueParameters) {
                    val parameter = newIrFunction.valueParameters[valueParameter.index]
                    val temporaryVariableSymbol =
                            IrVariableSymbolImpl(scope.createTemporaryVariableDescriptor(parameter.descriptor))
                    val temporaryVariable = if (valueParameter.hasDefaultValue()) {
                        val kIntAnd = symbols.intAnd
                        val condition = irNotEquals(irCall(kIntAnd).apply {
                            dispatchReceiver = irGet(maskParameter(newIrFunction, valueParameter.index / 32))
                            putValueArgument(0, irInt(1 shl (valueParameter.index % 32)))
                        }, irInt(0))
                        val expressionBody = getDefaultParameterExpressionBody(irFunction, valueParameter)

                        /* Use previously calculated values in next expression. */
                        expressionBody.transformChildrenVoid(object: IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                log { "GetValue: ${expression.descriptor}" }
                                val valueSymbol = variables[expression.descriptor] ?: return expression
                                return irGet(valueSymbol)
                            }
                        })
                        val variableInitialization = irIfThenElse(
                                type      = parameter.type,
                                condition = condition,
                                thenPart  = expressionBody.expression,
                                elsePart  = irGet(parameter))
                        scope.createTemporaryVariable(
                                symbol  = temporaryVariableSymbol,
                                initializer = variableInitialization)
                        /* Mapping calculated values with its origin variables. */
                    } else {
                        scope.createTemporaryVariable(
                                symbol  = temporaryVariableSymbol,
                                initializer = irGet(parameter))
                    }
                    +temporaryVariable

                    params.add(temporaryVariable)
                    variables.put(valueParameter, temporaryVariable)
                }
                if (irFunction is IrConstructor) {
                    + IrDelegatingConstructorCallImpl(
                            startOffset = irFunction.startOffset,
                            endOffset   = irFunction.endOffset,
                            type        = irFunction.returnType,
                            symbol      = irFunction.symbol, descriptor = irFunction.symbol.descriptor,
                            typeArgumentsCount = irFunction.typeParameters.size // FIXME: or class type parameters?
                    ).apply {
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGet(newIrFunction.dispatchReceiverParameter!!)
                        }
                    }
                } else {
                    +irReturn(irCall(irFunction.symbol).apply {
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGet(newIrFunction.dispatchReceiverParameter!!)
                        }
                        if (functionDescriptor.extensionReceiverParameter != null) {
                            extensionReceiver = irGet(variables[functionDescriptor.extensionReceiverParameter!!]!!)
                        }
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                    })
                }
            }
            // Remove default argument initializers.
            irFunction.valueParameters.forEach {
                it.defaultValue = null
            }

            return listOf(irFunction, newIrFunction)
        }
        return listOf(irFunction)
    }


    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

private fun Scope.createTemporaryVariableDescriptor(parameterDescriptor: ParameterDescriptor?): VariableDescriptor =
        IrTemporaryVariableDescriptorImpl(
                containingDeclaration = this.scopeOwner,
                name                  = parameterDescriptor!!.name.asString().synthesizedName,
                outType               = parameterDescriptor.type,
                isMutable             = false)

private fun Scope.createTemporaryVariable(symbol: IrVariableSymbol, initializer: IrExpression) =
        IrVariableImpl(
                startOffset = initializer.startOffset,
                endOffset   = initializer.endOffset,
                origin      = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                symbol      = symbol,
                type        = initializer.type).apply {

            this.initializer = initializer
        }

private fun getDefaultParameterExpressionBody(irFunction: IrFunction, valueParameter: ValueParameterDescriptor): IrExpressionBody {
    return irFunction.getDefault(valueParameter) ?: TODO("FIXME!!!")
}

private fun maskParameterDescriptor(function: IrFunction, number: Int) =
        maskParameter(function, number).descriptor as ValueParameterDescriptor
private fun maskParameter(function: IrFunction, number: Int) =
        function.valueParameters.single { it.descriptor.name == parameterMaskName(number) }

private fun markerParameterDescriptor(descriptor: FunctionDescriptor) = descriptor.valueParameters.single { it.name == kConstructorMarkerName }

internal fun nullConst(expression: IrElement, type: IrType, context: KonanBackendContext) = when {
    KotlinBuiltIns.isFloat(type)   -> IrConstImpl.float     (expression.startOffset, expression.endOffset, type, 0.0F)
    KotlinBuiltIns.isDouble(type)  -> IrConstImpl.double    (expression.startOffset, expression.endOffset, type, 0.0)
    KotlinBuiltIns.isBoolean(type) -> IrConstImpl.boolean   (expression.startOffset, expression.endOffset, type, false)
    KotlinBuiltIns.isByte(type)    -> IrConstImpl.byte      (expression.startOffset, expression.endOffset, type, 0)
    KotlinBuiltIns.isChar(type)    -> IrConstImpl.char      (expression.startOffset, expression.endOffset, type, 0.toChar())
    KotlinBuiltIns.isShort(type)   -> IrConstImpl.short     (expression.startOffset, expression.endOffset, type, 0)
    KotlinBuiltIns.isInt(type)     -> IrConstImpl.int       (expression.startOffset, expression.endOffset, type, 0)
    KotlinBuiltIns.isLong(type)    -> IrConstImpl.long      (expression.startOffset, expression.endOffset, type, 0)
    else                           -> IrConstImpl.constNull (expression.startOffset, expression.endOffset, context.ir.symbols.nothing.defaultType)
}

internal class DefaultParameterInjector constructor(val context: KonanBackendContext): BodyLoweringPass {
    override fun lower(irBody: IrBody) {

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                super.visitDelegatingConstructorCall(expression)
                val descriptor = expression.descriptor
                if (!descriptor.needsDefaultArgumentsLowering)
                    return expression
                val argumentsCount = argumentCount(expression)
                if (argumentsCount == descriptor.valueParameters.size)
                    return expression
                val (symbolForCall, params) = parametersForCall(expression)
                symbolForCall as IrConstructorSymbol
                return IrDelegatingConstructorCallImpl(
                        startOffset = expression.startOffset,
                        endOffset   = expression.endOffset,
                        type        = symbolForCall.owner.returnType,
                        symbol      = symbolForCall,
                        descriptor  = symbolForCall.descriptor,
                        typeArgumentsCount = symbolForCall.owner.typeParameters.size) // FIXME
                        .apply {
                            params.forEach {
                                log { "call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}" }
                                putValueArgument(it.first.index, it.second)
                            }
                            dispatchReceiver = expression.dispatchReceiver
                        }

            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val functionDescriptor = expression.descriptor

                if (!functionDescriptor.needsDefaultArgumentsLowering)
                    return expression

                val argumentsCount = argumentCount(expression)
                if (argumentsCount == functionDescriptor.valueParameters.size)
                    return expression
                val (symbol, params) = parametersForCall(expression)
                val function = symbol.owner as IrFunction
                val descriptor = symbol.descriptor
                descriptor.typeParameters.forEach { log { "$descriptor [${it.index}]: $it" } }
                descriptor.original.typeParameters.forEach { log { "${descriptor.original}[${it.index}] : $it" } }
                return IrCallImpl(
                        startOffset   = expression.startOffset,
                        endOffset     = expression.endOffset,
                        type          = function.returnType,
                        symbol        = symbol,
                        descriptor    = descriptor,
                        typeArgumentsCount = expression.typeArgumentsCount)
                        .apply {
                            this.copyTypeArgumentsFrom(expression)

                            params.forEach {
                                log { "call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}" }
                                putValueArgument(it.first.index, it.second)
                            }
                            expression.extensionReceiver?.apply{
                                extensionReceiver = expression.extensionReceiver
                            }
                            expression.dispatchReceiver?.apply {
                                dispatchReceiver = expression.dispatchReceiver
                            }
                            log { "call::extension@: ${ir2string(expression.extensionReceiver)}" }
                            log { "call::dispatch@: ${ir2string(expression.dispatchReceiver)}" }
                        }
            }

            private fun IrFunction.findSuperMethodWithDefaultArguments(): IrFunction? {
                if (!this.descriptor.needsDefaultArgumentsLowering) return null

                if (this !is IrSimpleFunction) return this

                this.overriddenSymbols.forEach {
                    it.owner.findSuperMethodWithDefaultArguments()?.let { return it }
                }

                return this
            }

            private fun parametersForCall(expression: IrFunctionAccessExpression): Pair<IrFunctionSymbol, List<Pair<ValueParameterDescriptor, IrExpression?>>> {
                val descriptor = expression.descriptor
                val keyFunction = expression.symbol.owner.findSuperMethodWithDefaultArguments()!!
                val realFunction = keyFunction.generateDefaultsFunction(context)
                realFunction.parent = keyFunction.parent
                val realDescriptor = realFunction.descriptor

                log { "$descriptor -> $realDescriptor" }
                val maskValues = Array((descriptor.valueParameters.size + 31) / 32, { 0 })
                val params = mutableListOf<Pair<ValueParameterDescriptor, IrExpression?>>()
                params.addAll(descriptor.valueParameters.mapIndexed { i, _ ->
                    val valueArgument = expression.getValueArgument(i)
                    if (valueArgument == null) {
                        val maskIndex = i / 32
                        maskValues[maskIndex] = maskValues[maskIndex] or (1 shl (i % 32))
                    }
                    val valueParameterDescriptor = realDescriptor.valueParameters[i]
                    val defaultValueArgument = if (valueParameterDescriptor.isVararg) null else nullConst(expression, realFunction.valueParameters[i].type, context)
                    valueParameterDescriptor to (valueArgument ?: defaultValueArgument)
                })
                maskValues.forEachIndexed { i, maskValue ->
                    params += maskParameterDescriptor(realFunction, i) to IrConstImpl.int(
                            startOffset = irBody.startOffset,
                            endOffset   = irBody.endOffset,
                            type        = context.ir.symbols.int.defaultType,
                            value       = maskValue)
                }
                if (expression.descriptor is ClassConstructorDescriptor) {
                    val defaultArgumentMarker = context.ir.symbols.defaultConstructorMarker
                    params += markerParameterDescriptor(realDescriptor) to IrGetObjectValueImpl(
                            startOffset = irBody.startOffset,
                            endOffset   = irBody.endOffset,
                            type        = defaultArgumentMarker.defaultType,
                            symbol      = defaultArgumentMarker)
                }
                else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
                    params += realDescriptor.valueParameters.last() to
                            IrConstImpl.constNull(irBody.startOffset, irBody.endOffset, context.ir.symbols.any.defaultType)
                }
                params.forEach {
                    log { "descriptor::${realDescriptor.name.asString()}#${it.first.index}: ${it.first.name.asString()}" }
                }
                return Pair(realFunction.symbol, params)
            }

            private fun argumentCount(expression: IrMemberAccessExpression) =
                    expression.descriptor.valueParameters.count { expression.getValueArgument(it) != null }
        })
    }

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }
}

private val CallableMemberDescriptor.needsDefaultArgumentsLowering
    get() = valueParameters.any { it.hasDefaultValue() } && !(this is FunctionDescriptor && isInline)

private fun IrFunction.generateDefaultsFunction(context: CommonBackendContext): IrFunction = with (this.descriptor) {
    return context.ir.defaultParameterDeclarationsCache.getOrPut(this) {
        val descriptor = when (this) {
            is ClassConstructorDescriptor ->
                ClassConstructorDescriptorImpl.create(
                        /* containingDeclaration = */ containingDeclaration,
                        /* annotations           = */ annotations,
                        /* isPrimary             = */ false,
                        /* source                = */ source)
            else -> {
                val name = Name.identifier("$name\$default")

                SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ containingDeclaration,
                        /* annotations           = */ annotations,
                        /* name                  = */ name,
                        /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                        /* source                = */ source)
            }
        }

        val function = this@generateDefaultsFunction

        val syntheticParameters = MutableList((valueParameters.size + 31) / 32) { i ->
            valueParameter(descriptor, valueParameters.size + i, parameterMaskName(i), context.irBuiltIns.intType, function.startOffset, function.endOffset)
        }
        if (this is ClassConstructorDescriptor) {
            syntheticParameters += valueParameter(descriptor, syntheticParameters.last().index + 1,
                    kConstructorMarkerName,
                    context.ir.symbols.defaultConstructorMarker.defaultType, function.startOffset, function.endOffset)
        }
        else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
            syntheticParameters += valueParameter(descriptor, syntheticParameters.last().index + 1,
                    "handler".synthesizedName,
                    context.irBuiltIns.anyNType, function.startOffset, function.endOffset)
        }

        val newValueParameters = function.valueParameters.map {
            val parameterDescriptor = ValueParameterDescriptorImpl(
                    containingDeclaration = descriptor,
                    original = null, /* ValueParameterDescriptorImpl::copy do not save original. */
                    index = it.index,
                    annotations = it.descriptor.annotations,
                    name = it.name,
                    outType = it.descriptor.type,
                    declaresDefaultValue = false,
                    isCrossinline = it.isCrossinline,
                    isNoinline = it.isNoinline,
                    varargElementType = it.varargElementType?.toKotlinType(),
                    source = it.descriptor.source)

            it.copyAsValueParameter(parameterDescriptor)

        } + syntheticParameters

        descriptor.initialize(
                /* receiverParameterType         = */ extensionReceiverParameter?.type,
                /* dispatchReceiverParameter     = */ dispatchReceiverParameter,
                /* typeParameters                = */ typeParameters.map {
            TypeParameterDescriptorImpl.createForFurtherModification(
                    /* containingDeclaration = */ descriptor,
                    /* annotations           = */ it.annotations,
                    /* reified               = */ it.isReified,
                    /* variance              = */ it.variance,
                    /* name                  = */ it.name,
                    /* index                 = */ it.index,
                    /* source                = */ it.source,
                    /* reportCycleError      = */ null,
                    /* supertypeLoopsChecker = */ SupertypeLoopChecker.EMPTY
            ).apply {
                it.upperBounds.forEach { addUpperBound(it) }
                setInitialized()
            }
        },
                /* unsubstitutedValueParameters  = */ newValueParameters.map { it.descriptor as ValueParameterDescriptor },
                /* unsubstitutedReturnType       = */ returnType,
                /* modality                      = */ Modality.FINAL,
                /* visibility                    = */ this.visibility)
        descriptor.isSuspend = this.isSuspend
        context.log{"adds to cache[$this] = $descriptor"}

        val startOffset = this.startOffsetOrUndefined
        val endOffset = this.endOffsetOrUndefined

        val result: IrFunction = when (descriptor) {
            is ClassConstructorDescriptor -> IrConstructorImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                    descriptor
            )

            else -> IrFunctionImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                    descriptor
            )
        }

        result.returnType = this@generateDefaultsFunction.returnType

        this@generateDefaultsFunction.typeParameters.mapTo(result.typeParameters) {
            assert(this@generateDefaultsFunction.descriptor.typeParameters[it.index] == it.descriptor)
            IrTypeParameterImpl(
                    startOffset, endOffset, origin, descriptor.typeParameters[it.index]
            ).apply { this.superTypes += it.superTypes } // FIXME: offsets must be from IR
        }
        function.parent.let {
            if (it is IrClass) result.createDispatchReceiverParameter(it)
        }
        function.extensionReceiverParameter?.let {
            result.extensionReceiverParameter = IrValueParameterImpl(
                    it.startOffset,
                    it.endOffset,
                    it.origin,
                    descriptor.extensionReceiverParameter!!,
                    it.type,
                    it.varargElementType
            ).apply { parent = result }
        }

        result.valueParameters += newValueParameters.also { it.forEach { it.parent = result } }

        result
    }
}

object DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER :
        IrDeclarationOriginImpl("DEFAULT_PARAMETER_EXTENT")

private fun valueParameter(descriptor: FunctionDescriptor, index: Int, name: Name, type: IrType, startOffset: Int, endOffset: Int): IrValueParameter {
    val parameterDescriptor = ValueParameterDescriptorImpl(
            containingDeclaration = descriptor,
            original = null,
            index = index,
            annotations = Annotations.EMPTY,
            name = name,
            outType = type.toKotlinType(),
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
    )
    return IrValueParameterImpl(
            startOffset,
            endOffset,
            IrDeclarationOrigin.DEFINED,
            parameterDescriptor,
            type,
            null
    )
}

internal val kConstructorMarkerName = "marker".synthesizedName

private fun parameterMaskName(number: Int) = "mask$number".synthesizedName

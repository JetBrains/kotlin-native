/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("FoldInitializerAndIfToElvis")

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.descriptors.needsInlining
import org.jetbrains.kotlin.backend.konan.descriptors.propertyIfAccessor
import org.jetbrains.kotlin.backend.konan.descriptors.resolveFakeOverride
import org.jetbrains.kotlin.backend.konan.ir.DeserializerDriver
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoidWithContext() {

    private val deserializer = DeserializerDriver(context)
    private val globalSubstituteMap = mutableMapOf<DeclarationDescriptor, SubstitutedDescriptor>()

    //-------------------------------------------------------------------------//

    fun inline(irModule: IrModuleFragment): IrElement {
        val transformedModule = irModule.accept(this, null)
        DescriptorSubstitutorForExternalScope(globalSubstituteMap, context).run(transformedModule)   // Transform calls to object that might be returned from inline function call.
        return transformedModule
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {

        val irCall = super.visitCall(expression) as IrCall
        val functionDescriptor = irCall.descriptor
        if (!functionDescriptor.needsInlining || functionDescriptor == context.ir.symbols.isInitializedGetterDescriptor) {
            return irCall // This call does not need inlining.
        }

        val functionDeclaration = getFunctionDeclaration(functionDescriptor)     // Get declaration of the function to be inlined.
        if (functionDeclaration == null) {                                                  // We failed to get the declaration.
            val message = "Inliner failed to obtain function declaration: " +
                          functionDescriptor.fqNameSafe.toString()
            context.reportWarning(message, currentFile, irCall)                             // Report warning.
            return irCall
        }

        functionDeclaration.transformChildrenVoid(this)                                     // Process recursive inline.
        val inliner = Inliner(globalSubstituteMap, functionDeclaration, currentScope!!, context, this)    // Create inliner for this scope.
        return inliner.inline(irCall)                                  // Return newly created IrInlineBody instead of IrCall.
    }

    //-------------------------------------------------------------------------//

    private fun getFunctionDeclaration(descriptor: FunctionDescriptor): IrFunction? =
            descriptor.resolveFakeOverride().original.let {
                when  {
                    it.isBuiltInIntercepted(context.config.configuration.languageVersionSettings) ->
                        error("Continuation.intercepted is not available with release coroutines")

                    it.isBuiltInSuspendCoroutineUninterceptedOrReturn(context.config.configuration.languageVersionSettings) ->
                        getFunctionDeclaration(context.ir.symbols.konanSuspendCoroutineUninterceptedOrReturn.descriptor)

                    it == context.ir.symbols.coroutineContextGetter ->
                        getFunctionDeclaration(context.ir.symbols.konanCoroutineContextGetter.descriptor)

                    else -> {
                        val functionDeclaration =
                                context.ir.originalModuleIndex.functions[it] ?:             // If function is declared in the current module.
                                deserializer.deserializeInlineBody(it)                      // Function is declared in another module.
                        functionDeclaration as IrFunction?
                    }
                }
            }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)
}

private val inlineConstructor = FqName("kotlin.native.internal.InlineConstructor")
private val FunctionDescriptor.isInlineConstructor get() = annotations.hasAnnotation(inlineConstructor)

//-----------------------------------------------------------------------------//

private class Inliner(val globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>,
                      val functionDeclaration: IrFunction,                                  // Function to substitute.
                      val currentScope: ScopeWithIr,
                      val context: Context,
                      val owner: FunctionInlining /*TODO: make inner*/) {

    val copyIrElement = DeepCopyIrTreeWithDescriptors(functionDeclaration.descriptor, currentScope.scope.scopeOwner, context) // Create DeepCopy for current scope.
    val substituteMap = mutableMapOf<ValueDescriptor, IrExpression>()

    //-------------------------------------------------------------------------//

    fun inline(irCall: IrCall): IrReturnableBlockImpl {                                     // Call to be substituted.
        val inlineFunctionBody = inlineFunction(irCall, functionDeclaration)
        return inlineFunctionBody
    }

    //-------------------------------------------------------------------------//
    /**
     * TODO: JVM inliner crashed on attempt inline this function from transform.kt with:
     *  j.l.IllegalStateException: Couldn't obtain compiled function body for
     *  public inline fun <reified T : org.jetbrains.kotlin.ir.IrElement> kotlin.collections.MutableList<T>.transform...
     */
     private inline fun <reified T : IrElement> MutableList<T>.transform(transformation: (T) -> IrElement) {
          forEachIndexed { i, item ->
              set(i, transformation(item) as T)
          }
     }

    private fun inlineFunction(callee: IrCall,                                             // Call to be substituted.
                               caller: IrFunction): IrReturnableBlockImpl {                 // Function to substitute.

        val copyFunctionDeclaration = copyIrElement.copy(                         // Create copy of original function.
            irElement       = caller,                                                       // Descriptors declared inside the function will be copied.
            typeSubstitutor = createTypeSubstitutor(callee)                                 // Type parameters will be substituted with type arguments.
        ) as IrFunction

        val irReturnableBlockSymbol = IrReturnableBlockSymbolImpl(copyFunctionDeclaration.descriptor.original)

        val evaluationStatements = evaluateArguments(callee, copyFunctionDeclaration)       // And list of evaluation statements.

        val statements = (copyFunctionDeclaration.body as IrBlockBody).statements           // IR statements from function copy.

        val startOffset = caller.startOffset
        val endOffset = caller.endOffset
        val descriptor = caller.descriptor.original
        if (descriptor.isInlineConstructor) {
            val delegatingConstructorCall = statements[0] as IrDelegatingConstructorCall
            val irBuilder = context.createIrBuilder(irReturnableBlockSymbol, startOffset, endOffset)
            irBuilder.run {
                val constructorDescriptor = delegatingConstructorCall.descriptor.original
                val constructorCall = irCall(delegatingConstructorCall.symbol, callee.type,
                        constructorDescriptor.typeParameters.map { delegatingConstructorCall.getTypeArgument(it)!! }).apply {
                    constructorDescriptor.valueParameters.forEach { putValueArgument(it, delegatingConstructorCall.getValueArgument(it)) }
                }
                val oldThis = delegatingConstructorCall.descriptor.constructedClass.thisAsReceiverParameter
                val newThis = currentScope.scope.createTemporaryVariable(
                        irExpression = constructorCall,
                        nameHint     = delegatingConstructorCall.descriptor.fqNameSafe.toString() + ".this"
                )
                statements[0] = newThis
                substituteMap[oldThis] = irGet(newThis)
                statements.add(irReturn(irGet(newThis)))
            }
        }

        val sourceFileName = context.ir.originalModuleIndex.declarationToFile[caller.descriptor.original]?:""

        // Update globalSubstituteMap before computing return type.
        // This is needed because of nested inlining.
        copyIrElement.addCurrentSubstituteMap(globalSubstituteMap)

        val transformer = ParameterSubstitutor()
        statements.transform { it.transform(transformer, data = null) }
        statements.addAll(0, evaluationStatements)
        val oldDescriptor = (copyFunctionDeclaration.returnType.classifierOrNull as? IrClassSymbol)?.descriptor
        val substitutedDescriptor = oldDescriptor?.let { globalSubstituteMap[it] }
        val returnType = substitutedDescriptor?.let { context.ir.translateErased((it.descriptor as ClassDescriptor).defaultType) }
                ?: copyFunctionDeclaration.returnType

        val isCoroutineIntrinsicCall = callee.descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(context.config.configuration.languageVersionSettings)

        return IrReturnableBlockImpl(                                     // Create new IR element to replace "call".
            startOffset = startOffset,
            endOffset   = endOffset,
            type        = returnType,
            symbol      = irReturnableBlockSymbol,
            origin      = if (isCoroutineIntrinsicCall) CoroutineIntrinsicLambdaOrigin else null,
            statements  = statements,
            sourceFileName = sourceFileName
        )
    }

    //---------------------------------------------------------------------//

    private inner class ParameterSubstitutor: IrElementTransformerVoid() {

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val newExpression = super.visitGetValue(expression) as IrGetValue
            val descriptor = newExpression.descriptor
            val argument = substituteMap[descriptor]                                        // Find expression to replace this parameter.
            if (argument == null) return newExpression                                      // If there is no such expression - do nothing.

            argument.transformChildrenVoid(this)                                            // Default argument can contain subjects for substitution.
            return copyIrElement.copy(                                                      // Make copy of argument expression.
                irElement       = argument,
                typeSubstitutor = null
            ) as IrExpression
        }

        //-----------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrExpression {
            if (!isLambdaCall(expression))
                return super.visitCall(expression)

            val dispatchReceiver = expression.dispatchReceiver as IrGetValue
            val functionArgument = substituteMap[dispatchReceiver.descriptor]
            if (functionArgument == null)
                return super.visitCall(expression)
            val dispatchDescriptor = dispatchReceiver.descriptor
            if (dispatchDescriptor is ValueParameterDescriptor &&
                    dispatchDescriptor.isNoinline) return super.visitCall(expression)

            if (functionArgument is IrFunctionReference) {
                val functionDescriptor = functionArgument.descriptor
                val functionParameters = functionDescriptor.explicitParameters
                val boundFunctionParameters = functionArgument.getArguments()
                val unboundFunctionParameters = functionParameters - boundFunctionParameters.map { it.first }
                val boundFunctionParametersMap = boundFunctionParameters.associate { it.first to it.second }

                var unboundIndex = 0
                val unboundArgsSet = unboundFunctionParameters.toSet()
                val valueParameters = expression.getArguments().drop(1) // Skip dispatch receiver.

                val immediateCall = IrCallImpl(
                        startOffset = expression.startOffset,
                        endOffset   = expression.endOffset,
                        type        = expression.type,
                        symbol      = functionArgument.symbol,
                        descriptor  = functionArgument.descriptor).apply {
                    functionParameters.forEach {
                        val argument =
                                if (!unboundArgsSet.contains(it))
                                    boundFunctionParametersMap[it]!!
                                else
                                    valueParameters[unboundIndex++].second
                        when (it) {
                            functionDescriptor.dispatchReceiverParameter -> this.dispatchReceiver = argument
                            functionDescriptor.extensionReceiverParameter -> this.extensionReceiver = argument
                            else -> putValueArgument((it as ValueParameterDescriptor).index, argument)
                        }
                    }
                    assert(unboundIndex == valueParameters.size, { "Not all arguments of <invoke> are used" })
                }
                return owner.visitCall(super.visitCall(immediateCall) as IrCall)
            }
            if (functionArgument !is IrBlock)
                return super.visitCall(expression)

            val functionDeclaration = getLambdaFunction(functionArgument)
            val newExpression = inlineFunction(expression, functionDeclaration)             // Inline the lambda. Lambda parameters will be substituted with lambda arguments.
            return newExpression.transform(this, null)                                      // Substitute lambda arguments with target function arguments.
        }

        //-----------------------------------------------------------------//

        override fun visitElement(element: IrElement) = element.accept(this, null)
    }

    //--- Helpers -------------------------------------------------------------//

    private fun isLambdaCall(irCall: IrCall) : Boolean {
        if (!irCall.descriptor.isFunctionInvoke) return false                               // Lambda mast be called by "invoke".
        if (irCall.dispatchReceiver !is IrGetValue)                      return false       // Dispatch receiver mast be IrGetValue.
        return true                                                                         // It is lambda call.
    }

    //-------------------------------------------------------------------------//

    private fun getLambdaFunction(lambdaArgument: IrBlock): IrFunction {
        val statements = lambdaArgument.statements
        return statements[0] as IrFunction
    }

    //-------------------------------------------------------------------------//

    private fun createTypeSubstitutor(irCall: IrCall): TypeSubstitutor? {
        if (irCall.typeArgumentsCount == 0) return null
        val descriptor = irCall.descriptor.resolveFakeOverride().original
        val typeParameters = descriptor.propertyIfAccessor.typeParameters
        val substitutionContext = mutableMapOf<TypeConstructor, TypeProjection>()
        for (index in 0 until irCall.typeArgumentsCount) {
            val typeArgument = irCall.getTypeArgument(index) ?: continue
            substitutionContext[typeParameters[index].typeConstructor] = TypeProjectionImpl(typeArgument.toKotlinType())
        }
        return TypeSubstitutor.create(substitutionContext)
    }

    //-------------------------------------------------------------------------//

    private class ParameterToArgument(val parameter: IrValueParameter,
                                      val argumentExpression : IrExpression) {

        val isInlinableLambdaArgument : Boolean
            get() {
                if (!InlineUtil.isInlineParameter(parameter.descriptor)) return false
                if (argumentExpression is IrFunctionReference
                        && !argumentExpression.descriptor.isSuspend) return true // Skip suspend functions for now since it's not supported by FE anyway.

                // Do pattern-matching on IR.
                if (argumentExpression !is IrBlock) return false
                if (argumentExpression.origin != IrStatementOrigin.LAMBDA &&
                    argumentExpression.origin != IrStatementOrigin.ANONYMOUS_FUNCTION) return false
                val statements = argumentExpression.statements
                val irFunction = statements[0]
                val irCallableReference = statements[1]
                if (irFunction !is IrFunction) return false
                if (irCallableReference !is IrCallableReference) return false
                return true
            }

        val isImmutableVariableLoad: Boolean
            get() = argumentExpression.let {
                it is IrGetValue && !it.descriptor.let { it is VariableDescriptor && it.isVar }
            }
    }

    //-------------------------------------------------------------------------//

    private fun buildParameterToArgument(irCall    : IrCall,                                // Call site.
                                         irFunction: IrFunction                             // Function to be called.
    ): List<ParameterToArgument> {

        val parameterToArgument = mutableListOf<ParameterToArgument>()                      // Result list.

        if (irCall.dispatchReceiver != null &&                                              // Only if there are non null dispatch receivers both
            irFunction.dispatchReceiverParameter != null)                                   // on call site and in function declaration.
            parameterToArgument += ParameterToArgument(
                parameter = irFunction.dispatchReceiverParameter!!,
                argumentExpression  = irCall.dispatchReceiver!!
            )

        val valueArguments =
                irCall.descriptor.valueParameters.map { irCall.getValueArgument(it) }.toMutableList()

        if (irFunction.extensionReceiverParameter != null) {
            parameterToArgument += ParameterToArgument(
                    parameter = irFunction.extensionReceiverParameter!!,
                    argumentExpression = if (irCall.extensionReceiver != null) {
                        irCall.extensionReceiver!!
                    } else {
                        // Special case: lambda with receiver is called as usual lambda:
                        valueArguments.removeAt(0)!!
                    }
            )
        } else if (irCall.extensionReceiver != null) {
            // Special case: usual lambda is called as lambda with receiver:
            valueArguments.add(0, irCall.extensionReceiver!!)
        }

        val parametersWithDefaultToArgument = mutableListOf<ParameterToArgument>()
        irFunction.valueParameters.forEach { parameter ->                                   // Iterate value parameters.
            val argument = valueArguments[parameter.index]                                  // Get appropriate argument from call site.
            when {
                argument != null -> {                                                       // Argument is good enough.
                    parameterToArgument += ParameterToArgument(                             // Associate current parameter with the argument.
                        parameter = parameter,
                        argumentExpression  = argument
                    )
                }

                // After ExpectDeclarationsRemoving pass default values from expect declarations
                // are represented correctly in IR.
                parameter.defaultValue != null -> {  // There is no argument - try default value.
                    parametersWithDefaultToArgument += ParameterToArgument(
                        parameter = parameter,
                        argumentExpression  = parameter.defaultValue!!.expression
                    )
                }

                parameter.varargElementType != null -> {
                    val emptyArray = IrVarargImpl(
                        startOffset       = irCall.startOffset,
                        endOffset         = irCall.endOffset,
                        type              = parameter.type,
                        varargElementType = parameter.varargElementType!!
                    )
                    parameterToArgument += ParameterToArgument(
                        parameter = parameter,
                        argumentExpression  = emptyArray
                    )
                }

                else -> {
                    val message = "Incomplete expression: call to ${irFunction.descriptor} " +
                        "has no argument at index ${parameter.index}"
                    throw Error(message)
                }
            }
        }
        return parameterToArgument + parametersWithDefaultToArgument                        // All arguments except default are evaluated at callsite,
                                                                                            // but default arguments are evaluated inside callee.
    }

    //-------------------------------------------------------------------------//

    private fun evaluateArguments(irCall             : IrCall,                              // Call site.
                                  functionDeclaration: IrFunction                           // Function to be called.
        ): List<IrStatement> {

        val parameterToArgumentOld = buildParameterToArgument(irCall, functionDeclaration)  // Create map parameter_descriptor -> original_argument_expression.
        val evaluationStatements   = mutableListOf<IrStatement>()                           // List of evaluation statements.
        val substitutor = ParameterSubstitutor()
        parameterToArgumentOld.forEach {
            val parameterDescriptor = it.parameter.descriptor

            /*
             * We need to create temporary variable for each argument except inlinable lambda arguments.
             * For simplicity and to produce simpler IR we don't create temporaries for every immutable variable,
             * not only for those referring to inlinable lambdas.
             */
            if (it.isInlinableLambdaArgument) {
                substituteMap[parameterDescriptor] = it.argumentExpression
                return@forEach
            }

            if (it.isImmutableVariableLoad) {
                substituteMap[parameterDescriptor] = it.argumentExpression.transform(substitutor, data = null)   // Arguments may reference the previous ones - substitute them.
                return@forEach
            }

            val newVariable = currentScope.scope.createTemporaryVariable(                   // Create new variable and init it with the parameter expression.
                irExpression = it.argumentExpression.transform(substitutor, data = null),   // Arguments may reference the previous ones - substitute them.
                nameHint     = functionDeclaration.descriptor.name.toString(),
                isMutable    = false)

            evaluationStatements.add(newVariable)                                           // Add initialization of the new variable in statement list.
            val getVal = IrGetValueImpl(                                                    // Create new expression, representing access the new variable.
                startOffset = currentScope.irElement.startOffset,
                endOffset   = currentScope.irElement.endOffset,
                type        = newVariable.type,
                symbol      = newVariable.symbol
            )
            substituteMap[parameterDescriptor] = getVal                                     // Parameter will be replaced with the new variable.
        }
        return evaluationStatements
    }
}







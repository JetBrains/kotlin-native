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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.createArrayOfExpression
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*

internal class PropertyDelegationLowering(val context: Context) : FileLoweringPass {
    val reflectionTypes = context.reflectionTypes
    private val kotlinPackage = context.irModule!!.descriptor.getPackage(FqName("kotlin"))
    private val genericArrayType = kotlinPackage.memberScope.getContributedClassifier(Name.identifier("Array"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private fun getKPropertyImplConstructor(receiverTypes: List<KotlinType>,
                                            returnType: KotlinType,
                                            isLocal: Boolean,
                                            isMutable: Boolean) : Pair<IrConstructorSymbol, ClassConstructorDescriptor> {

        val symbols = context.ir.symbols

        val classSymbol =
                if (isLocal) {
                    assert(receiverTypes.isEmpty(), { "Local delegated property cannot have explicit receiver" })
                    when {
                        isMutable -> symbols.kLocalDelegatedMutablePropertyImpl
                        else -> symbols.kLocalDelegatedPropertyImpl
                    }
                } else {
                    when (receiverTypes.size) {
                        0 -> when {
                            isMutable -> symbols.kMutableProperty0Impl
                            else -> symbols.kProperty0Impl
                        }
                        1 -> when {
                            isMutable -> symbols.kMutableProperty1Impl
                            else -> symbols.kProperty1Impl
                        }
                        2 -> when {
                            isMutable -> symbols.kMutableProperty2Impl
                            else -> symbols.kProperty2Impl
                        }
                        else -> throw AssertionError("More than 2 receivers is not allowed")
                    }
                }

        val classDescriptor = classSymbol.descriptor

        val typeParameters = classDescriptor.declaredTypeParameters
        val arguments = (receiverTypes + listOf(returnType))
                .mapIndexed { index, type -> typeParameters[index].typeConstructor to TypeProjectionImpl(type) }
                .toMap()

        val constructorSymbol = classSymbol.constructors.single()

        return constructorSymbol to constructorSymbol.descriptor.substitute(TypeSubstitutor.create(arguments))!!
    }

    private fun ClassDescriptor.replace(vararg type: KotlinType): SimpleType {
        return this.defaultType.replace(type.map(::TypeProjectionImpl))
    }

    override fun lower(irFile: IrFile) {
        val kProperties = mutableMapOf<VariableDescriptorWithAccessors, Pair<IrExpression, Int>>()

        val arrayClass = context.ir.symbols.array

        val arrayItemGetter = arrayClass.functions.single { it.descriptor.name == Name.identifier("get") }

        val typeParameterT = genericArrayType.declaredTypeParameters[0]
        val kPropertyImplType = reflectionTypes.kProperty1Impl.replace(context.builtIns.anyType, context.builtIns.anyType)
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(kPropertyImplType)))
        val substitutedArrayItemGetter = arrayItemGetter.descriptor.substitute(typeSubstitutor)!!

        val kPropertiesField = IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION,
                createKPropertiesFieldDescriptor(irFile.packageFragmentDescriptor,
                        genericArrayType.replace(kPropertyImplType)
                )
        )

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                return super.visitFunction(declaration)
            }

            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                declaration.transformChildrenVoid(this)

                val initializer = declaration.delegate.initializer!!
                return IrVariableImpl(declaration.startOffset, declaration.endOffset,
                        declaration.origin, declaration.delegate.descriptor,
                        IrBlockImpl(initializer.startOffset, initializer.endOffset, initializer.type, null,
                                listOf(
                                        declaration.getter,
                                        declaration.setter,
                                        initializer
                                ).filterNotNull())
                )

            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)

                val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                if (receiversCount == 1) // Has receiver.
                    return createKProperty(expression)
                else if (receiversCount == 2)
                    throw AssertionError("Callable reference to properties with two receivers is not allowed: ${expression.descriptor}")
                else { // Cache KProperties with no arguments.
                    val field = kProperties.getOrPut(expression.descriptor) {
                        createKProperty(expression) to kProperties.size
                    }

                    return IrCallImpl(
                            expression.startOffset, expression.endOffset,
                            arrayItemGetter, substitutedArrayItemGetter
                    ).apply {
                        dispatchReceiver =
                                IrGetFieldImpl(expression.startOffset, expression.endOffset, kPropertiesField.symbol)

                        putValueArgument(0, IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, field.second))
                    }
                }

            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)
                val propertyDescriptor = expression.descriptor

                val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                if (receiversCount == 2)
                    throw AssertionError("Callable reference to properties with two receivers is not allowed: $propertyDescriptor")
                else { // Cache KProperties with no arguments.
                    // TODO: what about `receiversCount == 1` case?
                    val field = kProperties.getOrPut(propertyDescriptor) {
                        createLocalKProperty(expression, propertyDescriptor) to kProperties.size
                    }

                    return IrCallImpl(
                            expression.startOffset, expression.endOffset,
                            arrayItemGetter, substitutedArrayItemGetter
                    ).apply {
                        dispatchReceiver =
                                IrGetFieldImpl(expression.startOffset, expression.endOffset, kPropertiesField.symbol)

                        putValueArgument(0, IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, field.second))
                    }
                }

            }
        })

        if (kProperties.isNotEmpty()) {
            val initializers = kProperties.values.sortedBy { it.second }.map { it.first }
            // TODO: move to object for lazy initialization.
            irFile.declarations.add(0, kPropertiesField.apply {
                initializer = IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        context.createArrayOfExpression(kPropertyImplType, initializers, UNDEFINED_OFFSET, UNDEFINED_OFFSET))
            })
        }
    }

    private fun createKProperty(expression: IrPropertyReference): IrCallImpl {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        val receiverTypes = mutableListOf<KotlinType>()

        val propertyDescriptor = expression.descriptor

        val returnType = propertyDescriptor.type
        val getterCallableReference = propertyDescriptor.getter?.let { getter ->
            getter.extensionReceiverParameter.let {
                if (it != null && expression.extensionReceiver == null)
                    receiverTypes.add(it.type)
            }
            getter.dispatchReceiverParameter.let {
                if (it != null && expression.dispatchReceiver == null)
                    receiverTypes.add(it.type)
            }
            val getterKFunctionType = context.reflectionTypes.getKFunctionType(
                    annotations = Annotations.EMPTY,
                    receiverType = receiverTypes.firstOrNull(),
                    parameterTypes = if (receiverTypes.size < 2) listOf() else listOf(receiverTypes[1]),
                    returnType = returnType)
            IrFunctionReferenceImpl(
                    startOffset, endOffset,
                    getterKFunctionType, expression.getter!!, getter, null
            ).apply {
                dispatchReceiver = expression.dispatchReceiver
                extensionReceiver = expression.extensionReceiver
            }
        }

        val setterCallableReference = propertyDescriptor.setter?.let {
            if (!isKMutablePropertyType(expression.type)) null
            else {
                val setterKFunctionType = context.reflectionTypes.getKFunctionType(
                        annotations = Annotations.EMPTY,
                        receiverType = receiverTypes.firstOrNull(),
                        parameterTypes = if (receiverTypes.size < 2) listOf(returnType) else listOf(receiverTypes[1], returnType),
                        returnType = context.builtIns.unitType)
                IrFunctionReferenceImpl(
                        startOffset, endOffset,
                        setterKFunctionType, expression.setter!!, it, null
                ).apply {
                    dispatchReceiver = expression.dispatchReceiver
                    extensionReceiver = expression.extensionReceiver
                }
            }
        }

        val (symbol, descriptor) = getKPropertyImplConstructor(
                receiverTypes = receiverTypes,
                returnType = returnType,
                isLocal = false,
                isMutable = setterCallableReference != null)
        val initializer = IrCallImpl(startOffset, endOffset, symbol, descriptor).apply {
            putValueArgument(0, IrConstImpl<String>(startOffset, endOffset,
                    context.builtIns.stringType, IrConstKind.String, propertyDescriptor.name.asString()))
            if (getterCallableReference != null)
                putValueArgument(1, getterCallableReference)
            if (setterCallableReference != null)
                putValueArgument(2, setterCallableReference)
        }
        return initializer
    }

    private fun createLocalKProperty(expression: IrLocalDelegatedPropertyReference, propertyDescriptor: VariableDescriptorWithAccessors): IrCallImpl {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset

        val returnType = propertyDescriptor.type

        val (symbol, descriptor) = getKPropertyImplConstructor(
                receiverTypes = emptyList(),
                returnType = returnType,
                isLocal = true,
                isMutable = false)

        val initializer = IrCallImpl(startOffset, endOffset, symbol, descriptor).apply {
            putValueArgument(0, IrConstImpl<String>(startOffset, endOffset,
                    context.builtIns.stringType, IrConstKind.String, propertyDescriptor.name.asString()))
        }
        return initializer
    }

    private fun isKMutablePropertyType(type: KotlinType): Boolean {
        val arguments = type.arguments
        val expectedClassDescriptor = when (arguments.size) {
            0 -> return false
            1 -> reflectionTypes.kMutableProperty0
            2 -> reflectionTypes.kMutableProperty1
            3 -> reflectionTypes.kMutableProperty2
            else -> throw AssertionError("More than 2 receivers is not allowed")
        }
        return type == expectedClassDescriptor.defaultType.replace(arguments)
    }

    private object DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION :
            IrDeclarationOriginImpl("KPROPERTIES_FOR_DELEGATION")

    private fun createKPropertiesFieldDescriptor(containingDeclaration: DeclarationDescriptor, fieldType: SimpleType): PropertyDescriptorImpl {
        return PropertyDescriptorImpl.create(containingDeclaration, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE,
                false, "KPROPERTIES".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).initialize(fieldType)
    }
}


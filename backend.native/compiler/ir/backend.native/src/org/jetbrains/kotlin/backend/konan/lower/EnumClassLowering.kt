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

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.descriptors.createValueParameter
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_ENUM
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.backend.common.ir.createArrayOfExpression
import org.jetbrains.kotlin.backend.common.ir.createSimpleDelegatingConstructorDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance

internal class EnumSyntheticFunctionsBuilder(val context: Context) {
    fun buildValuesExpression(startOffset: Int, endOffset: Int,
                              enumClassDescriptor: ClassDescriptor): IrExpression {
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClassDescriptor)

        val typeParameterT = genericValuesDescriptor.typeParameters[0]
        val enumClassType = enumClassDescriptor.defaultType
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
        val substitutedValueOf = genericValuesDescriptor.substitute(typeSubstitutor)!!

        return IrCallImpl(startOffset, endOffset,
                genericValuesSymbol, substitutedValueOf, mapOf(typeParameterT to enumClassType))
                .apply {
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObject.defaultType, loweredEnum.implObject.symbol)
                    putValueArgument(0, IrGetFieldImpl(startOffset, endOffset, loweredEnum.valuesField.symbol, receiver))
                }
    }

    fun buildValueOfExpression(startOffset: Int, endOffset: Int,
                               enumClassDescriptor: ClassDescriptor,
                               value: IrExpression): IrExpression {
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClassDescriptor)

        val typeParameterT = genericValueOfDescriptor.typeParameters[0]
        val enumClassType = enumClassDescriptor.defaultType
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
        val substitutedValueOf = genericValueOfDescriptor.substitute(typeSubstitutor)!!

        return IrCallImpl(startOffset, endOffset,
                genericValueOfSymbol, substitutedValueOf, mapOf(typeParameterT to enumClassType))
                .apply {
                    putValueArgument(0, value)
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObject.defaultType, loweredEnum.implObject.symbol)
                    putValueArgument(1, IrGetFieldImpl(startOffset, endOffset, loweredEnum.valuesField.symbol, receiver))
                }
    }

    private val genericValueOfSymbol = context.ir.symbols.valueOfForEnum
    private val genericValueOfDescriptor = genericValueOfSymbol.descriptor

    private val genericValuesSymbol = context.ir.symbols.valuesForEnum
    private val genericValuesDescriptor = genericValuesSymbol.descriptor
}

internal class EnumUsageLowering(val context: Context)
    : IrElementTransformerVoid(), FileLoweringPass {

    private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val enumClassDescriptor = expression.descriptor.containingDeclaration as ClassDescriptor
        return loadEnumEntry(expression.startOffset, expression.endOffset, enumClassDescriptor, expression.descriptor.name)
    }

    // TODO: remove as soon IR is fixed (there should no be any enum get with GET_OBJECT operation).
    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        if (expression.descriptor.kind != ClassKind.ENUM_ENTRY)
            return super.visitGetObjectValue(expression)
        val enumClassDescriptor = expression.descriptor.containingDeclaration as ClassDescriptor
        return loadEnumEntry(expression.startOffset, expression.endOffset, enumClassDescriptor, expression.descriptor.name)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        val descriptor = expression.descriptor as? FunctionDescriptor
                ?: return expression
        if (descriptor.original != enumValuesDescriptor && descriptor.original != enumValueOfDescriptor)
            return expression

        val genericT = descriptor.original.typeParameters[0]
        val substitutedT = expression.getTypeArgument(genericT)!!
        val classDescriptor = substitutedT.constructor.declarationDescriptor as? ClassDescriptor
                ?: return expression // Type parameter.

        assert (classDescriptor.kind == ClassKind.ENUM_CLASS)

        return if (descriptor.original == enumValuesDescriptor) {
                   enumSyntheticFunctionsBuilder.buildValuesExpression(expression.startOffset, expression.endOffset, classDescriptor)
               } else {
                   val value = expression.getValueArgument(0)!!
                   enumSyntheticFunctionsBuilder.buildValueOfExpression(expression.startOffset, expression.endOffset, classDescriptor, value)
               }
    }

    private val enumValueOfSymbol = context.ir.symbols.enumValueOf
    private val enumValueOfDescriptor = enumValueOfSymbol.descriptor

    private val enumValuesSymbol = context.ir.symbols.enumValues
    private val enumValuesDescriptor = enumValuesSymbol.descriptor

    private fun loadEnumEntry(startOffset: Int, endOffset: Int, enumClassDescriptor: ClassDescriptor, name: Name): IrExpression {
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClassDescriptor)
        val ordinal = loweredEnum.entriesMap[name]!!
        return IrCallImpl(startOffset, endOffset, loweredEnum.itemGetterSymbol, loweredEnum.itemGetterDescriptor).apply {
            dispatchReceiver = IrCallImpl(startOffset, endOffset, loweredEnum.valuesGetter.symbol)
            putValueArgument(0, IrConstImpl.int(startOffset, endOffset, enumClassDescriptor.module.builtIns.intType, ordinal))
        }
    }
}

internal class EnumClassLowering(val context: Context) : ClassLoweringPass {
    fun run(irFile: IrFile) {
        runOnFilePostfix(irFile)
        EnumUsageLowering(context).lower(irFile)
    }

    override fun lower(irClass: IrClass) {
        val descriptor = irClass.descriptor
        if (descriptor.kind != ClassKind.ENUM_CLASS) return
        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(irClass.descriptor)
        private val enumEntryOrdinals = mutableMapOf<ClassDescriptor, Int>()
        private val loweredEnumConstructors = mutableMapOf<ClassConstructorDescriptor, IrConstructor>()
        private val descriptorToIrConstructorWithDefaultArguments = mutableMapOf<ClassConstructorDescriptor, IrConstructor>()
        private val defaultEnumEntryConstructors = mutableMapOf<ClassConstructorDescriptor, IrConstructor>()
        private val loweredEnumConstructorParameters = mutableMapOf<ValueParameterDescriptor, ValueParameterDescriptor>()
        private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

        fun run() {
            insertInstanceInitializerCall()
            assignOrdinalsToEnumEntries()
            lowerEnumConstructors(irClass)
            lowerEnumEntriesClasses()
            val defaultClass = createDefaultClassForEnumEntries()
            lowerEnumClassBody()
            if (defaultClass != null)
                irClass.addChild(defaultClass)
            createImplObject()
        }

        private fun insertInstanceInitializerCall() {
            irClass.transformChildrenVoid(object: IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested
                    return declaration
                }

                override fun visitConstructor(declaration: IrConstructor): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val blockBody = declaration.body as? IrBlockBody
                            ?: throw AssertionError("Unexpected constructor body: ${declaration.body}")
                    if (blockBody.statements.all { it !is IrInstanceInitializerCall }) {
                        blockBody.statements.transformFlat {
                            if (it is IrEnumConstructorCall)
                                listOf(it, IrInstanceInitializerCallImpl(declaration.startOffset, declaration.startOffset,
                                        irClass.symbol))
                            else null
                        }
                    }
                    return declaration
                }
            })
        }

        private fun assignOrdinalsToEnumEntries() {
            var ordinal = 0
            irClass.declarations.forEach {
                if (it is IrEnumEntry) {
                    enumEntryOrdinals.put(it.descriptor, ordinal)
                    ordinal++
                }
            }
        }

        private fun lowerEnumEntriesClasses() {
            irClass.declarations.transformFlat { declaration ->
                if (declaration is IrEnumEntry) {
                    listOfNotNull(declaration, lowerEnumEntryClass(declaration.correspondingClass))
                } else null
            }
        }

        private fun lowerEnumEntryClass(enumEntryClass: IrClass?): IrClass? {
            if (enumEntryClass == null) return null

            lowerEnumConstructors(enumEntryClass)

            return enumEntryClass
        }

        private fun createDefaultClassForEnumEntries(): IrClass? {
            if (!irClass.declarations.any({ it is IrEnumEntry && it.correspondingClass == null })) return null
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val descriptor = irClass.descriptor
            val defaultClassDescriptor = ClassDescriptorImpl(descriptor, "DEFAULT".synthesizedName, Modality.FINAL,
                    ClassKind.CLASS, listOf(descriptor.defaultType), SourceElement.NO_SOURCE, false)
            val defaultClass = IrClassImpl(startOffset, endOffset, IrDeclarationOrigin.DEFINED, defaultClassDescriptor)


            val constructors = mutableSetOf<ClassConstructorDescriptor>()

            descriptor.constructors.forEach {
                val loweredEnumConstructorSymbol = loweredEnumConstructors[it]!!.symbol
                val loweredEnumConstructor = loweredEnumConstructorSymbol.descriptor
                val constructorDescriptor = defaultClassDescriptor.createSimpleDelegatingConstructorDescriptor(loweredEnumConstructor)
                val constructor = defaultClass.addSimpleDelegatingConstructor(
                        loweredEnumConstructorSymbol, constructorDescriptor,
                        DECLARATION_ORIGIN_ENUM)
                constructors.add(constructorDescriptor)
                defaultEnumEntryConstructors.put(loweredEnumConstructor, constructor)

                val irConstructor = descriptorToIrConstructorWithDefaultArguments[loweredEnumConstructor]
                if (irConstructor != null) {
                    it.valueParameters.filter { it.declaresDefaultValue() }.forEach { argument ->
                        val loweredArgument = loweredEnumConstructor.valueParameters[argument.loweredIndex()]
                        val body = irConstructor.getDefault(loweredArgument)!!.deepCopyWithVariables()
                        body.transformChildrenVoid(ParameterMapper(constructor))
                        constructor.putDefault(constructorDescriptor.valueParameters[loweredArgument.index], body)
                    }
                }
            }

            val memberScope = stub<MemberScope>("enum default class")
            defaultClassDescriptor.initialize(memberScope, constructors, null)

            defaultClass.createParameterDeclarations()
            defaultClass.setSuperSymbolsAndAddFakeOverrides(listOf(irClass))

            return defaultClass
        }

        private fun createImplObject() {
            val implObject = loweredEnum.implObject

            val enumEntries = mutableListOf<IrEnumEntry>()
            var i = 0
            while (i < irClass.declarations.size) {
                val declaration = irClass.declarations[i]
                var delete = false
                when (declaration) {
                    is IrEnumEntry -> {
                        enumEntries.add(declaration)
                        delete = true
                    }
                    is IrFunction -> {
                        val body = declaration.body
                        if (body is IrSyntheticBody) {
                            when (body.kind) {
                                IrSyntheticBodyKind.ENUM_VALUEOF ->
                                    declaration.body = createSyntheticValueOfMethodBody(declaration)
                                IrSyntheticBodyKind.ENUM_VALUES ->
                                    declaration.body = createSyntheticValuesMethodBody(declaration)
                            }
                        }
                    }
                }
                if (delete)
                    irClass.declarations.removeAt(i)
                else
                    ++i
            }

            val constructorOfAny = context.ir.symbols.any.constructors.single()

            implObject.addSimpleDelegatingConstructor(
                    constructorOfAny, implObject.descriptor.constructors.single(),
                    DECLARATION_ORIGIN_ENUM)

            implObject.addChild(createSyntheticValuesPropertyDeclaration(enumEntries))
            implObject.addChild(createValuesPropertyInitializer(enumEntries))

            irClass.addChild(implObject)
        }

        private val genericCreateUninitializedInstanceSymbol = context.ir.symbols.createUninitializedInstance
        private val genericCreateUninitializedInstanceDescriptor = genericCreateUninitializedInstanceSymbol.descriptor

        private fun createSyntheticValuesPropertyDeclaration(enumEntries: List<IrEnumEntry>): IrPropertyImpl {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            val irValuesInitializer = context.createArrayOfExpression(irClass.descriptor.defaultType,
                    enumEntries
                            .sortedBy { it.descriptor.name }
                            .map {
                                val enumEntryClass = ((it.initializerExpression!! as IrCall).descriptor as ConstructorDescriptor).constructedClass
                                val typeParameterT = genericCreateUninitializedInstanceDescriptor.typeParameters[0]
                                val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumEntryClass.defaultType)))
                                val substitutedCreateUninitializedInstance = genericCreateUninitializedInstanceDescriptor.substitute(typeSubstitutor)!!
                                IrCallImpl(startOffset, endOffset,
                                        genericCreateUninitializedInstanceSymbol, substitutedCreateUninitializedInstance, mapOf(typeParameterT to enumEntryClass.defaultType)
                                )
                            }, startOffset, endOffset)
            val irField = loweredEnum.valuesField.apply {
                initializer = IrExpressionBodyImpl(startOffset, endOffset, irValuesInitializer)
            }

            val getter = loweredEnum.valuesGetter

            val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                    loweredEnum.implObject.defaultType, loweredEnum.implObject.symbol)
            val value = IrGetFieldImpl(startOffset, endOffset, loweredEnum.valuesField.symbol, receiver)
            val returnStatement = IrReturnImpl(startOffset, endOffset, loweredEnum.valuesGetter.symbol, value)
            getter.body = IrBlockBodyImpl(startOffset, endOffset, listOf(returnStatement))

            return IrPropertyImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM,
                    false, loweredEnum.valuesField.descriptor, irField, getter, null).also {
                it.parent = loweredEnum.implObject
            }
        }

        private val initInstanceSymbol = context.ir.symbols.initInstance

        private val arrayGetSymbol = context.ir.symbols.array.functions.single { it.descriptor.name == Name.identifier("get") }

        private val arrayType = context.builtIns.getArrayType(Variance.INVARIANT, irClass.defaultType)

        private fun createValuesPropertyInitializer(enumEntries: List<IrEnumEntry>): IrAnonymousInitializerImpl {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            return IrAnonymousInitializerImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM, loweredEnum.implObject.descriptor).apply {
                body = context.createIrBuilder(symbol, startOffset, endOffset).irBlockBody(irClass) {
                    val instances = irTemporary(irGetField(irGet(loweredEnum.implObject.thisReceiver!!.symbol), loweredEnum.valuesField.symbol))
                    enumEntries
                            .sortedBy { it.descriptor.name }
                            .withIndex()
                            .forEach {
                                val instance = irCall(arrayGetSymbol).apply {
                                    dispatchReceiver = irGet(instances.symbol)
                                    putValueArgument(0, irInt(it.index))
                                }
                                val initializer = it.value.initializerExpression!! as IrCall
                                +irCall(initInstanceSymbol).apply {
                                    putValueArgument(0, instance)
                                    putValueArgument(1, initializer)
                                }
                            }
                    +irCall(this@EnumClassLowering.context.ir.symbols.freeze, listOf(arrayType)).apply {
                        extensionReceiver = irGet(loweredEnum.implObject.thisReceiver!!.symbol)
                    }
                }
            }
        }

        private fun createSyntheticValuesMethodBody(declaration: IrFunction): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val valuesExpression = enumSyntheticFunctionsBuilder.buildValuesExpression(startOffset, endOffset, irClass.descriptor)

            return IrBlockBodyImpl(startOffset, endOffset,
                    listOf(IrReturnImpl(startOffset, endOffset, declaration.symbol, valuesExpression))
            )
        }

        private fun createSyntheticValueOfMethodBody(declaration: IrFunction): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val value = IrGetValueImpl(startOffset, endOffset, declaration.valueParameters[0].symbol)
            val valueOfExpression = enumSyntheticFunctionsBuilder.buildValueOfExpression(startOffset, endOffset, irClass.descriptor, value)

            return IrBlockBodyImpl(
                    startOffset, endOffset,
                    listOf(IrReturnImpl(startOffset, endOffset, declaration.symbol, valueOfExpression))
            )
        }

        private fun lowerEnumConstructors(irClass: IrClass) {
            irClass.declarations.forEachIndexed { index, declaration ->
                if (declaration is IrConstructor)
                    irClass.declarations[index] = transformEnumConstructor(declaration)
            }
        }

        private fun transformEnumConstructor(enumConstructor: IrConstructor): IrConstructor {
            val constructorDescriptor = enumConstructor.descriptor
            val loweredConstructorDescriptor = lowerEnumConstructor(constructorDescriptor)
            val loweredEnumConstructor = IrConstructorImpl(
                    enumConstructor.startOffset, enumConstructor.endOffset, enumConstructor.origin,
                    loweredConstructorDescriptor,
                    enumConstructor.body!! // will be transformed later
            )
            loweredEnumConstructor.parent = enumConstructor.parent

            loweredEnumConstructors[constructorDescriptor] = loweredEnumConstructor

            loweredEnumConstructor.createParameterDeclarations()

            enumConstructor.descriptor.valueParameters.filter { it.declaresDefaultValue() }.forEach {
                val body = enumConstructor.getDefault(it)!!
                body.transformChildrenVoid(object: IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        val descriptor = expression.descriptor
                        when (descriptor) {
                            is ValueParameterDescriptor -> {
                                return IrGetValueImpl(expression.startOffset,
                                        expression.endOffset,
                                        loweredEnumConstructor.valueParameters[descriptor.loweredIndex()].symbol)
                            }
                        }
                        return expression
                    }
                })
                loweredEnumConstructor.putDefault(loweredConstructorDescriptor.valueParameters[it.loweredIndex()], body)
                descriptorToIrConstructorWithDefaultArguments[loweredConstructorDescriptor] = loweredEnumConstructor
            }
            return loweredEnumConstructor
        }

        private fun lowerEnumConstructor(constructorDescriptor: ClassConstructorDescriptor): ClassConstructorDescriptor {
            val loweredConstructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
                    constructorDescriptor.containingDeclaration,
                    constructorDescriptor.annotations,
                    constructorDescriptor.isPrimary,
                    constructorDescriptor.source
            )

            val valueParameters =
                    listOf(
                            loweredConstructorDescriptor.createValueParameter(0, "name", context.builtIns.stringType),
                            loweredConstructorDescriptor.createValueParameter(1, "ordinal", context.builtIns.intType)
                    ) +
                            constructorDescriptor.valueParameters.map {
                                lowerConstructorValueParameter(loweredConstructorDescriptor, it)
                            }
            loweredConstructorDescriptor.initialize(valueParameters, Visibilities.PROTECTED)

            loweredConstructorDescriptor.returnType = constructorDescriptor.returnType

            return loweredConstructorDescriptor
        }

        private fun lowerConstructorValueParameter(
                loweredConstructorDescriptor: ClassConstructorDescriptor,
                valueParameterDescriptor: ValueParameterDescriptor
        ): ValueParameterDescriptor {
            val loweredValueParameterDescriptor = valueParameterDescriptor.copy(
                    loweredConstructorDescriptor,
                    valueParameterDescriptor.name,
                    valueParameterDescriptor.loweredIndex()
            )
            loweredEnumConstructorParameters[valueParameterDescriptor] = loweredValueParameterDescriptor
            return loweredValueParameterDescriptor
        }

        private fun lowerEnumClassBody() {
            irClass.transformChildrenVoid(EnumClassBodyTransformer())
        }

        private inner class InEnumClassConstructor(val enumClassConstructor: IrConstructor) :
                EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset
                val origin = enumConstructorCall.origin

                val result = IrDelegatingConstructorCallImpl(startOffset, endOffset,
                        enumConstructorCall.symbol, enumConstructorCall.descriptor)

                assert(result.descriptor.valueParameters.size == 2) {
                    "Enum(String, Int) constructor call expected:\n${result.dump()}"
                }

                val nameParameter = enumClassConstructor.valueParameters.getOrElse(0) {
                    throw AssertionError("No 'name' parameter in enum constructor: $enumClassConstructor")
                }

                val ordinalParameter = enumClassConstructor.valueParameters.getOrElse(1) {
                    throw AssertionError("No 'ordinal' parameter in enum constructor: $enumClassConstructor")
                }

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, nameParameter.symbol, origin))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, ordinalParameter.symbol, origin))

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                val descriptor = delegatingConstructorCall.descriptor
                val startOffset = delegatingConstructorCall.startOffset
                val endOffset = delegatingConstructorCall.endOffset

                val loweredDelegatedConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = IrDelegatingConstructorCallImpl(startOffset, endOffset,
                        loweredDelegatedConstructor.symbol, loweredDelegatedConstructor.descriptor)

                result.putValueArgument(0,
                        IrGetValueImpl(startOffset, endOffset, enumClassConstructor.valueParameters[0].symbol))
                result.putValueArgument(1,
                        IrGetValueImpl(startOffset, endOffset, enumClassConstructor.valueParameters[1].symbol))

                descriptor.valueParameters.forEach { valueParameter ->
                    result.putValueArgument(valueParameter.loweredIndex(), delegatingConstructorCall.getValueArgument(valueParameter))
                }

                return result
            }
        }

        private abstract inner class InEnumEntry(private val enumEntry: ClassDescriptor) : EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val name = enumEntry.name.asString()
                val ordinal = enumEntryOrdinals[enumEntry]!!

                val descriptor = enumConstructorCall.descriptor
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset

                val loweredConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = createConstructorCall(startOffset, endOffset, loweredConstructor.symbol)

                result.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.builtIns.stringType, name))
                result.putValueArgument(1, IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, ordinal))

                descriptor.valueParameters.forEach { valueParameter ->
                    val i = valueParameter.index
                    result.putValueArgument(i + 2, enumConstructorCall.getValueArgument(i))
                }

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                throw AssertionError("Unexpected delegating constructor call within enum entry: $enumEntry")
            }

            abstract fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol): IrMemberAccessExpression
        }

        private inner class InEnumEntryClassConstructor(enumEntry: ClassDescriptor) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol)
                    = IrDelegatingConstructorCallImpl(startOffset, endOffset, loweredConstructor, loweredConstructor.descriptor)
        }

        private inner class InEnumEntryInitializer(enumEntry: ClassDescriptor) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol): IrCall {
                return IrCallImpl(startOffset, endOffset,
                        defaultEnumEntryConstructors[loweredConstructor.descriptor]?.symbol ?: loweredConstructor)
            }
        }

        private inner class EnumClassBodyTransformer : IrElementTransformerVoid() {
            private var enumConstructorCallTransformer: EnumConstructorCallTransformer? = null

            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.kind == ClassKind.ENUM_CLASS)
                    return declaration
                return super.visitClass(declaration)
            }

            override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
                assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }

                enumConstructorCallTransformer = InEnumEntryInitializer(declaration.descriptor)

                var result: IrEnumEntry = IrEnumEntryImpl(declaration.startOffset, declaration.endOffset, declaration.origin,
                        declaration.descriptor, null, declaration.initializerExpression)
                result = super.visitEnumEntry(result) as IrEnumEntry

                enumConstructorCallTransformer = null

                return result
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                val constructorDescriptor = declaration.descriptor
                val containingClass = constructorDescriptor.containingDeclaration

                // TODO local (non-enum) class in enum class constructor?
                val previous = enumConstructorCallTransformer

                if (containingClass.kind == ClassKind.ENUM_ENTRY) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumEntryClassConstructor(containingClass)
                } else if (containingClass.kind == ClassKind.ENUM_CLASS) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumClassConstructor(declaration)
                }

                val result = super.visitConstructor(declaration)

                enumConstructorCallTransformer = previous

                return result
            }

            override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val callTransformer = enumConstructorCallTransformer ?:
                        throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" + irClass.dump())


                return callTransformer.transform(expression)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.descriptor.containingDeclaration.kind == ClassKind.ENUM_CLASS) {
                    val callTransformer = enumConstructorCallTransformer ?:
                            throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" + irClass.dump())

                    return callTransformer.transform(expression)
                }
                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val loweredParameter = loweredEnumConstructorParameters[expression.descriptor]
                if (loweredParameter != null) {
                    val loweredEnumConstructor = loweredEnumConstructors[expression.descriptor.containingDeclaration]!!
                    val loweredIrParameter = loweredEnumConstructor.valueParameters[loweredParameter.index]
                    assert(loweredIrParameter.descriptor == loweredParameter)
                    return IrGetValueImpl(expression.startOffset, expression.endOffset,
                            loweredIrParameter.symbol, expression.origin)
                } else {
                    return expression
                }
            }
        }
    }
}

private fun ValueParameterDescriptor.loweredIndex(): Int = index + 2

private class ParameterMapper(val originalConstructor: IrConstructor) : IrElementTransformerVoid() {
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val descriptor = expression.descriptor
        when (descriptor) {
            is ValueParameterDescriptor -> {
                return IrGetValueImpl(expression.startOffset,
                        expression.endOffset,
                        originalConstructor.valueParameters[descriptor.index].symbol)
            }
        }
        return expression
    }
}

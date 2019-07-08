/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.EnumWhenLowering
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_ENUM
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

internal class EnumSyntheticFunctionsBuilder(val context: Context) {
    fun buildValuesExpression(startOffset: Int, endOffset: Int,
                              enumClass: IrClass): IrExpression {

        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClass)

        return irCall(startOffset, endOffset, genericValuesSymbol.owner, listOf(enumClass.defaultType))
                .apply {
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObject.defaultType,
                            loweredEnum.implObject.symbol)
                    putValueArgument(0, IrGetFieldImpl(
                            startOffset,
                            endOffset,
                            loweredEnum.valuesField.symbol,
                            loweredEnum.valuesField.type,
                            receiver
                    ))
                }
    }

    fun buildValueOfExpression(startOffset: Int, endOffset: Int,
                               enumClass: IrClass,
                               value: IrExpression): IrExpression {
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClass)

        return irCall(startOffset, endOffset, genericValueOfSymbol.owner, listOf(enumClass.defaultType))
                .apply {
                    putValueArgument(0, value)
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObject.defaultType, loweredEnum.implObject.symbol)
                    putValueArgument(1, IrGetFieldImpl(
                            startOffset,
                            endOffset,
                            loweredEnum.valuesField.symbol,
                            loweredEnum.valuesField.type,
                            receiver
                    ))
                }
    }

    private val genericValueOfSymbol = context.ir.symbols.valueOfForEnum

    private val genericValuesSymbol = context.ir.symbols.valuesForEnum
}

internal class EnumUsageLowering(val context: Context)
    : IrElementTransformerVoid(), FileLoweringPass {

    private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val entry = expression.symbol.owner
        return loadEnumEntry(
                expression.startOffset,
                expression.endOffset,
                entry.parentAsClass,
                entry.name
        )
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        if (expression.symbol != enumValuesSymbol && expression.symbol != enumValueOfSymbol)
            return expression

        val irClassSymbol = expression.getTypeArgument(0)!!.classifierOrNull as? IrClassSymbol
                ?: return expression // Type parameter.

        if (irClassSymbol == context.ir.symbols.enum) return expression // Type parameter erased to 'Enum'.

        val irClass = irClassSymbol.owner

        assert (irClass.kind == ClassKind.ENUM_CLASS)

        return if (expression.symbol == enumValuesSymbol) {
            enumSyntheticFunctionsBuilder.buildValuesExpression(expression.startOffset, expression.endOffset, irClass)
        } else {
            val value = expression.getValueArgument(0)!!
            enumSyntheticFunctionsBuilder.buildValueOfExpression(expression.startOffset, expression.endOffset, irClass, value)
        }
    }

    private val enumValueOfSymbol = context.ir.symbols.enumValueOf

    private val enumValuesSymbol = context.ir.symbols.enumValues

    private fun loadEnumEntry(startOffset: Int, endOffset: Int, enumClass: IrClass, name: Name): IrExpression {
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClass)
        val ordinal = loweredEnum.entriesMap[name]!!
        return IrCallImpl(
                startOffset, endOffset, enumClass.defaultType,
                loweredEnum.itemGetterSymbol.owner.symbol, loweredEnum.itemGetterSymbol.descriptor,
                typeArgumentsCount = 0
        ).apply {
            dispatchReceiver = IrCallImpl(startOffset, endOffset, loweredEnum.valuesGetter.returnType, loweredEnum.valuesGetter.symbol)
            putValueArgument(0, IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, ordinal))
        }
    }
}

internal class EnumClassLowering(val context: Context) : ClassLoweringPass {

    fun run(irFile: IrFile) {
        // EnumWhenLowering should be performed before EnumUsageLowering because
        // the latter performs lowering of IrGetEnumValue
        EnumWhenLowering(context).lower(irFile)
        runOnFilePostfix(irFile)
        EnumUsageLowering(context).lower(irFile)
    }

    override fun lower(irClass: IrClass) {
        if (irClass.kind != ClassKind.ENUM_CLASS) return
        EnumClassTransformer(irClass).run()
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(irClass)
        private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

        fun run() {
            pullUpEnumEntriesClasses()
            createImplObject()
        }

        private fun pullUpEnumEntriesClasses() {
            irClass.declarations.transformFlat { declaration ->
                if (declaration is IrEnumEntry) {
                    val correspondingClass = declaration.correspondingClass
                    declaration.correspondingClass = null
                    listOfNotNull(declaration, correspondingClass)
                } else null
            }
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

            implObject.declarations += createSyntheticValuesPropertyDeclaration(enumEntries)


            irClass.declarations += implObject
        }

        private val createUninitializedInstance = context.ir.symbols.createUninitializedInstance.owner

        private fun createSyntheticValuesPropertyDeclaration(enumEntries: List<IrEnumEntry>): IrPropertyImpl {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            val implObject = loweredEnum.implObject
            val constructor = implObject.constructors.single()

            val irValuesInitializer = context.createArrayOfExpression(
                    startOffset, endOffset,
                    irClass.defaultType,
                    enumEntries
                            .sortedBy { it.name }
                            .map {
                                val initializer = it.initializerExpression
                                val entryConstructorCall = when {
                                    initializer is IrConstructorCall -> initializer

                                    initializer is IrBlock && initializer.origin == ARGUMENTS_REORDERING_FOR_CALL ->
                                        initializer.statements.last() as IrConstructorCall

                                    else -> error("Unexpected initializer: $initializer")
                                }
                                val entryClass = entryConstructorCall.symbol.owner.constructedClass

                                irCall(startOffset, endOffset,
                                        createUninitializedInstance,
                                        listOf(entryClass.defaultType)
                                )

                            }
            )
            val irField = loweredEnum.valuesField
            context.createIrBuilder(constructor.symbol).run {
                (constructor.body as IrBlockBody).statements +=
                        irSetField(irGet(implObject.thisReceiver!!), irField, irValuesInitializer)
            }

            val getter = loweredEnum.valuesGetter
            context.createIrBuilder(getter.symbol).run {
                getter.body = irBlockBody(irClass) { +irReturn(irGetField(irGetObject(implObject.symbol), irField)) }
            }

            createValuesPropertyInitializer(enumEntries)

            return IrPropertyImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM,
                    false, loweredEnum.valuesField.descriptor, irField, getter, null).apply {
                parent = implObject
            }
        }

        private val initInstanceSymbol = context.ir.symbols.initInstance

        private val arrayGetSymbol = context.ir.symbols.array.functions.single { it.owner.name == Name.identifier("get") }

        private val arrayType = context.ir.symbols.array.typeWith(irClass.defaultType)

        private fun createValuesPropertyInitializer(enumEntries: List<IrEnumEntry>) {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            fun IrBlockBuilder.initInstanceCall(instance: IrCall, constructor: IrConstructorCall): IrCall =
                    irCall(initInstanceSymbol).apply {
                        putValueArgument(0, instance)
                        putValueArgument(1, constructor)
                    }

            val implObject = loweredEnum.implObject
            val constructor = implObject.constructors.single()
            val irBuilder = context.createIrBuilder(constructor.symbol, startOffset, endOffset)
            val valuesInitializer = irBuilder.irBlock(startOffset, endOffset) {
                val receiver = implObject.thisReceiver!!
                val instances = irTemporary(irGetField(irGet(receiver), loweredEnum.valuesField))
                enumEntries
                        .sortedBy { it.name }
                        .withIndex()
                        .forEach {
                            val instance = irCall(arrayGetSymbol).apply {
                                dispatchReceiver = irGet(instances)
                                putValueArgument(0, irInt(it.index))
                            }
                            val initializer = it.value.initializerExpression!!
                            initializer.setDeclarationsParent(constructor)
                            when {
                                initializer is IrConstructorCall -> +initInstanceCall(instance, initializer)

                                initializer is IrBlock && initializer.origin == ARGUMENTS_REORDERING_FOR_CALL -> {
                                    val statements = initializer.statements
                                    val constructorCall = statements.last() as IrConstructorCall
                                    statements[statements.lastIndex] = initInstanceCall(instance, constructorCall)
                                    +initializer
                                }

                                else -> error("Unexpected initializer: $initializer")
                            }
                        }
                +irCall(this@EnumClassLowering.context.ir.symbols.freeze, listOf(arrayType)).apply {
                    extensionReceiver = irGet(receiver)
                }
            }
            (constructor.body as IrBlockBody).statements += valuesInitializer
        }

        private fun createSyntheticValuesMethodBody(declaration: IrFunction): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val valuesExpression = enumSyntheticFunctionsBuilder.buildValuesExpression(startOffset, endOffset, irClass)

            return IrBlockBodyImpl(startOffset, endOffset).apply {
                statements += IrReturnImpl(
                        startOffset,
                        endOffset,
                        context.irBuiltIns.nothingType,
                        declaration.symbol,
                        valuesExpression
                )
            }
        }

        private fun createSyntheticValueOfMethodBody(declaration: IrFunction): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val parameter = declaration.valueParameters[0]
            val value = IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol)
            val valueOfExpression = enumSyntheticFunctionsBuilder.buildValueOfExpression(startOffset, endOffset, irClass, value)

            return IrBlockBodyImpl(startOffset, endOffset).apply {
                statements += IrReturnImpl(
                        startOffset,
                        endOffset,
                        context.irBuiltIns.nothingType,
                        declaration.symbol,
                        valueOfExpression
                )
            }
        }

    }
}

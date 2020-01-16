/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.common.ir.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.backend.konan.descriptors.getArgumentValueOrNull
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.irBuilder
import org.jetbrains.kotlin.name.FqName

internal class CEnumCompanionGenerator(
        override val irBuiltIns: IrBuiltIns,
        override val symbolTable: SymbolTable,
        override val typeTranslator: TypeTranslator
) : DescriptorToIrTranslationMixin {

    companion object {
        private val cEnumEntryAliasAnnonation = FqName("kotlinx.cinterop.internal.CEnumEntryAlias")
    }

    private val cEnumByValueFunctionGenerator =
            CEnumByValueFunctionGenerator(irBuiltIns, symbolTable, typeTranslator)

    // Depends on already generated `.values()` irFunction.
    fun generate(enumClass: IrClass): IrClass =
            createClass(enumClass.descriptor.companionObjectDescriptor!!) { companionIrClass ->
                companionIrClass.superTypes += irBuiltIns.anyType
                companionIrClass.addSimpleDelegatingConstructor(
                        irBuiltIns.anyClass.owner.constructors.first(),
                        irBuiltIns,
                        isPrimary = true
                )
                val valuesFunction = enumClass.functions.single { it.name.identifier == "values" }.symbol
                val byValueIrFunction = cEnumByValueFunctionGenerator
                        .generateByValueFunction(companionIrClass, valuesFunction)
                companionIrClass.addMember(byValueIrFunction)
                findEntryAliases(companionIrClass.descriptor)
                        .map { declareEntryAliasProperty(it, enumClass) }
                        .forEach(companionIrClass::addMember)
            }

    /**
     * Returns all properties in companion object that represent aliases to
     * enum entries.
     */
    private fun findEntryAliases(companionDescriptor: ClassDescriptor) =
            companionDescriptor.defaultType.memberScope.getContributedDescriptors()
                    .filterIsInstance<PropertyDescriptor>()
                    .filter { it.annotations.hasAnnotation(cEnumEntryAliasAnnonation) }

    private fun fundCorrespondingEnumEntrySymbol(aliasDescriptor: PropertyDescriptor, irClass: IrClass): IrEnumEntrySymbol {
        val enumEntryName = aliasDescriptor.annotations
                .findAnnotation(cEnumEntryAliasAnnonation)!!
                .getArgumentValueOrNull<String>("entryName")
        return irClass.declarations.filterIsInstance<IrEnumEntry>()
                .single { it.name.identifier == enumEntryName }.symbol
    }

    private fun generateAliasGetterBody(getter: IrSimpleFunction, entrySymbol: IrEnumEntrySymbol): IrBody =
            irBuilder(irBuiltIns, getter.symbol).irBlockBody {
                +irReturn(
                        IrGetEnumValueImpl(startOffset, endOffset, entrySymbol.owner.parentAsClass.defaultType, entrySymbol)
                )
            }

    private fun declareEntryAliasProperty(propertyDescriptor: PropertyDescriptor, enumClass: IrClass): IrProperty {
        val irProperty = createProperty(propertyDescriptor)
        irProperty.getter = declareSimpleIrFunction(propertyDescriptor.getter!!).also { getter ->
            getter.correspondingPropertySymbol = irProperty.symbol
            val entrySymbol = fundCorrespondingEnumEntrySymbol(propertyDescriptor, enumClass)
            getter.body = generateAliasGetterBody(getter, entrySymbol)
        }
        return irProperty
    }
}
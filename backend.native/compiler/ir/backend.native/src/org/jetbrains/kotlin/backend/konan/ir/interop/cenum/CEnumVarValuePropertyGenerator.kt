/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.objcexport.getErasedTypeClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnsignedNumberType

/**
 * Generate IR for `value` property for (E: CEnum).Var class.
 */
internal class CEnumVarValuePropertyGenerator(
        override val irBuiltIns: IrBuiltIns,
        override val symbolTable: SymbolTable,
        override val typeTranslator: TypeTranslator,
        interopBuiltIns: InteropBuiltIns
) : DescriptorToIrTranslationMixin {

    private val nativeMemUtilsDesc: ClassDescriptor = interopBuiltIns.packageScope
            .getContributedClassifier(Name.identifier("nativeMemUtils"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private val nativeMemUtilsSymbol = symbolTable.referenceClass(nativeMemUtilsDesc)

    private fun findMemoryOperationsForType(primitiveType: KotlinType): Pair<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol> {
        val signedPrimitiveType = if (primitiveType.isUnsignedNumberType()) {
            val property = primitiveType.getErasedTypeClass()
                    .unsubstitutedMemberScope.getContributedDescriptors()
                    .single { it.name.identifier == "data" } as PropertyDescriptor
            property.type
        } else {
            primitiveType
        }
        val readPrimitiveSymbol = nativeMemUtilsDesc
                .unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("get${signedPrimitiveType}"), NoLookupLocation.FROM_BACKEND).single()
                .let(symbolTable::referenceSimpleFunction)
        val writePrimitiveSymbol = nativeMemUtilsDesc
                .unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("put${signedPrimitiveType}"), NoLookupLocation.FROM_BACKEND).single()
                .let(symbolTable::referenceSimpleFunction)
        return readPrimitiveSymbol to writePrimitiveSymbol
    }

    fun generate(enumClass: IrClass, enumVarClass: IrClass): IrProperty {
        val valuePropertyDescriptor = enumVarClass.descriptor.unsubstitutedMemberScope
                .getContributedVariables(Name.identifier("value"), NoLookupLocation.FROM_BACKEND).single()
        val enumCompanionObject = enumClass.companionObject()!! as IrClass
        val entryValueGetter = enumClass.properties.single { it.name.identifier == "value" }.getter!!
        val byValueIrFunction = enumCompanionObject.functions.single { it.name.identifier == "byValue" }
        val primitiveType = byValueIrFunction.descriptor.valueParameters[0].type
        val (readPrimitiveSymbol, writePrimitiveSymbol) = findMemoryOperationsForType(primitiveType)
        val irValueProperty = createProperty(valuePropertyDescriptor)
        symbolTable.withScope(valuePropertyDescriptor) {
            irValueProperty.getter = declareSimpleIrFunction(valuePropertyDescriptor.getter!!).also { getter ->
                getter.correspondingPropertySymbol = irValueProperty.symbol
                // Signed base type T:
                //  byValue(nativeMemUtils.getT($this))
                // Unsigned base type UT:
                //  byValue(UT(nativeMemUtils.getT($this)))
                getter.body = irBuilder(irBuiltIns, getter.symbol).irBlockBody {
                    val memoryValue = irCall(readPrimitiveSymbol).apply {
                        dispatchReceiver = irGetObject(nativeMemUtilsSymbol)
                        putValueArgument(0, irGet(getter.dispatchReceiverParameter!!))
                    }
                    val memoryValueCorrectType = if (primitiveType.isUnsignedNumberType()) {
                        val primaryConstructor = primitiveType.getErasedTypeClass().unsubstitutedPrimaryConstructor!!
                        val unsignedClassConstructor = symbolTable.referenceConstructor(primaryConstructor)
                        irCall(unsignedClassConstructor).also {
                            it.putValueArgument(0, memoryValue)
                        }
                    } else {
                        memoryValue
                    }
                    val byValueResult = irCall(byValueIrFunction).also {
                        it.dispatchReceiver = irGetObject(enumCompanionObject.symbol)
                        it.putValueArgument(0, memoryValueCorrectType)
                    }
                    +irReturn(byValueResult)
                }
            }
            irValueProperty.setter = declareSimpleIrFunction(valuePropertyDescriptor.setter!!).also { setter ->
                setter.correspondingPropertySymbol = irValueProperty.symbol
                // Signed base type T:
                //  nativeMemUtils.putT($this, value.value)
                // Unsigned base type UT:
                //  nativeMemUtils.putT($this, value.value.data)
                setter.body = irBuilder(irBuiltIns, setter.symbol).irBlockBody {
                    val enumEntryValue = irCall(entryValueGetter).also {
                        it.dispatchReceiver = irGet(setter.valueParameters[0])
                    }
                    val valueToWrite = if (primitiveType.isUnsignedNumberType()) {
                        val data = primitiveType.getErasedTypeClass()
                                .unsubstitutedMemberScope.getContributedDescriptors()
                                .single { it.name.identifier == "data" } as PropertyDescriptor
                        irCall(data.getter!!.let(symbolTable::referenceSimpleFunction)).also {
                            it.dispatchReceiver = enumEntryValue
                        }
                    } else {
                        enumEntryValue
                    }
                    +irCall(writePrimitiveSymbol).also { call ->
                        call.dispatchReceiver = irGetObject(nativeMemUtilsSymbol)
                        call.putValueArgument(0, irGet(setter.dispatchReceiverParameter!!))
                        call.putValueArgument(1, valueToWrite)
                    }
                }
            }
        }
        return irValueProperty
    }
}
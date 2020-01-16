/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.findDeclarationByName
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Generate IR for function that returns appropriate enum entry for the provided integral value.
 */
internal class CEnumByValueFunctionGenerator(
        override val irBuiltIns: IrBuiltIns,
        override val symbolTable: SymbolTable,
        override val typeTranslator: TypeTranslator
) : DescriptorToIrTranslationMixin {
    fun generateByValueFunction(
            companionIrClass: IrClass,
            valuesIrFunctionSymbol: IrSimpleFunctionSymbol
    ): IrFunction {
        val byValueFunctionDescriptor = companionIrClass.descriptor.findDeclarationByName<FunctionDescriptor>("byValue")!!
        val byValueIrFunction = declareSimpleIrFunction(byValueFunctionDescriptor)
        val irValueParameter = byValueIrFunction.valueParameters.first()
        // checkNotNull {
        // val values: Array<E> = values()
        // val i: Int = 0
        // val size: Int = values.size
        // while (i < size) {
        //      val entry: E = values[i]
        //      val entryValue = entry.value
        //      if (entryValue == arg) {
        //          return entry
        //      }
        //      i++
        // }
        // return null
        // }
        byValueIrFunction.body = irBuilder(irBuiltIns, byValueIrFunction.symbol).irBlockBody {
            +irReturn(irForceNotNull(irBlock {
                val values = irTemporaryVar(irCall(valuesIrFunctionSymbol))
                val inductionVariable = irTemporaryVar(irInt(0))
                val valuesSize = irCall(values.type.getClass()!!.getPropertyGetter("size")!!.owner).also { irCall ->
                    irCall.dispatchReceiver = irGet(values)
                }
                val getElementFn = values.type.getClass()!!.functions.single {
                    it.name == OperatorNameConventions.GET &&
                            it.valueParameters.size == 1 &&
                            it.valueParameters[0].type.isInt()
                }
                val plusFun = inductionVariable.type.getClass()!!.functions.single {
                    it.name == OperatorNameConventions.PLUS &&
                            it.valueParameters.size == 1 &&
                            it.valueParameters[0].type == irBuiltIns.intType
                }
                val lessFunctionSymbol = irBuiltIns.lessFunByOperandType.getValue(irBuiltIns.intClass)
                +irWhile().also { loop ->
                    loop.condition = irCall(lessFunctionSymbol).also { irCall ->
                        irCall.putValueArgument(0, irGet(inductionVariable))
                        irCall.putValueArgument(1, valuesSize)
                    }
                    loop.body = irBlock {
                        val untypedEntry = irCall(getElementFn).also { irCall ->
                            irCall.dispatchReceiver = irGet(values)
                            irCall.putValueArgument(0, irGet(inductionVariable))
                        }
                        val entry = irTemporaryVar(irImplicitCast(untypedEntry, byValueIrFunction.returnType))
                        val valueGetter = entry.type.getClass()!!.getPropertyGetter("value")!!
                        val entryValue = irTemporaryVar(irGet(irValueParameter.type, irGet(entry), valueGetter))
                        +irIfThenElse(
                                type = irBuiltIns.unitType,
                                condition = irEquals(irGet(entryValue), irGet(irValueParameter)),
                                thenPart = irReturn(irGet(entry)),
                                elsePart = irSetVar(inductionVariable, irCallOp(plusFun, irGet(inductionVariable), irInt(1)))
                        )
                    }
                }
                +irReturn(irNull())
            }))
        }
        return byValueIrFunction
    }
}
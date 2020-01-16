/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop

import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType

internal inline fun <reified T: DeclarationDescriptor> ClassDescriptor.findDeclarationByName(name: String): T? =
        defaultType.memberScope
                .getContributedDescriptors()
                .filterIsInstance<T>()
                .firstOrNull { it.name.identifier == name }

/**
 * Provides a set of functions and properties that helps
 * to translate descriptor declarations to corresponding IR.
 */
internal interface DescriptorToIrTranslationMixin {

    val symbolTable: SymbolTable

    val irBuiltIns: IrBuiltIns

    val typeTranslator: TypeTranslator

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun createConstructor(constructorDescriptor: ClassConstructorDescriptor): IrConstructor {
        val irConstructor = symbolTable.declareConstructor(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, constructorDescriptor
        )
        constructorDescriptor.valueParameters.mapTo(irConstructor.valueParameters) { valueParameterDescriptor ->
            symbolTable.declareValueParameter(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED,
                    valueParameterDescriptor,
                    valueParameterDescriptor.type.toIrType()).also {
                it.parent = irConstructor
            }
        }
        irConstructor.returnType = constructorDescriptor.returnType.toIrType()
        return irConstructor
    }

    fun createProperty(propertyDescriptor: PropertyDescriptor): IrProperty {
        val irValueProperty = symbolTable.declareProperty(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, propertyDescriptor)
        return irValueProperty
    }

    /**
     * Declares [IrClass] instance from [descriptor] and populates it with
     * supertypes, <this> parameter declaration and fake overrides.
     * Additional elements are passed via [builder] callback.
     */
    fun createClass(descriptor: ClassDescriptor, builder: (IrClass) -> Unit): IrClass =
            symbolTable.declareClass(
                    startOffset = SYNTHETIC_OFFSET,
                    endOffset = SYNTHETIC_OFFSET,
                    origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    descriptor = descriptor
            ).also { irClass ->
                symbolTable.withScope(descriptor) {
                    descriptor.typeConstructor.supertypes.mapTo(irClass.superTypes) {
                        it.toIrType()
                    }
                    irClass.createParameterDeclarations()
                    builder(irClass)
                    createFakeOverrides(descriptor).forEach(irClass::addMember)
                }
            }

    private fun createFakeOverrides(classDescriptor: ClassDescriptor): List<IrDeclaration> {
        val fakeOverrides = classDescriptor.unsubstitutedMemberScope
                .getContributedDescriptors()
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
        return fakeOverrides.map {
            when (it) {
                is PropertyDescriptor -> createFakeOverrideProperty(it)
                is FunctionDescriptor -> createFakeOverrideFunction(it)
                else -> error("Unexpected fake override descriptor: $it")
            } as IrDeclaration // Assistance for type inference.
        }
    }

    private fun createFakeOverrideProperty(propertyDescriptor: PropertyDescriptor): IrProperty {
        val irProperty = symbolTable.declareProperty(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.FAKE_OVERRIDE, propertyDescriptor)
        irProperty.getter = propertyDescriptor.getter?.let {
            val irGetter = createFakeOverrideFunction(it)
            irGetter.correspondingPropertySymbol = irProperty.symbol
            irGetter
        }
        irProperty.setter = propertyDescriptor.setter?.let {
            val irSetter = createFakeOverrideFunction(it)
            irSetter.correspondingPropertySymbol = irProperty.symbol
            irSetter
        }
        return irProperty
    }

    fun createFakeOverrideFunction(
            functionDescriptor: FunctionDescriptor,
            origin: IrDeclarationOrigin = IrDeclarationOrigin.FAKE_OVERRIDE
    ): IrSimpleFunction {
        val irFunction = symbolTable.declareSimpleFunctionWithOverrides(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, origin, functionDescriptor)
        symbolTable.withScope(functionDescriptor) {
            irFunction.returnType = functionDescriptor.returnType!!.toIrType()
            functionDescriptor.valueParameters.mapTo(irFunction.valueParameters) {
                symbolTable.declareValueParameter(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED, it, it.type.toIrType())
            }
            irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
                symbolTable.declareValueParameter(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED, it, it.type.toIrType())
            }
        }
        return irFunction
    }

    fun declareSimpleIrFunction(
            functionDescriptor: FunctionDescriptor,
            declarationOrigin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    ): IrSimpleFunction {
        val irFunction = symbolTable.declareSimpleFunction(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, declarationOrigin, functionDescriptor)
        symbolTable.withScope(functionDescriptor) {
            irFunction.returnType = functionDescriptor.returnType!!.toIrType()
            functionDescriptor.valueParameters.mapTo(irFunction.valueParameters) {
                symbolTable.declareValueParameter(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED, it, it.type.toIrType())
            }
            irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
                symbolTable.declareValueParameter(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED, it, it.type.toIrType())
            }
        }
        return irFunction
    }
}

private fun ClassDescriptor.implementsCEnum(interopBuiltIns: InteropBuiltIns): Boolean =
        interopBuiltIns.cEnum in this.getAllSuperClassifiers()

/**
 * All enums that come from interop library implement CEnum interface.
 * This function checks that given symbol located in subtree of
 * CEnum inheritor.
 */
internal fun IrSymbol.findCEnumDescriptor(interopBuiltIns: InteropBuiltIns): ClassDescriptor? =
        descriptor.parentsWithSelf
                .filterIsInstance<ClassDescriptor>()
                .firstOrNull { it.implementsCEnum(interopBuiltIns) }
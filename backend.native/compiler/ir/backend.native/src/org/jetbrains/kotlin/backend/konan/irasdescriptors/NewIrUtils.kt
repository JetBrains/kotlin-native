/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.irasdescriptors

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.descriptors.backingField
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.toKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType

val IrConstructor.constructedClass get() = this.parent as IrClass

val <T : IrDeclaration> T.original get() = this
val IrDeclaration.containingDeclaration get() = this.parent

val IrDeclarationParent.fqNameSafe: FqName get() = when (this) {
    is IrPackageFragment -> this.fqName
    is IrDeclaration -> this.parent.fqNameSafe.child(this.name)

    else -> error(this)
}

val IrDeclaration.name: Name
    get() = when (this) {
        is IrSimpleFunction -> this.name
        is IrClass -> this.name
        is IrEnumEntry -> this.name
        is IrProperty -> this.name
        is IrLocalDelegatedProperty -> this.name
        is IrField -> this.name
        is IrVariable -> this.name
        is IrConstructor -> SPECIAL_INIT_NAME
        is IrValueParameter -> this.name
        else -> error(this)
    }

private val SPECIAL_INIT_NAME = Name.special("<init>")

val IrField.fqNameSafe: FqName get() = this.parent.fqNameSafe.child(this.name)

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
val IrFunction.allParameters: List<IrValueParameter>
    get() = if (this is IrConstructor) {
        listOf(this.constructedClass.thisReceiver
                ?: error(this.descriptor)
        ) + explicitParameters
    } else {
        explicitParameters
    }

/**
 * @return naturally-ordered list of the parameters that can have values specified at call site.
 */
val IrFunction.explicitParameters: List<IrValueParameter>
    get() {
        val result = ArrayList<IrValueParameter>(valueParameters.size + 2)

        this.dispatchReceiverParameter?.let {
            result.add(it)
        }

        this.extensionReceiverParameter?.let {
            result.add(it)
        }

        result.addAll(valueParameters)

        return result
    }

fun IrFunction.explicitParameters() = this.explicitParameters // FIXME

val IrValueParameter.isVararg get() = this.varargElementType != null

val IrFunction.isSuspend get() = this is IrSimpleFunction && this.isSuspend

fun IrClass.isUnit() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.unit.toSafe()

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
val IrTypeParameter.upperBounds get() = this.superTypes
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.any.toSafe()
fun IrClass.isNothing() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.nothing.toSafe()

fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

val IrProperty.konanBackingField: IrField?
    get() {
        this.backingField?.let { return it }

        (this.descriptor as? DeserializedPropertyDescriptor)?.backingField?.let { backingFieldDescriptor ->
            val result = IrFieldImpl(
                    this.startOffset,
                    this.endOffset,
                    IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                    backingFieldDescriptor,
                    this.getter!!.returnType // FIXME
            ).also {
                it.parent = this.parent
            }
            this.backingField = result
            return result
        }

        return null
    }

val IrClass.defaultType: KotlinType
    get() = this.thisReceiver!!.descriptor.type

val IrField.containingClass get() = this.parent as? IrClass

val IrFunction.isReal get() = this.origin != IrDeclarationOrigin.FAKE_OVERRIDE

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && (parent as? IrClass)?.isFinalClass != true

val IrFunction.isOverridable get() = this is IrSimpleFunction && this.isOverridable

val IrFunction.isOverridableOrOverrides
    get() = this is IrSimpleFunction && (this.isOverridable || this.overriddenSymbols.isNotEmpty())

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

fun IrSimpleFunction.overrides(other: IrSimpleFunction): Boolean {
    if (this == other) return true

    this.overriddenSymbols.forEach {
        if (it.owner.overrides(other)) {
            return true
        }
    }

    return false
}

fun IrClass.isSpecialClassWithNoSupertypes() = this.isAny() || this.isNothing()

val IrClass.companionObject: IrClass? get() = this.declarations.filterIsInstance<IrClass>().atMostOne { it.isCompanion }

internal val IrValueParameter.isValueParameter get() = this.index >= 0

internal val IrFunctionAccessExpression.function get() = this.symbol.owner as IrFunction

fun IrModuleFragment.referenceAllTypeExternalClassifiers(symbolTable: SymbolTable) {
    // Nothing to do anymore!
}

private val IrCall.annotationClass
    get() = (this.symbol.owner as IrConstructor).constructedClass

fun List<IrCall>.hasAnnotation(fqName: FqName): Boolean =
        this.any { it.annotationClass.fqNameSafe == fqName }

fun IrAnnotationContainer.hasAnnotation(fqName: FqName) =
        this.annotations.hasAnnotation(fqName)

fun List<IrCall>.findAnnotation(fqName: FqName): IrCall? = this.firstOrNull {
    it.annotationClass.fqNameSafe == fqName
}

fun IrCall.getStringValue(name: String): String = this.getStringValueOrNull(name)!!

fun IrCall.getStringValueOrNull(name: String): String? {
    val parameter = this.descriptor.valueParameters.single { it.name.asString() == name }
    val constantValue = this.getValueArgument(parameter.index) as IrConst<*>?
    return constantValue?.value as String?
}

fun getStringValue(annotation: IrCall): String? {
    val argument = annotation.getValueArgument(0) ?: return null
    return (argument as IrConst<*>).value as String
}

object KotlinBuiltIns {
    fun isNothing(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES.nothing)

    fun isUnit(type: IrType) = type.isUnit()

    fun isUnitOrNullableUnit(type: IrType) =
            type.isConstructedFromClass(KotlinBuiltIns.FQ_NAMES.unit)

    fun isFloat(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._float)
    fun isDouble(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._double)
    fun isBoolean(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._boolean)
    fun isByte(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._byte)
    fun isShort(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._short)
    fun isInt(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._int)
    fun isLong(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._long)

    fun isChar(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES._char)
    fun isString(type: IrType) = type.isNotNullableConstructedFromClass(KotlinBuiltIns.FQ_NAMES.string)

    fun isArray(type: IrType) = type.isConstructedFromClass(KotlinBuiltIns.FQ_NAMES.array)
    fun isPrimitiveArray(type: IrType) = KotlinBuiltIns.isPrimitiveArray(type.toKotlinType())
    fun getPrimitiveArrayType(classifier: IrClassifierSymbol) = KotlinBuiltIns.getPrimitiveArrayType(classifier.descriptor)

    fun isPrimitiveType(type: IrType): Boolean = KotlinBuiltIns.isPrimitiveType(type.toKotlinType()) // FIXME
    fun isPrimitiveTypeOrNullablePrimitiveType(type: IrType): Boolean =
            KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(type.toKotlinType()) // FIXME
}

object TypeUtils {
    fun getClassDescriptor(type: IrType) = type.getClass()

    fun getTypeParameterDescriptorOrNull(type: IrType): IrTypeParameter? =
            when (type) {
                is IrSimpleType -> type.classifier.owner as? IrTypeParameter
                else -> null
            }

    // FIXME: isn't it already implemented in psi2ir?
    fun isNullableType(type: IrType): Boolean = type.containsNull()
}

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.Variance

class DummyDescriptors(builtIns: IrBuiltIns) {


    val classDescriptor = ErrorUtils.createErrorClass("the descriptor should not be needed")
    val functionDescriptor = builtIns.builtIns.any.unsubstitutedMemberScope.getContributedDescriptors().first { it is FunctionDescriptor } as FunctionDescriptor

    val constructorDescriptor = builtIns.builtIns.any.getConstructors().first()

    val propertyDescriptor = builtIns.builtIns.string.unsubstitutedMemberScope.getContributedDescriptors().first { it is PropertyDescriptor } as PropertyDescriptor


    val variableDescriptor = IrTemporaryVariableDescriptorImpl(
            classDescriptor,
            Name.identifier("the descriptor should not be needed"),
            builtIns.builtIns.unitType)

    val parameterDescriptor = ValueParameterDescriptorImpl(
            functionDescriptor,
            null,
            0,
            Annotations.EMPTY,
            Name.identifier("the descriptor should not be needed"),
            builtIns.builtIns.unitType,
            false,
            false,
            false,
            null,
            SourceElement.NO_SOURCE)

    val typeParameterDescriptor = TypeParameterDescriptorImpl.createWithDefaultBound(
            classDescriptor,
            Annotations.EMPTY,
            false,
            Variance.INVARIANT,
            Name.identifier("the descriptor should not be needed"),
            0)

}
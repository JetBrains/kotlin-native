package org.jetbrains.kotlin.backend.konan.optimizer

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.types.KotlinType

internal fun IrCallWithNewFunction(call: IrCall, newDescriptor: CallableDescriptor): IrCall {

    var newCall =  IrCallImpl(call.startOffset, 
                              call.endOffset, 
                              //call.type, 
                              newDescriptor.returnType!!,
                              // This one is new
                              newDescriptor,
                              // And this one is empty
                              mapOf<TypeParameterDescriptor, KotlinType>(), 
                              call.origin, 
                              call.superQualifier)

    newDescriptor.valueParameters.mapIndexed { i, valueParameterDescriptor ->
        newCall.putValueArgument(i, call.getValueArgument(i))
    }

    return newCall
}


internal fun IrCallWithNewClass(call: IrCall, newClassDescriptor: ClassDescriptor, receiver: ReceiverParameterDescriptor?): IrCall {

    val specializedDescriptor = sameMember(newClassDescriptor, call.descriptor.original)

    assert(specializedDescriptor is FunctionDescriptor)

    val newCalleeDescriptor = specializedDescriptor as FunctionDescriptor

    var newCall =  IrCallImpl(call.startOffset, 
                              call.endOffset, 
                              newCalleeDescriptor.returnType!!,
                              // This one is new
                              newCalleeDescriptor, 
                              // And this one is empty
                              mapOf<TypeParameterDescriptor, KotlinType>(), 
                              call.origin, 
                              call.superQualifier)

    newCalleeDescriptor.valueParameters.mapIndexed { i, valueParameterDescriptor ->
        newCall.putValueArgument(i, call.getValueArgument(i))
    }

    newCall.dispatchReceiver = call.dispatchReceiver
    newCall.extensionReceiver = call.extensionReceiver

    return newCall
}

internal fun IrGetVarWithNewType(value: IrGetValue, newClassDescriptor: ClassDescriptor): IrGetValue {
    val newValueDescriptor = changeType(
                                value.descriptor, 
                                newClassDescriptor.getDefaultType())

    var newGetValue = IrGetValueImpl(
                        value.startOffset,
                        value.endOffset,
                        newValueDescriptor,
                        value.origin)
    return newGetValue

}

internal fun IrSetVarWithNewType(value: IrSetVariable, newClassDescriptor: ClassDescriptor): IrSetVariable {
    val newValueDescriptor = changeType(value.descriptor, newClassDescriptor.getDefaultType())

    var newSetVariable = IrSetVariableImpl(
                            value.startOffset,
                            value.endOffset,
                            // FIXME: remove as
                            newValueDescriptor as VariableDescriptor, 
                            value.value,
                            value.origin)
    return newSetVariable

}

internal fun IrVarWithNewType(value: IrVariable, newClassDescriptor: ClassDescriptor): IrVariable {
    val newValueDescriptor = changeType(value.descriptor, newClassDescriptor.getDefaultType())

    var newVariable = IrVariableImpl(
                        value.startOffset,
                        value.endOffset,
                        value.origin,
                        // FIXME: remove as
                        newValueDescriptor as VariableDescriptor, 
                        value.initializer)
    return newVariable

}


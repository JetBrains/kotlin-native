package org.jetbrains.kotlin.backend.konan.optimizer

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils


val typeRewriter = RewriteMap<ValueDescriptor, KotlinType>()

internal fun changeType(old: ValueDescriptor, type: KotlinType): ValueDescriptor {
    return typeRewriter.getOrPut(
                            old, 
                            type,  
                            {valueDescriptorWithNewType(old, type)}
                        )
}

internal fun valueDescriptorWithNewType(old: ValueDescriptor, type: KotlinType): ValueDescriptor {

    val new = 
        when (old) {
            is IrTemporaryVariableDescriptor 
                -> LocalVariableDescriptor(
                        old.containingDeclaration,
                        old.annotations,
                        old.name, 
                        type,
                        false, // mutable
                        false, // isDelegated
                        old.source)
            is LocalVariableDescriptor 
                -> LocalVariableDescriptor(
                        old.containingDeclaration,
                        old.annotations,
                        old.name,
                        type,
                        old.isVar(),
                        old.isDelegated(),
                        old.source)
            else -> {
                    TODO()
            }
        }


    println("SPECIALIZATION: REWRITTEN VALUE: " + old + " -> " + new)
    return new
}

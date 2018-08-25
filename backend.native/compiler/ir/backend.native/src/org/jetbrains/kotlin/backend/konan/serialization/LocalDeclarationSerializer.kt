/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.Modality.FINAL
import org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE
import org.jetbrains.kotlin.descriptors.Visibilities.INTERNAL
import org.jetbrains.kotlin.descriptors.annotations.Annotations.Companion.EMPTY
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.types.KotlinType

/* 
 * This class knows how to create KonanDescriptorSerializer 
 * invocations to serialize function local declaration descriptors.
 * Those descriptors are not part of the public descriptor tree.
 *
 * We maintain the stack of the serializers because this class
 * provides type serialization facility for all the types in IR.
 * And class serialization is context specific.
 */

internal class LocalDeclarationSerializer(val context: Context, val rootFunctionSerializer: KonanDescriptorSerializer) {

    private val contextStack = mutableListOf<KonanDescriptorSerializer>(rootFunctionSerializer)

    fun pushContext(descriptor: DeclarationDescriptor) {
        val previousContext = contextStack.peek()!!
        val newSerializer = previousContext.createChildSerializer(descriptor)
        contextStack.push(newSerializer)
    }

    fun popContext() {
        contextStack.pop()
    }

    val localSerializer 
        get() = contextStack.peek()!!

    fun typeSerializer(type: KotlinType) = localSerializer.typeId(type)

    fun serializeLocalDeclaration(descriptor: DeclarationDescriptor): KonanIr.DeclarationDescriptor {

        val proto = KonanIr.DeclarationDescriptor.newBuilder()

        context.log{"### serializeLocalDeclaration: $descriptor"}

        when (descriptor) {
            is ClassConstructorDescriptor ->
                proto.setConstructor(localSerializer.constructorProto(descriptor))

            is FunctionDescriptor ->
                proto.setFunction(localSerializer.functionProto(descriptor))

            is PropertyDescriptor ->
                proto.setProperty(localSerializer.propertyProto(descriptor))

            is ClassDescriptor ->
                proto.setClazz(localSerializer.classProto(descriptor))

            is VariableDescriptor -> {
                val property = variableAsProperty(descriptor)
                originalVariables.put(property, descriptor)
                proto.setProperty(localSerializer.propertyProto(property))
            }

            else -> error("Unexpected descriptor kind: $descriptor")
         }

         return proto.build()
     }

    // TODO: We utilize DescriptorSerializer's property 
    // serialization to serialize variables for now.
    // Need to introduce an extension protobuf message
    // and serialize variables directly.
    private fun variableAsProperty(variable: VariableDescriptor): PropertyDescriptor {

        @Suppress("DEPRECATION")
        val isDelegated = when (variable) {
            is LocalVariableDescriptor -> variable.isDelegated
            is IrTemporaryVariableDescriptor -> false
            else -> error("Unexpected variable descriptor.")
        }

        val property = PropertyDescriptorImpl.create(
                variable.containingDeclaration,
                EMPTY,
                FINAL,
                INTERNAL,
                variable.isVar(),
                variable.name,
                DECLARATION,
                NO_SOURCE,
                false, false, false, false, false, 
                isDelegated)

        property.setType(variable.type, listOf(), null, null as KotlinType?)

        // TODO: transform the getter and the setter too.
        property.initialize(null, null)
        return property
    }
}

val originalVariables = mutableMapOf<PropertyDescriptor, VariableDescriptor>()


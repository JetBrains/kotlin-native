/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.*
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.*
import org.jetbrains.kotlin.types.KotlinType

internal class KonanSerializerExtension(val context: Context, val declarationTable: DeclarationTable) :
        KotlinSerializerExtensionBase(KonanSerializerProtocol)/*, IrAwareExtension */ {

    //val inlineDescriptorTable = DescriptorTable(context.irBuiltIns)
    override val stringTable = KonanStringTable()
    override fun shouldUseTypeTable(): Boolean = true

    fun uniqId(descriptor: DeclarationDescriptor): KonanLinkData.DescriptorUniqId {
        if (declarationTable.descriptors[descriptor] == null) {
            println("### no declaration for descriptor: $descriptor")
        }
        val index = declarationTable.descriptors[descriptor] ?: -1
        return newDescriptorUniqId(index)
    }

    override fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {
        // TODO: For debugging purpose we store the textual 
        // representation of serialized types.
        // To be removed for release 1.0.
        proto.setExtension(KonanLinkData.typeText, type.toString())

        super.serializeType(type, proto)
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        proto.setExtension(KonanLinkData.typeParamUniqId, uniqId(typeParameter))
        super.serializeTypeParameter(typeParameter, proto)
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
        proto.setExtension(KonanLinkData.valueParamUniqId, uniqId(descriptor))
        super.serializeValueParameter(descriptor, proto)
    }

    override fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
        proto.setExtension(KonanLinkData.enumEntryUniqId, uniqId(descriptor))
        // Serialization doesn't preserve enum entry order, so we need to serialize ordinal.
        val ordinal = context.specialDeclarationsFactory.getEnumEntryOrdinal(descriptor)
        proto.setExtension(KonanLinkData.enumEntryOrdinal, ordinal)
        super.serializeEnumEntry(descriptor, proto)
    }

    override fun serializeConstructor(descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder) {
        proto.setExtension(KonanLinkData.constructorUniqId, uniqId(descriptor))
        super.serializeConstructor(descriptor, proto)
    }

    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder) {
        proto.setExtension(KonanLinkData.classUniqId, uniqId(descriptor))
        super.serializeClass(descriptor, proto)
    }

    override fun serializeFunction(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder) {
        proto.setExtension(KonanLinkData.functionUniqId, uniqId(descriptor))
        super.serializeFunction(descriptor, proto)
    }

    override fun serializeProperty(descriptor: PropertyDescriptor, proto: ProtoBuf.Property.Builder) {
        val variable = originalVariables[descriptor]
        if (variable != null) {
            proto.setExtension(KonanLinkData.usedAsVariable, true)
        }
        proto.setExtension(KonanLinkData.propertyUniqId, uniqId(descriptor))
        proto.setExtension(KonanLinkData.hasBackingField,
            context.ir.propertiesWithBackingFields.contains(descriptor))

        super.serializeProperty(descriptor, proto)
    }
/*
    override fun addFunctionIR(proto: ProtoBuf.Function.Builder, serializedIR: String) 
        = proto.setInlineIr(inlineBody(serializedIR))

    override fun addConstructorIR(proto: ProtoBuf.Constructor.Builder, serializedIR: String) 
        = proto.setConstructorIr(inlineBody(serializedIR))

    override fun addGetterIR(proto: ProtoBuf.Property.Builder, serializedIR: String) 
        = proto.setGetterIr(inlineBody(serializedIR))

    override fun addSetterIR(proto: ProtoBuf.Property.Builder, serializedIR: String) 
        = proto.setSetterIr(inlineBody(serializedIR))

    override fun serializeInlineBody(descriptor: FunctionDescriptor, serializer: KonanDescriptorSerializer): String {

        return IrSerializer( 
            context, inlineDescriptorTable, stringTable, serializer, descriptor).serializeInlineBody()
    }
    */
}

object KonanSerializerProtocol : SerializerExtensionProtocol(
        ExtensionRegistryLite.newInstance().apply {
           KonanLinkData.registerAllExtensions(this)
        },
        KonanLinkData.packageFqName,
        KonanLinkData.constructorAnnotation,
        KonanLinkData.classAnnotation,
        KonanLinkData.functionAnnotation,
        KonanLinkData.propertyAnnotation,
        KonanLinkData.enumEntryAnnotation,
        KonanLinkData.compileTimeValue,
        KonanLinkData.parameterAnnotation,
        KonanLinkData.typeAnnotation,
        KonanLinkData.typeParameterAnnotation
)
/*
internal interface IrAwareExtension {

    fun serializeInlineBody(descriptor: FunctionDescriptor, serializer: KonanDescriptorSerializer): String 

    fun addFunctionIR(proto: ProtoBuf.Function.Builder, serializedIR: String): ProtoBuf.Function.Builder

    fun addConstructorIR(proto: ProtoBuf.Constructor.Builder, serializedIR: String): ProtoBuf.Constructor.Builder

    fun addSetterIR(proto: ProtoBuf.Property.Builder, serializedIR: String): ProtoBuf.Property.Builder

    fun addGetterIR(proto: ProtoBuf.Property.Builder, serializedIR: String): ProtoBuf.Property.Builder
}
*/

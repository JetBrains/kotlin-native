/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*

fun <T, M : GeneratedMessageLite.ExtendableMessage<M>> M.tryGetExtension(extension: GeneratedMessageLite.GeneratedExtension<M, T>) =
        if (this.hasExtension(extension)) this.getExtension<T>(extension) else null

interface DescriptorUniqIdAware {
    fun getUniqId(descriptor: DeclarationDescriptor): Long?
}

object DeserializedDescriptorUniqIdAware : DescriptorUniqIdAware {
    override fun getUniqId(descriptor: DeclarationDescriptor): Long? = when (descriptor) {
        is DeserializedClassDescriptor -> descriptor.classProto.tryGetExtension(KlibMetadataProtoBuf.classUniqId)
        is DeserializedSimpleFunctionDescriptor -> descriptor.proto.tryGetExtension(KlibMetadataProtoBuf.functionUniqId)
        is DeserializedPropertyDescriptor -> descriptor.proto.tryGetExtension(KlibMetadataProtoBuf.propertyUniqId)
        is DeserializedClassConstructorDescriptor -> descriptor.proto.tryGetExtension(KlibMetadataProtoBuf.constructorUniqId)
        is DeserializedTypeParameterDescriptor -> descriptor.proto.tryGetExtension(KlibMetadataProtoBuf.typeParamUniqId)
        is DeserializedTypeAliasDescriptor -> descriptor.proto.tryGetExtension(KlibMetadataProtoBuf.typeAliasUniqId)
        else -> null
    }?.index
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.needsSerializedIr
import org.jetbrains.kotlin.backend.konan.serialization.IrAwareExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.metadata.serialization.MutableTypeTable
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable

class KonanDescriptorSerializer private constructor(
        private val context: Context,
        containingDeclaration: DeclarationDescriptor?,
        typeParameters: Interner<TypeParameterDescriptor>,
        extension: SerializerExtension,
        typeTable: MutableTypeTable,
        versionRequirementTable: MutableVersionRequirementTable,
        serializeTypeTableToFunction: Boolean
): DescriptorSerializer(
        containingDeclaration = containingDeclaration,
        typeParameters = typeParameters,
        extension = extension,
        typeTable = typeTable,
        versionRequirementTable = versionRequirementTable,
        serializeTypeTableToFunction = serializeTypeTableToFunction) {

    /*
     * Most of *Proto operations require a local child serializer to be created and modified (typeTable).
     * These modification do not appear in parent serializer while it's necessary to have them for the
     * serialization of inline bodies.
     */
    private var cachedChildSerializer = this

    override fun createChildSerializer(descriptor: DeclarationDescriptor): KonanDescriptorSerializer {
        cachedChildSerializer =
                KonanDescriptorSerializer(context, descriptor, Interner(typeParameters), extension, typeTable, versionRequirementTable,
                serializeTypeTableToFunction = false)
        return cachedChildSerializer
    }
    override fun classProto(classDescriptor: ClassDescriptor): ProtoBuf.Class.Builder =
            super.classProto(classDescriptor).also { builder ->
                /* Konan specific chunk */
                context.ir.classesDelegatedBackingFields[classDescriptor]?.forEach {
                    builder.addProperty(propertyProto(it))
                }
                // Invocation of the propertyProto above can add more types
                // to the type table that should also be serialized.
                typeTable.serialize()?.let { builder.mergeTypeTable(it) }
            }

    override fun propertyProto(descriptor: PropertyDescriptor): ProtoBuf.Property.Builder =
            super.propertyProto(descriptor).also { builder ->
                val local = cachedChildSerializer

                /* Konan specific chunk */
                if (extension is IrAwareExtension) {
                    descriptor.getter?.onlyIf({ needsSerializedIr }) {
                        extension.addGetterIR(builder,
                                extension.serializeInlineBody(it, local))
                    }
                    descriptor.setter?.onlyIf({ needsSerializedIr }) {
                        extension.addSetterIR(builder,
                                extension.serializeInlineBody(it, local))
                    }
                }
            }

    override fun functionProto(descriptor: FunctionDescriptor): ProtoBuf.Function.Builder =
            super.functionProto(descriptor).also { builder ->
                /* Konan specific chunk */
                if (extension is IrAwareExtension && descriptor.needsSerializedIr) {
                    extension.addFunctionIR(builder,
                            extension.serializeInlineBody(descriptor, cachedChildSerializer))
                }
            }

    override fun constructorProto(descriptor: ConstructorDescriptor): ProtoBuf.Constructor.Builder =
            super.constructorProto(descriptor).also { builder ->
                /* Konan specific chunk */
                if (extension is IrAwareExtension && descriptor.needsSerializedIr) {
                    extension.addConstructorIR(builder,
                            extension.serializeInlineBody(descriptor, cachedChildSerializer))
                }
            }

    companion object {
        @JvmStatic
        internal fun createTopLevel(context: Context, extension: SerializerExtension): KonanDescriptorSerializer {
            return KonanDescriptorSerializer(context, null, Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                                        serializeTypeTableToFunction = false)
        }

        @JvmStatic
        internal fun create(context: Context, descriptor: ClassDescriptor, extension: SerializerExtension): KonanDescriptorSerializer {
            val container = descriptor.containingDeclaration
            val parentSerializer = if (container is ClassDescriptor)
                create(context, container, extension)
            else
                createTopLevel(context, extension)

            // Calculate type parameter ids for the outer class beforehand, as it would've had happened if we were always
            // serializing outer classes before nested classes.
            // Otherwise our interner can get wrong ids because we may serialize classes in any order.
            val serializer = KonanDescriptorSerializer(
                    context,
                    descriptor,
                    Interner(parentSerializer.typeParameters),
                    parentSerializer.extension,
                    MutableTypeTable(),
                    MutableVersionRequirementTable(),
                    serializeTypeTableToFunction = false
            )
            for (typeParameter in descriptor.declaredTypeParameters) {
                serializer.typeParameters.intern(typeParameter)
            }
            return serializer
        }
    }
}

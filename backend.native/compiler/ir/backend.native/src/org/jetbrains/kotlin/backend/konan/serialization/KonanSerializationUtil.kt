/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer

/*
 * This is Konan specific part of public descriptor 
 * tree serialization and deserialization.
 *
 * It takes care of module and package fragment serializations.
 * The lower level (classes and members) serializations are delegated 
 * to the KonanDescriptorSerializer class.
 * The lower level deserializations are performed by the frontend
 * with MemberDeserializer class.
 */

internal class KonanSerializationUtil(val context: Context, private val metadataVersion: BinaryVersion) {

    lateinit var serializerContext: SerializerContext

    data class SerializerContext(
            val serializerExtension: KonanSerializerExtension,
            val topSerializer: KonanDescriptorSerializer,
            var classSerializer: KonanDescriptorSerializer = topSerializer
    )

    fun createNewContext(): SerializerContext {
        val extension = KonanSerializerExtension(context, metadataVersion)
        return SerializerContext(
                extension,
                KonanDescriptorSerializer.createTopLevel(context, extension)
        )
    }

    inline fun <T> withNewContext(crossinline block: SerializerContext.() -> T): T {
        serializerContext = createNewContext()
        return with(serializerContext, block)
    }

    fun serializeClass(packageName: FqName,
                       builder: KonanProtoBuf.LinkDataClasses.Builder,
                       classDescriptor: ClassDescriptor) {
        with(serializerContext) {
            val previousSerializer = classSerializer

            // TODO: this is to filter out object{}. Change me.
            if (classDescriptor.isExported())
                classSerializer = KonanDescriptorSerializer.create(context, classDescriptor, serializerExtension)

            val classProto = classSerializer.classProto(classDescriptor).build()
                    ?: error("Class not serialized: $classDescriptor")

            builder.addClasses(classProto)
            val index = classSerializer.stringTable.getFqNameIndex(classDescriptor)
            builder.addClassName(index)

            serializeClasses(packageName, builder,
                    classDescriptor.unsubstitutedInnerClassesScope
                            .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))

            classSerializer = previousSerializer
        }
    }

    fun serializeClasses(packageName: FqName,
                         builder: KonanProtoBuf.LinkDataClasses.Builder,
                         descriptors: Collection<DeclarationDescriptor>) {

        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(packageName, builder, descriptor)
            }
        }
    }

    fun serializePackage(fqName: FqName, module: ModuleDescriptor):
            List<KonanProtoBuf.LinkDataPackageFragment> {

        // TODO: ModuleDescriptor should be able to return
        // the package only with the contents of that module, without dependencies

        val fragments = module.getPackage(fqName).fragments.filter { it.module == module }
        if (fragments.isEmpty()) return emptyList()

        val descriptorKindClass =
                DescriptorKindFilter(
                        DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK
                                or DescriptorKindFilter.SINGLETON_CLASSIFIERS_MASK
                )

        val classifierDescriptors = KonanDescriptorSerializer.sort(
                fragments.flatMap {
                    it.getMemberScope().getDescriptorsFiltered(descriptorKindClass)
                }.filter { !it.isExpectMember }
        )

        val descriptorKindTopLevel =
                DescriptorKindFilter(
                        DescriptorKindFilter.CALLABLES_MASK
                                or DescriptorKindFilter.TYPE_ALIASES_MASK
                )
        val topLevelDescriptors = KonanDescriptorSerializer.sort(
                fragments.flatMap { fragment ->
                    fragment.getMemberScope().getDescriptorsFiltered(descriptorKindTopLevel)
                }
                        .filter {
                            !it.isExpectMember
                        }
        )
        val result = mutableListOf<KonanProtoBuf.LinkDataPackageFragment>()

        result += classifierDescriptors.chunked(TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE) { descriptors ->
            withNewContext {
                val classesBuilder = KonanProtoBuf.LinkDataClasses.newBuilder()
                serializeClasses(fqName, classesBuilder, descriptors)
                val classesProto = classesBuilder.build()
                val packageProto = ProtoBuf.Package.newBuilder().build()
                val strings = serializerExtension.stringTable
                val (stringTableProto, nameTableProto) = strings.buildProto()
                serializeClasses(fqName, classesBuilder, descriptors)

                val fragmentBuilder = KonanProtoBuf.LinkDataPackageFragment.newBuilder()
                fragmentBuilder
                        .setPackage(packageProto)
                        .setClasses(classesProto)
                        .setIsEmpty(descriptors.isEmpty())
                        .setFqName(fqName.asString())
                        .setStringTable(stringTableProto)
                        .setNameTable(nameTableProto)
                        .build()
            }
        }

        result += topLevelDescriptors.chunked(TOP_LEVEL_DECLARATION_COUNT_PER_FILE) { descriptors ->
            withNewContext {
                val packageProto = topSerializer.packagePartProto(fqName, descriptors).build()
                        ?: error("Package fragments not serialized: $fragments")

                val strings = serializerExtension.stringTable
                val (stringTableProto, nameTableProto) = strings.buildProto()

                val isEmpty = descriptors.isEmpty()
                val emptyClassesProtoForPackage = KonanProtoBuf.LinkDataClasses.newBuilder().build()
                val fragmentBuilder = KonanProtoBuf.LinkDataPackageFragment.newBuilder()

                fragmentBuilder
                        .setPackage(packageProto)
                        .setClasses(emptyClassesProtoForPackage)
                        .setIsEmpty(isEmpty)
                        .setFqName(fqName.asString())
                        .setStringTable(stringTableProto)
                        .setNameTable(nameTableProto)
                        .build()
            }
        }

        return result
    }

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
        }

        getSubPackages(FqName.ROOT)
        return result
    }

    internal fun serializeModule(moduleDescriptor: ModuleDescriptor): LinkData {
        val libraryProto = KonanProtoBuf.LinkDataLibrary.newBuilder()
        libraryProto.moduleName = moduleDescriptor.name.asString()
        val fragments = mutableListOf<List<ByteArray>>()
        val fragmentNames = mutableListOf<String>()

        getPackagesFqNames(moduleDescriptor).forEach iteration@{ packageFqName ->
            val packageProtos = serializePackage(packageFqName, moduleDescriptor)
            if (packageProtos.isEmpty()) return@iteration

            val packageFqNameStr = packageFqName.asString()
            libraryProto.addPackageFragmentName(packageFqNameStr)
            if (packageProtos.all { it.isEmpty }) {
                libraryProto.addEmptyPackage(packageFqNameStr)
            }
            fragments.add(packageProtos.map { it.toByteArray() })
            fragmentNames.add(packageFqNameStr)
        }
        val libraryAsByteArray = libraryProto.build().toByteArray()
        return LinkData(libraryAsByteArray, fragments, fragmentNames)
    }
}

private const val TOP_LEVEL_DECLARATION_COUNT_PER_FILE = 128
private const val TOP_LEVEL_CLASS_DECLARATION_COUNT_PER_FILE = 64


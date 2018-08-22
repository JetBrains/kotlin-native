package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.storage.StorageManager

class KonanPackageFragment(
        val fqNameString: String,
        val reader: KonanLibrary,
        storageManager: StorageManager,
        module: ModuleDescriptor
) : DeserializedPackageFragment(FqName(fqNameString), storageManager, module) {

    lateinit var components: DeserializationComponents

    override fun initialize(components: DeserializationComponents) {
        this.components = components
    }

    // The proto field is lazy so that we can load only needed
    // packages from the library.
    private val protoForNames: KonanProtoBuf.LinkDataPackageFragment by lazy {
        parsePackageFragment(reader.packageMetadata(fqNameString))
    }

    val proto: KonanProtoBuf.LinkDataPackageFragment
        get() = protoForNames.also { reader.markPackageAccessed(fqNameString) }

    private val nameResolver by lazy {
        NameResolverImpl(protoForNames.stringTable, protoForNames.nameTable)
    }

    override val classDataFinder by lazy {
        KonanClassDataFinder(proto, nameResolver)
    }

    private val memberScope_ by lazy {
        /* TODO: we fake proto binary versioning for now. */
        DeserializedPackageMemberScope(
                this,
                proto.getPackage(),
                nameResolver,
                KonanMetadataVersion.INSTANCE,
                /* containerSource = */ null,
                components) { loadClassNames() }
    }

    override fun getMemberScope(): DeserializedPackageMemberScope = memberScope_

    private val classifierNames: Set<Name> by lazy {
        val result = mutableSetOf<Name>()
        result.addAll(loadClassNames())
        protoForNames.getPackage().typeAliasList.mapTo(result) { nameResolver.getName(it.name) }
        result
    }

    fun hasTopLevelClassifier(name: Name): Boolean = name in classifierNames

    private fun loadClassNames(): Collection<Name> {

        val classNameList = protoForNames.classes.classNameList

        val names = classNameList.mapNotNull {
            val classId = nameResolver.getClassId(it)
            val shortName = classId.shortClassName
            if (!classId.isNestedClass) shortName else null
        }

        return names
    }
}

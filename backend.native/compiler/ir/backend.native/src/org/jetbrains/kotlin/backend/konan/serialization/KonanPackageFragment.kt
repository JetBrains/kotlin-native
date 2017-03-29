package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager

class KonanPackageFragment(fqNameString: String,
    packageLoader: (String)->KonanLinkData.PackageFragment,
    storageManager: StorageManager, module: ModuleDescriptor) : 
    DeserializedPackageFragment(FqName(fqNameString), 
        storageManager, module) {

    // The proto field is lazy so that we can load only needed
    // packages from the library.
    val proto: KonanLinkData.PackageFragment by lazy {
        packageLoader(fqNameString)
    }

    private val nameResolver by lazy {
        NameResolverImpl(proto.getStringTable(), proto.getNameTable())
    }

    override val classDataFinder by lazy {
        KonanClassDataFinder(proto, nameResolver)
    }

    override fun computeMemberScope(): DeserializedPackageMemberScope {
        val packageProto = proto.getPackage()

        return DeserializedPackageMemberScope( this, packageProto, 
            nameResolver, /* containerSource = */ null, 
            components, {loadClassNames()} )
    }

    private fun loadClassNames(): Collection<Name> {

        val classNameList = proto.getClasses().getClassNameList()

        val names = classNameList.mapNotNull { 
            val classId = nameResolver.getClassId(it)
            val shortName = classId.getShortClassName()
            if (!classId.isNestedClass) shortName else null
       }

       return names ?: listOf()
    }
}


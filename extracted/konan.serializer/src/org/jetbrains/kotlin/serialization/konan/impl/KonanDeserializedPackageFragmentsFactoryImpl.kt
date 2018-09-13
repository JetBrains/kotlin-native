package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.exportForwardDeclarations
import org.jetbrains.kotlin.konan.library.isInterop
import org.jetbrains.kotlin.konan.library.packageFqName
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

// FIXME(ddol): decouple and move interop-specific logic back to Kotlin/Native.
internal object KonanDeserializedPackageFragmentsFactoryImpl: KonanDeserializedPackageFragmentsFactory {

    override fun createDeserializedPackageFragments(
            library: KonanLibrary,
            packageFragmentNames: List<String>,
            moduleDescriptor: ModuleDescriptor,
            packageAccessedHandler: PackageAccessedHandler?,
            storageManager: StorageManager
    ) = packageFragmentNames.flatMap {
        val fqName = FqName(it)
        val parts = library.packageMetadataParts(fqName.asString())
        parts.map { partName ->
            KonanPackageFragment(fqName, library, packageAccessedHandler, storageManager, moduleDescriptor, partName)
        }
    }

    override fun createSyntheticPackageFragments(
            library: KonanLibrary,
            deserializedPackageFragments: List<KonanPackageFragment>,
            moduleDescriptor: ModuleDescriptor
    ): List<PackageFragmentDescriptor> {

        if (!library.isInterop) return emptyList()

        val mainPackageFqName = library.packageFqName ?.let { FqName(it) }
                ?: error("Inconsistent manifest: interop library ${library.libraryName} should have `package` specified")
        val exportForwardDeclarations = library.exportForwardDeclarations.map { FqName(it) }

        val aliasedPackageFragments = deserializedPackageFragments.filter { it.fqName == mainPackageFqName }

        val result = mutableListOf<PackageFragmentDescriptor>()
        listOf(ForwardDeclarationsFqNames.cNamesStructs, ForwardDeclarationsFqNames.objCNamesClasses, ForwardDeclarationsFqNames.objCNamesProtocols).mapTo(result) { fqName ->
            ClassifierAliasingPackageFragmentDescriptor(aliasedPackageFragments, moduleDescriptor, fqName)
        }

        result.add(ExportedForwardDeclarationsPackageFragmentDescriptor(moduleDescriptor, mainPackageFqName, exportForwardDeclarations))

        return result
    }
}

/**
 * The package fragment to export forward declarations from interop package namespace, i.e.
 * redirect "$pkg.$name" to e.g. "cnames.structs.$name".
 */
class ExportedForwardDeclarationsPackageFragmentDescriptor(
        module: ModuleDescriptor,
        fqName: FqName,
        declarations: List<FqName>
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val nameToFqName = declarations.map { it.shortName() to it }.toMap()

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            val declFqName = nameToFqName[name] ?: return null

            val packageView = module.getPackage(declFqName.parent())
            return packageView.memberScope.getContributedClassifier(name, location)
        }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("declarations = $declarations")

            p.popIndent()
            p.println("}")
        }

    }

    override fun getMemberScope() = memberScope
}

/**
 * The package fragment that redirects all requests for classifier lookup to its targets.
 */
class ClassifierAliasingPackageFragmentDescriptor(
        targets: List<KonanPackageFragment>,
        module: ModuleDescriptor,
        fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        override fun getContributedClassifier(name: Name, location: LookupLocation) =
                targets.firstNotNullResult {
                    if (it.hasTopLevelClassifier(name)) {
                        it.getMemberScope().getContributedClassifier(name, location)
                    } else {
                        null
                    }
                }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("targets = $targets")

            p.popIndent()
            p.println("}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}

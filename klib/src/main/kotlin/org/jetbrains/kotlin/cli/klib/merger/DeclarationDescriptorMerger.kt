package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.storage.StorageManager

class DeclarationDescriptorMerger(private val storageManager: StorageManager,
                                  private val builtIns: KotlinBuiltIns) {
    var totalSize: Int = 0
    fun merge(modules: List<ModuleWithTargets>): ModuleDescriptorImpl {
        totalSize = modules.map { it.targets.size }.sum()

        return mergeModules(modules)
    }

    private fun mergeModules(modules: List<ModuleWithTargets>): ModuleDescriptorImpl {
        val packagesByName: Map<FqName, List<PackageViewDescriptor>> = modules.flatMap { (module, _) ->
            module.getPackagesFqNames().map { module.getPackage(it) }
                    // TODO think of more appropriate filter
                    .filter { it.fragments.all { fragmentDescriptor -> fragmentDescriptor.module == module || fragmentDescriptor.fqName == FqName.ROOT } }
        }.groupBy { it.fqName }

        val origin = SyntheticModulesOrigin // TODO find out is it ok to use that origins
        val moduleToWrite = ModuleDescriptorImpl(
                modules.first().module.name,
                storageManager,
                builtIns,
                capabilities = mapOf(
                        KonanModuleOrigin.CAPABILITY to origin,
                        ImplicitIntegerCoercion.MODULE_CAPABILITY to false
                ))


        val fragmentDescriptors = mutableListOf<PackageFragmentDescriptor>()
        for ((name, packages) in packagesByName) {
            if (packages.size == totalSize) {
                val newScope = mergeSimplePackages(packages)

                fragmentDescriptors.add(MergerFragmentDescriptor(moduleToWrite, name, newScope))
            }
        }

        // TODO here may be empty package
        val packageFragmentProviderImpl = PackageFragmentProviderImpl(fragmentDescriptors)
        moduleToWrite.initialize(packageFragmentProviderImpl)

        return moduleToWrite
    }

    private fun mergeSimplePackages(packages: List<PackageViewDescriptor>): MemberScope {
        val mergedMembers = listOf(
                DescriptorKindFilter.CLASSIFIERS,
                DescriptorKindFilter.VALUES
        )
                .flatMap { mergeOldMembers(packages, it) }

        return SimpleMemberScope(mergedMembers)
    }


    private fun mergeOldMembers(packageViewDescriptors: List<PackageViewDescriptor>,
                                descriptorKindFilter: DescriptorKindFilter): List<DeclarationDescriptor> {
        fun PackageViewDescriptor.getDescriptors() = memberScope.getDescriptorsFiltered(descriptorKindFilter)

        val equalDescriptorsMap = mutableMapOf<DescriptorHolder, MutableList<DescriptorHolder>>()
        for (viewDescriptor in packageViewDescriptors) {
            val descriptors = viewDescriptor.getDescriptors().map { DescriptorHolder(it) }

            for (desc in descriptors) {
                equalDescriptorsMap.getOrPut(desc, ::mutableListOf)
                        .add(desc)
            }
        }

        return equalDescriptorsMap.values
                .filter { it.size == totalSize }
                .map { it.first().descriptor }
    }
}

package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.storage.StorageManager

fun ModuleDescriptor.getPackagesFqNames(): Set<FqName> {
    val result = mutableSetOf<FqName>()

    fun getSubPackages(fqName: FqName) {
        result.add(fqName)
        getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
    }

    getSubPackages(FqName.ROOT)
    return result
}

class DeclarationDescriptorDiffer(private val storageManager: StorageManager,
                                  private val builtIns: KotlinBuiltIns) {
    var totalSize: Int = 0
    fun diff(modules: List<ModuleWithTargets>): List<ModuleWithTargets> {
        totalSize = modules.map { it.targets.size }.sum()

        return diffModules(modules)
    }


    private fun getPackagesWithTargets(modules: List<ModuleWithTargets>): List<PackageWithTargets> =
            modules.flatMap { (module, targets) ->
                module.getPackagesFqNames().map { module.getPackage(it) }
                        // TODO think of more appropriate filter
                        .filter {
                            it.fragments.all { fragmentDescriptor ->
                                fragmentDescriptor.module == module || fragmentDescriptor.fqName == FqName.ROOT
                            }
                        }
                        .map { PackageWithTargets(it, targets) }
            }

    private fun diffModules(modules: List<ModuleWithTargets>): List<ModuleWithTargets> {
        val packagesByName: Map<FqName, List<PackageWithTargets>> = getPackagesWithTargets(modules)
                .groupBy { it.packageViewDescriptor.fqName }

        val origin = SyntheticModulesOrigin // TODO find out is it ok to use that origins
        val targets = modules.flatMap { it.targets }/*.distinct()*/
        val targetToModules = targets.map {
            it to ModuleDescriptorImpl(
                    modules.first().module.name,
                    storageManager,
                    builtIns,
                    capabilities = mapOf(
                            KonanModuleOrigin.CAPABILITY to origin,
                            ImplicitIntegerCoercion.MODULE_CAPABILITY to false
                    ))
        }.toMap()

        val targetToFragmentDescriptors = mutableMapOf<KonanTarget, MutableList<MergerFragmentDescriptor>>()
        for ((name, packages) in packagesByName) {
            val mismatchedDeclarations = diffPackages(packages)
            val targetToDeclarations = mismatchedDeclarations.groupBy({ it.first }, { it.second })

            for (target in targetToModules.keys) {
                val memberScope = SimpleMemberScope(targetToDeclarations.getOrDefault(target, listOf()))
                targetToFragmentDescriptors.getOrPut(target, ::mutableListOf)
                        .add(MergerFragmentDescriptor(targetToModules[target]!!, name, memberScope))
            }
        }


        for ((target, fragmentDesc) in targetToFragmentDescriptors) {
            val packageFragmentProviderImpl = PackageFragmentProviderImpl(fragmentDesc)
            targetToModules[target]!!.initialize(packageFragmentProviderImpl)
        }

        // TODO here may be empty package
        return targetToModules.map {
            ModuleWithTargets(it.value, listOf(it.key))
        }
    }

    private fun diffPackages(packages: List<PackageWithTargets>): List<Pair<KonanTarget, DeclarationDescriptor>> =
            listOf(
                    DescriptorKindFilter.CLASSIFIERS,
                    DescriptorKindFilter.VALUES
            )
                    .flatMap { mismatchedMembers(packages, it) }

    private fun mismatchedMembers(packageViewDescriptors: List<PackageWithTargets>,
                                  descriptorKindFilter: DescriptorKindFilter): List<Pair<KonanTarget, DeclarationDescriptor>> {
        val equalDescriptorsMap = mutableMapOf<DescriptorHolder, MutableList<KonanTarget>>()
        for ((viewDescriptor, targets) in packageViewDescriptors) {
            val descriptorHolders = viewDescriptor.memberScope
                    .getDescriptorsFiltered(descriptorKindFilter)
                    .map { DescriptorHolder(it) }

            for (descriptorHolder in descriptorHolders) {
                equalDescriptorsMap.getOrPut(descriptorHolder, ::mutableListOf)
                        .addAll(targets)
            }
        }

        return equalDescriptorsMap
                .filter { (_, targets) -> targets.size < totalSize }
                .flatMap { (desc, targets) -> targets.map { it to desc.descriptor } }
    }
}

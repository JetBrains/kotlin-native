package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.storage.StorageManager


interface DescriptorMerger {
    fun mergeModules(modules: List<ModuleDescriptorImpl>): ModuleDescriptorImpl
}

class DummyDescriptorMerger(private val storageManager: StorageManager,
                            private val builtIns: KotlinBuiltIns) : DescriptorMerger {
    private fun ModuleDescriptor.getPackagesFqNames(): Set<FqName> {
        val result = mutableSetOf<FqName>()

        fun getSubPackages(fqName: FqName) {
            result.add(fqName)
            getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
        }

        getSubPackages(FqName.ROOT)
        return result
    }

    override fun mergeModules(modules: List<ModuleDescriptorImpl>): ModuleDescriptorImpl {
        val packagesByName: Map<FqName, List<PackageViewDescriptor>> = modules.flatMap { module ->
            module.getPackagesFqNames().map { module.getPackage(it) }
                    // TODO think of more appropriate filter
                    .filter { it.fragments.all { it.module == module || it.fqName == FqName.ROOT } }
        }.groupBy { it.fqName }


        val origin = SyntheticModulesOrigin // TODO find out is it ok to use that origins
        val moduleToWrite = ModuleDescriptorImpl(
                modules.first().name,
                storageManager,
                builtIns,
                capabilities = mapOf(
                        KonanModuleOrigin.CAPABILITY to origin,
                        ImplicitIntegerCoercion.MODULE_CAPABILITY to false
                ))


        val countOfModules = modules.size
        val fragmentDescriptors = mutableListOf<PackageFragmentDescriptor>()
        for ((name, packages) in packagesByName) {

            if (packages.size == countOfModules) {
                val newScope = mergePackages(packages)
                fragmentDescriptors.add(MergerFragmentDescriptor(moduleToWrite, name, newScope))
            }
        }


        val packageFragmentProviderImpl = PackageFragmentProviderImpl(fragmentDescriptors)
        moduleToWrite.initialize(packageFragmentProviderImpl)

        return moduleToWrite
    }

    private fun mergePackages(packages: List<PackageViewDescriptor>): MemberScope {
        val whatToMerge = listOf(
                DescriptorKindFilter.CLASSIFIERS,
                DescriptorKindFilter.VALUES
        )

        val mergedMembers = whatToMerge.flatMap { mergeMembers(packages, it) }

        return SimpleMemberScope(mergedMembers)
    }

    private fun mergeMembers(packageViewDescriptors: List<PackageViewDescriptor>,
                             descriptorKindFilter: DescriptorKindFilter): List<DeclarationDescriptor> {

        fun PackageViewDescriptor.getDescriptors() = memberScope.getDescriptorsFiltered(descriptorKindFilter)

        val nameToDescriptor = mutableMapOf<Name, MutableList<DeclarationDescriptor>>()
        for (viewDescriptor in packageViewDescriptors) {
            val descriptors = viewDescriptor.getDescriptors()
            descriptors.forEach { it ->
                if (it.name in nameToDescriptor) {
                    nameToDescriptor[it.name]!!.add(it)
                } else {
                    nameToDescriptor[it.name] = mutableListOf(it)
                }
            }
        }

        val totalValue = packageViewDescriptors.size

        return nameToDescriptor.map { (_, list) -> list }
                .filter { it.size == totalValue }
                .map { it.map { DescriptorHolder(it) }.toSet() }
                .filter { it.size == 1 }
                .map { it.first().descriptor }
    }


}

class DescriptorHolder(val descriptor: DeclarationDescriptor) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false


        other as DescriptorHolder

        if (descriptor === other.descriptor) return true
//        if (descriptor.javaClass != other.descriptor.javaClass) return false

        return when {
            descriptor is FunctionDescriptor && other.descriptor is FunctionDescriptor ->
                compareFunctionDescriptors(descriptor, other.descriptor)

            descriptor is ValueDescriptor && other.descriptor is ValueDescriptor ->
                compareValueDescriptors(descriptor, other.descriptor)

            else -> false
        }
    }

    private fun compareParams(o1: List<ParameterDescriptor>, o2: List<ParameterDescriptor>): Boolean {
        return o1.size == o2.size &&
                (o1 zip o2).all { (a, b) -> compareValueParameterDescriptors(a, b) }
    }

    private fun compareFunctionDescriptors(o1: FunctionDescriptor, o2: FunctionDescriptor): Boolean {
        return o1.name == o2.name
                && (o1.isSuspend == o2.isSuspend)
                && (o1.returnType == o2.returnType)
                && (compareParams(o1.explicitParameters, o2.explicitParameters))

    }

    private fun compareValueDescriptors(o1: ValueDescriptor, o2: ValueDescriptor): Boolean = when {
        o1 is VariableDescriptor && o2 is VariableDescriptor -> compareVariableDescriptors(o1, o2)
        o1 is ValueParameterDescriptor && o2 is ValueParameterDescriptor -> compareValueParameterDescriptors(o1, o2)
        else -> false
    }


    private fun compareVariableDescriptors(o1: VariableDescriptor, o2: VariableDescriptor): Boolean =
            o1.name == o2.name
                    && (o1.isConst == o2.isConst)
                    && (o1.isVar == o2.isVar)
                    && (o1.isLateInit == o2.isLateInit)
                    && (o1.isSuspend == o2.isSuspend)
                    && o1.type == o2.type
                    && o1.returnType == o2.returnType

    private fun compareValueParameterDescriptors(o1: ParameterDescriptor, o2: ParameterDescriptor): Boolean {
        return o1.name == o2.name
                && (o1.returnType == o2.returnType)
                && (o1.type == o2.type)
                && o1.isSuspend == o2.isSuspend
                && (Visibilities.compare(o1.visibility, o2.visibility) == 0)
    }

    override fun hashCode(): Int {
        return descriptor.name.hashCode()
    }
}

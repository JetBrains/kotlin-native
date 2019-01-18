package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class MergerFragmentDescriptor(moduleDescriptor: ModuleDescriptor,
                               fqName: FqName,
                               private val memberScope: MemberScope) : PackageFragmentDescriptorImpl(moduleDescriptor, fqName) {
    override fun getMemberScope(): MemberScope {
        return memberScope
    }
}


data class ModuleWithTargets(
        val module: ModuleDescriptorImpl,
        val targets: List<KonanTarget>
)

data class PackageWithTargets(
        val packageViewDescriptor: PackageViewDescriptor,
        val targets: List<KonanTarget>
)

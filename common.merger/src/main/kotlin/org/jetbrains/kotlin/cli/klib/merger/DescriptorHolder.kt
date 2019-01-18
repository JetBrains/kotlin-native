package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class DescriptorHolder(val descriptor: DeclarationDescriptor) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DescriptorHolder

        if (descriptor === other.descriptor) return true

        return when {
            descriptor is FunctionDescriptor && other.descriptor is FunctionDescriptor ->
                compareFunctionDescriptors(descriptor, other.descriptor)

            descriptor is ValueDescriptor && other.descriptor is ValueDescriptor ->
                compareValueDescriptors(descriptor, other.descriptor)

            descriptor is ClassDescriptor && other.descriptor is ClassDescriptor ->
                compareClassDescriptors(descriptor, other.descriptor)

            descriptor is TypeAliasDescriptor && other.descriptor is TypeAliasDescriptor ->
                compareTypeAliasDescriptors(descriptor, other.descriptor)

            else -> false
        }
    }

    private fun Collection<DeclarationDescriptor>.wrap(): Set<DescriptorHolder> = map { DescriptorHolder(it) }.toSet()

    private fun compareTypeAliasDescriptors(o1: TypeAliasDescriptor, o2: TypeAliasDescriptor): Boolean =
            o1.name == o2.name
                    && o1.isActual == o2.isActual
                    && o1.isExpect == o2.isExpect
                    && o1.isExternal == o2.isExternal
                    && o1.expandedType == o2.expandedType
//                    && o1.underlyingType == o2.underlyingType

    private fun compareClassDescriptors(o1: ClassDescriptor, o2: ClassDescriptor): Boolean {
        val firstMemberScope = o1.unsubstitutedMemberScope
        val secondMemberScope = o2.unsubstitutedMemberScope

        fun compareDescriptors(kind: DescriptorKindFilter) =
                firstMemberScope.getDescriptorsFiltered(kind).wrap() ==
                        secondMemberScope.getDescriptorsFiltered(kind).wrap()

        return o1.name == o2.name
                && o1.kind == o2.kind
                && o1.constructors.wrap() == o2.constructors.wrap()
                && compareDescriptors(DescriptorKindFilter.FUNCTIONS)
                && compareDescriptors(DescriptorKindFilter.VARIABLES)
                && compareDescriptors(DescriptorKindFilter.CLASSIFIERS)
//                && compareDescriptors(DescriptorKindFilter.TYPE_ALIASES)
    }

    private fun compareParams(o1: List<ParameterDescriptor>, o2: List<ParameterDescriptor>): Boolean =
            o1.size == o2.size
                    && (o1 zip o2).all { (a, b) -> compareValueParameterDescriptors(a, b) }

    private fun compareFunctionDescriptors(o1: FunctionDescriptor, o2: FunctionDescriptor): Boolean =
            o1.name == o2.name
                    && o1.isSuspend == o2.isSuspend
                    && o1.returnType == o2.returnType
                    && compareParams(o1.explicitParameters, o2.explicitParameters)

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

    private fun compareValueParameterDescriptors(o1: ParameterDescriptor, o2: ParameterDescriptor): Boolean =
            (o1.name == o2.name
                    && (o1.returnType == o2.returnType)
                    && (o1.type == o2.type)
                    && o1.isSuspend == o2.isSuspend
                    && (Visibilities.compare(o1.visibility, o2.visibility) == 0))

    override fun hashCode(): Int {
        return descriptor.name.hashCode()
    }
}

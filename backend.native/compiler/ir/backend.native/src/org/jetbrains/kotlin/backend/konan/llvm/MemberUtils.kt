package org.jetbrains.kotlin.backend.konan.optimizer

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.backend.konan.llvm.functionName

internal fun allMembers(clazz: ClassDescriptor): List<DeclarationDescriptor> {
    val members = clazz.unsubstitutedMemberScope.getContributedDescriptors()
    val primaryConstructor = clazz.getUnsubstitutedPrimaryConstructor()
    val constructors = clazz.getConstructors()
    val getters = members.filterIsInstance<PropertyDescriptor>().map{it -> it.getter }
    val setters = members.filterIsInstance<PropertyDescriptor>().map{it -> it.setter }
    val allCombined =  members+listOf(primaryConstructor)+constructors+getters+setters
    return allCombined.filterNotNull()
}

internal fun memberCanonicalName(descriptor: DeclarationDescriptor): String {
    return when (descriptor) {
         is FunctionDescriptor -> descriptor.name.asString()
         is PropertyDescriptor -> descriptor.name.asString()
         else -> TODO(descriptor.toString())
        }
}


internal fun sameMember(newClassDescriptor: ClassDescriptor, 
                   calleeDescriptor: DeclarationDescriptor): DeclarationDescriptor {
    val oldClassDescriptor = calleeDescriptor.getContainingDeclaration()
    if (oldClassDescriptor !is ClassDescriptor) {
        TODO()
    }
    val oldMembers = allMembers(oldClassDescriptor)
    val newMembers =  allMembers(newClassDescriptor)

    newMembers.forEach {
        if (memberCanonicalName(it) == memberCanonicalName(calleeDescriptor)) {
            println("SPECIALIZATION: call rewrite: " + calleeDescriptor + " -> " + it)
            return it 
        }
    }

    throw Error("Could not find matching member")

}


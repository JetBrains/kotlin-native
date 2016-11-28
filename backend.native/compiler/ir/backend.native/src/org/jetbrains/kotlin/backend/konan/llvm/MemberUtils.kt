package org.jetbrains.kotlin.backend.konan.optimizer

import org.jetbrains.kotlin.descriptors.*

internal fun allMembers(clazz: ClassDescriptor): List<DeclarationDescriptor> {
    val members = clazz.unsubstitutedMemberScope.getContributedDescriptors()
    val primaryConstructor = clazz.getUnsubstitutedPrimaryConstructor()
    val constructors = clazz.getConstructors()
    val getters = members.filterIsInstance<PropertyDescriptor>().map{it -> it.getter }
    val setters = members.filterIsInstance<PropertyDescriptor>().map{it -> it.setter }
    val allCombined =  members+listOf(primaryConstructor)+constructors+getters+setters
    return allCombined.filterNotNull()
}


internal fun sameMember(newClassDescriptor: ClassDescriptor, 
                   calleeDescriptor: DeclarationDescriptor): DeclarationDescriptor {
    val newMembers =  allMembers(newClassDescriptor)
    //FIXME remove as
    val oldMembers = allMembers(calleeDescriptor.getContainingDeclaration() as ClassDescriptor)

    //if (oldMembers.size != newMembers.size) {
        oldMembers.forEach{ 
            println(" From: "+ it)
        }
        newMembers.forEach{
            println("   To: "+it)
        }
     //   throw Error("Generic and specializer members differ")
   // }


    oldMembers.zip(newMembers).forEach { (old, new) -> 
        println("COMPARING " + old.original + " WITH " + calleeDescriptor)
        if (old.original == calleeDescriptor) {
            println("REWRITE CALL: " + old + " -> " + new)
            return new
        }
    } 

    throw Error("Could not find matching member")

}


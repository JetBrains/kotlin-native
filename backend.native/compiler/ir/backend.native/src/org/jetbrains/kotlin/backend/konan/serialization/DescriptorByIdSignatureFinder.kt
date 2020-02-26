package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature

// The code here is intentionally copy-pasted from DeclarationStubGenerator with
// minor changes.
// "Find descriptor by IdSignature" task appears to be common and should be unified later.
class DescriptorByIdSignatureFinder(
        private val moduleDescriptor: ModuleDescriptor
) {
    fun findDescriptorBySignature(signature: IdSignature): DeclarationDescriptor? = when (signature) {
        is IdSignature.AccessorSignature -> findDescriptorForAccessorSignature(signature)
        is IdSignature.PublicSignature -> findDescriptorForPublicSignature(signature)
        else -> error("only PublicSignature or AccessorSignature should reach this point, got $signature")
    }

    private fun findDescriptorForAccessorSignature(signature: IdSignature.AccessorSignature): DeclarationDescriptor? {
        val propertyDescriptor = findDescriptorBySignature(signature.propertySignature) as? PropertyDescriptor
                ?: return null
        return propertyDescriptor.accessors.singleOrNull {
            it.name == signature.accessorSignature.declarationFqn.shortName()
        }
    }

    private fun findDescriptorForPublicSignature(signature: IdSignature.PublicSignature): DeclarationDescriptor? {
        val packageDescriptor = moduleDescriptor.getPackage(signature.packageFqName())
        val pathSegments = signature.declarationFqn.pathSegments()
        val toplevelDescriptors = packageDescriptor.memberScope.getContributedDescriptors { name -> name == pathSegments.first() }
                .filter { it.name == pathSegments.first() }
        val candidates = pathSegments.drop(1).fold(toplevelDescriptors) { acc, current ->
            acc.flatMap { container ->
                val classDescriptor = container as? ClassDescriptor
                        ?: return@flatMap emptyList<DeclarationDescriptor>()
                val nextStepCandidates = classDescriptor.constructors +
                        classDescriptor.unsubstitutedMemberScope.getContributedDescriptors { name -> name == current } +
                        // Static scope is required only for Enum.values() and Enum.valueOf().
                        classDescriptor.staticScope.getContributedDescriptors { name -> name == current }
                nextStepCandidates.filter { it.name == current }
            }
        }
        return when (candidates.size) {
            1 -> candidates.first()
            else ->candidates.firstOrNull { signature.id == DeserializedDescriptorUniqIdAware.getUniqId(it) }
        }
    }
}
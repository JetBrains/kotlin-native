/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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
            else -> {
                val id = signature.id
                        ?: error("$signature has no id, but there are multiple candidates: ${candidates.joinToString { it.fqNameSafe.toString() }}")
                findDescriptorByHash(candidates, id)
                        ?: error("No descriptor found for $signature")
            }
        }
    }

    private fun findDescriptorByHash(candidates: List<DeclarationDescriptor>, hash: Long): DeclarationDescriptor? {
        candidates.map { candidate ->
            if (candidate is CallableMemberDescriptor && candidate.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                if (hashIsAmongRealDescriptors(candidate, hash)) return candidate
            } else {
                val candidateHash = with (KonanManglerDesc) { candidate.signatureMangle }
                if (candidateHash == hash) return candidate
            }
        }
        return null
    }

    private fun hashIsAmongRealDescriptors(
            fakeOverrideMemberDescriptor: CallableMemberDescriptor,
            hash: Long
    ): Boolean {
        val overriddenRealMembers = fakeOverrideMemberDescriptor.resolveFakeOverrideMaybeAbstract()
        return overriddenRealMembers.any { realDescriptor ->
            val mangle = with (KonanManglerDesc) { realDescriptor.signatureMangle }
            mangle == hash
        }
    }
}
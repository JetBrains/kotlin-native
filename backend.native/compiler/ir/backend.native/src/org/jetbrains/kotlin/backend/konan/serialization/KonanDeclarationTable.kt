package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.DescToIrIdSignatureComputer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class KonanGlobalDeclarationTable(signatureSerializer: IdSignatureSerializer, builtIns: IrBuiltIns) : GlobalDeclarationTable(signatureSerializer, KonanManglerIr) {
    init {
        loadKnownBuiltins(builtIns)
    }
}

class KonanDeclarationTable(
        globalDeclarationTable: GlobalDeclarationTable
) : DeclarationTable(globalDeclarationTable) {

    private val signatureIdComposer = DescToIrIdSignatureComputer(KonanIdSignaturer(KonanManglerDesc))

    override fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? =
            if (declaration.descriptor.module.isFromInteropLibrary()) {
                signatureIdComposer.computeSignature(declaration)
            } else null
}
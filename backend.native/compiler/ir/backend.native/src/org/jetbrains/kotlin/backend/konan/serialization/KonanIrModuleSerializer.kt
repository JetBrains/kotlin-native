package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.konan.llvm.KonanManglerIr
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol

private class KonanDeclarationTable(globalDeclarationTable: GlobalDeclarationTable) :
        DeclarationTable(globalDeclarationTable) {

    /**
     * It is incorrect to compute UniqId for declarations from metadata-based libraries.
     * Instead we should get precomputed value from metadata.
     */
//    override fun tryComputeBackendSpecificUniqId(declaration: IrDeclaration): UniqId? {
//        return if (declaration.descriptor.module.isFromInteropLibrary()) {
//            // Property accessor doesn't provide UniqId so we need to get it from the property itself.
//            UniqId(declaration.descriptor.propertyIfAccessor.getUniqId() ?: error("No uniq id found for ${declaration.descriptor}"))
//        } else {
//            null
//        }
//    }
}

class KonanIrModuleSerializer(
    logger: LoggingContext,
    irBuiltIns: IrBuiltIns,
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    val skipExpects: Boolean
) : IrModuleSerializer<KonanIrFileSerializer>(logger) {

    private val signaturer = IdSignatureSerializer(KonanManglerIr)
    private val globalDeclarationTable = KonanGlobalDeclarationTable(signaturer, irBuiltIns)

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer =
            KonanIrFileSerializer(logger, KonanDeclarationTable(globalDeclarationTable), expectDescriptorToSymbol, skipExpects = skipExpects)
}

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.descriptors.propertyIfAccessor
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.UniqId
import org.jetbrains.kotlin.ir.util.isAccessor
import org.jetbrains.kotlin.resolve.descriptorUtil.module

private class KonanDeclarationTable(
        descriptorTable: DescriptorTable,
        globalDeclarationTable: GlobalDeclarationTable,
        startIndex: Long
) : DeclarationTable(descriptorTable, globalDeclarationTable, startIndex),
        DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware {

    /**
     * It is incorrect to compute UniqId for declarations from metadata-based libraries.
     * Instead, we should get precomputed value from metadata.
     */
    override fun tryComputeBackendSpecificUniqId(declaration: IrDeclaration): UniqId? =
            if (shouldExtractInteropUniqId(declaration)) extractUniqId(declaration) else null

    private fun extractUniqId(declaration: IrDeclaration): UniqId {
        val index = declaration.descriptor.getUniqId()
                ?: error("No uniq id found for ${declaration.descriptor}")
        return UniqId(index)
    }

    private fun shouldExtractInteropUniqId(declaration: IrDeclaration): Boolean =
            declaration.descriptor.module.isFromInteropLibrary()
                    && !declaration.isLocalDeclaration()
                    // Delegate UniqId computation to default mangler since
                    // accessors presented only in IR.
                    && !declaration.isAccessor

    // Shameless copy-paste from `DeclarationTable`.
    private fun IrDeclaration.isLocalDeclaration(): Boolean {
        return origin == IrDeclarationOrigin.FAKE_OVERRIDE
                || !isExportedDeclaration(this)
                || this is IrValueDeclaration
                || this is IrAnonymousInitializer
                || this is IrLocalDelegatedProperty
    }
}

class KonanIrModuleSerializer(
    logger: LoggingContext,
    irBuiltIns: IrBuiltIns,
    private val descriptorTable: DescriptorTable,
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    val skipExpects: Boolean
) : IrModuleSerializer<KonanIrFileSerializer>(logger) {

    private val globalDeclarationTable = KonanGlobalDeclarationTable(irBuiltIns)

    // We skip files that contain generated IR from interop libraries.
    override fun backendSpecificFileFilter(file: IrFile): Boolean =
        !file.packageFragmentDescriptor.module.isFromInteropLibrary()

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer =
            KonanIrFileSerializer(logger, KonanDeclarationTable(descriptorTable, globalDeclarationTable, 0), expectDescriptorToSymbol, skipExpects = skipExpects)
}

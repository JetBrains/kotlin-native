package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.llvm.*

import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class KonanGlobalDeclarationTable(builtIns: IrBuiltIns) : GlobalDeclarationTable(TODO("Signaturer"), TODO("KonanIrMangler")) {
    init {
        loadKnownBuiltins(builtIns)
    }
}
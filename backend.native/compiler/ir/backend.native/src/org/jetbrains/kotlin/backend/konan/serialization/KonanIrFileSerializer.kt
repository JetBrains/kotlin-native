package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.llvm.neverRetainFor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.konan.target.KonanTarget

class KonanIrFileSerializer(
        val target: KonanTarget,
        logger: LoggingContext,
        declarationTable: DeclarationTable,
        bodiesOnlyForInlines: Boolean = false
): IrFileSerializer(logger, declarationTable, bodiesOnlyForInlines) {

    override fun backendSpecificExplicitRoot(declaration: IrFunction) =
            if (declaration.neverRetainFor(target)) false else
                declaration.annotations.hasAnnotation(RuntimeNames.exportForCppRuntime)

    override fun backendSpecificExplicitRoot(declaration: IrClass) =
            declaration.annotations.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation)
}
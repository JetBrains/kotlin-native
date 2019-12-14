package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.IrBoxCounterField
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class CreateBoxingCounterLowering(val context: Context): FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(AutoboxCreator(context))
    }
}

internal class AutoboxCreator(val context: Context) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        val runCountFunction = declaration.findDeclaration<IrFunction> { it.name.toString() == "runCount" } ?: return
        val counterField = IrBoxCounterField.get(context)
        counterField.tryAddTo(declaration)
        wrap(runCountFunction)
    }

    private fun wrap(runCountFunction: IrFunction) {
        runCountFunction.returnType = context.irBuiltIns.intType
        val builder = context.createIrBuilder(runCountFunction.symbol)
        runCountFunction.body = builder.irBlockBody(runCountFunction) {
            +IrTryImpl(
                    runCountFunction.startOffset,
                    runCountFunction.endOffset,
                    type = context.irBuiltIns.nothingType
            ).apply {
                tryResult = irBlock {
                    runCountFunction.body?.statements?.forEach { +it }
                }
                finallyExpression = irReturn(irGetField(null, IrBoxCounterField.get(this@AutoboxCreator.context).field))
            }
        }
    }
}
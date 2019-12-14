package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.IrBoxCounterField
import org.jetbrains.kotlin.backend.konan.boxing.irInc
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class CountBoxingsLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(BoxingsCounterVisitor(context))
    }
}

internal class BoxingsCounterVisitor(val context: Context) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        declaration.acceptChildrenVoid(this)

        val boxFunctions = declaration.declarations.filterIsInstance<IrFunction>().filter { it.nameForIrSerialization.asString().endsWith("-box>") }
        boxFunctions.forEach { function ->
            val builder = context.createIrBuilder(function.symbol)
            val statements = function.body?.statements
            function.body = builder.irBlockBody(function) {
                +irInc(IrBoxCounterField.get(this@BoxingsCounterVisitor.context))
                statements?.forEach { +it }
            }
        }
    }
}
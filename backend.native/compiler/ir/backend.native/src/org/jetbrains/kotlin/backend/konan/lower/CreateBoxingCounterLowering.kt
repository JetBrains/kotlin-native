package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.IrBoxCounterField
import org.jetbrains.kotlin.backend.konan.boxing.irNullize
import org.jetbrains.kotlin.backend.konan.boxing.irPrintln
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName

internal class CreateBoxingCounterLowering(val context: Context): FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val counter = IrBoxCounterField.get(context)
        counter.tryAddTo(irFile)
        val autoboxCreator = AutoboxCreator(context)
        irFile.acceptVoid(autoboxCreator)
    }
}

internal class AutoboxCreator(val context: Context) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        visitDeclarationContainer(declaration)
        super.visitClass(declaration)
    }

    override fun visitFile(declaration: IrFile) {
        visitDeclarationContainer(declaration)
        super.visitFile(declaration)
    }

    private fun visitDeclarationContainer(declarationContainer: IrDeclarationContainer) {
        val annotatedDeclarations = declarationContainer.declarations.filterIsInstance<IrFunction>().filter {
            it.hasAnnotation(FqName.fromSegments(listOf("org", "jetbrains", "ring", "CountBoxings")))
        }
        annotatedDeclarations.forEach {
            wrap(it)
        }
    }

    private fun wrap(runCountFunction: IrFunction) {
        val counter = IrBoxCounterField.get(context)
        val builder = context.createIrBuilder(runCountFunction.symbol)
        runCountFunction.body = builder.irBlockBody(runCountFunction) {
            +irNullize(counter)
            +IrTryImpl(
                    runCountFunction.startOffset,
                    runCountFunction.endOffset,
                    type = context.irBuiltIns.nothingType
            ).apply {
                tryResult = irBlock {
                    runCountFunction.body?.statements?.forEach { +it }
                }
                finallyExpression = irPrintln(counter)
            }
        }
    }
}
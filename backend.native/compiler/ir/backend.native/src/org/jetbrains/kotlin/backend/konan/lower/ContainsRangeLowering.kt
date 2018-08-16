package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.jvm.intrinsics.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.psi2ir.intermediate.*

internal class ContainsRangeLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = ContainsRangeTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

private class ContainsRangeTransformer(val context: Context) : IrElementTransformerVoidWithContext() {
    private val symbols = context.ir.symbols
    private val scopeOwnerSymbol
        get() = currentScope!!.scope.scopeOwnerSymbol

    val primClasses = setOf("Byte", "Char", "Short", "Int", "Long")
    val rangeClasses = primClasses.map { "${it}Range" }.toSet()

    override fun visitCall(expression: IrCall): IrExpression {
        val expressionDescriptor = expression.descriptor

        if (
            expressionDescriptor.inPackage("kotlin.ranges") &&
            expressionDescriptor.inClassName(rangeClasses) &&
            expressionDescriptor.isIdentifier("contains") &&
            expression.valueArgumentsCount == 1
        ) {
            val args = expression.receiverAndArgs()
            val builder = context.createIrBuilder(scopeOwnerSymbol, expression.startOffset, expression.endOffset)

            val range = args.first()
            val item = args.last()

            with(builder) {
                val booleanType = context.irBuiltIns.booleanType

                // Temp: Needed to avoid evaluating the expression twice
                // var a = 0
                // a++ in (0 until 10)
                // println(a) // a = 1

                if (range is IrCall) {
                    val rangeDescriptor = range.descriptor

                    val rangeArgs = range.receiverAndArgs()
                    if (rangeArgs.size == 2) {
                        val low = rangeArgs.first()
                        val high = rangeArgs.last()

                        when {
                            rangeDescriptor.isIdentifier("rangeTo") &&
                                    rangeDescriptor.inPackage("kotlin") &&
                                    rangeDescriptor.inClassName(primClasses)
                            -> {
                                return createTempBlock(booleanType, item) {
                                    irAnd(
                                        irGreaterEqual(it.load(), low),
                                        irLessEqual(it.load(), high)
                                    )
                                }

                            }
                            rangeDescriptor.isTopLevelFunction("kotlin.ranges", "until") -> {
                                return createTempBlock(booleanType, item) {
                                    irAnd(
                                        irGreaterEqual(it.load(), low),
                                        irLessThan(it.load(), high)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.visitCall(expression)
    }

    fun IrBuilderWithScope.createTempBlock(blockType: IrType, tempExpr: IrExpression, callback: IrBlock.(temp: IntermediateValue) -> IrExpression): IrBlock {
        val irBlock =
            IrBlockImpl(startOffset, endOffset, blockType, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL)
        val temp = scope.createTemporaryVariableInBlock(context, tempExpr, irBlock, "temp")
        irBlock.addIfNotNull(callback(irBlock, temp))
        return irBlock
    }

    // Tools
    private fun Named.isIdentifier(id: String) = !name.isSpecial && this.name.identifier == id

    val FunctionDescriptor.functionName: String? get() = if (!name.isSpecial) this.name.identifier else null
    val FunctionDescriptor.declPackageName: String? get() =
        ((containingDeclaration as? PackageFragmentDescriptor) ?: (containingDeclaration.containingDeclaration as? PackageFragmentDescriptor))?.fqName?.asString()

    val FunctionDescriptor.declClassName: String? get() = (containingDeclaration as? ClassDescriptor)?.name?.asString()
    fun FunctionDescriptor.inPackage(_package: String): Boolean = declPackageName == _package
    fun FunctionDescriptor.inClassName(name: String): Boolean = declClassName == name
    fun FunctionDescriptor.inClassName(names: Set<String>): Boolean = declClassName in names

    fun FunctionDescriptor.isClassMethod(_package: String, clazz: String, name: String): Boolean =
        this.isIdentifier(name) && this.containingDeclaration.isTopLevelInPackage(clazz, _package)

    fun FunctionDescriptor.isTopLevelFunction(_package: String, name: String): Boolean =
        this.isIdentifier(name) && this.inPackage(_package)
}
package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.boxing.TypeParameterEliminator
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.Variance

internal class GenericsSpecialization(val context: Context) : FileLoweringPass {

    private val transformer = SpecializationTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(transformer)
        transformer.newFunctions.forEach { (specialization, origin) ->
            when (val parent = origin.parent) {
                is IrDeclarationContainer -> parent.addChild(specialization)
                is IrStatementContainer -> {
                    val originIndex = parent.statements.indexOf(origin)
                    parent.statements.add(originIndex + 1, specialization)
                    specialization.parent = parent
                }
                is IrFunction -> {
                    val builder = context.createIrBuilder(parent.symbol)
                    val statements = parent.body?.statements?.toMutableList()
                    if (statements == null) {
                        irFile.addChild(specialization)
                    }
                    else {
                        statements.add(statements.indexOf(origin), specialization)
                        specialization.parent = parent
                    }
                    parent.body = builder.irBlockBody(parent) {
                        statements?.forEach {
                            +it
                        }
                    }
                }
                else -> irFile.addChild(specialization)
            }
        }
        transformer.newFunctions.clear()
    }
}

internal class SpecializationTransformer(val context: Context): IrBuildingTransformer(context) {

    private val currentSpecializations = mutableMapOf<Pair<IrFunction, IrType>, IrFunction>()

    // specialization -> origin
    val newFunctions = mutableMapOf<IrFunction, IrFunction>()

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.typeArgumentsCount != 1 || expression.getTypeArgument(0) !in context.irBuiltIns.primitiveIrTypes) {
            return expression
        }
        val primitiveTypeArgument = expression.getTypeArgument(0)!!
        val owner: IrFunction = expression.symbol.owner
        if (owner.name.asString() !in listOf("eqls__", "id__", "localId__", "anonId__", "doDefault__") || owner.typeParameters.size != 1) {
            return expression
        }
        val typeParameter = owner.typeParameters.first()
        if (typeParameter.superTypes.size != 1 || typeParameter.superTypes.first() != context.irBuiltIns.anyNType || typeParameter.variance != Variance.INVARIANT) {
            return expression
        }
        val newFunction = owner.getSpecialization(primitiveTypeArgument)
        return builder.at(expression).run {
            irCall(newFunction).apply {
                extensionReceiver = expression.extensionReceiver
                dispatchReceiver = expression.dispatchReceiver
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i))
                }
            }
        }
    }

    // TODO generalize
    private fun IrFunction.getSpecialization(type: IrType): IrFunction {
        if (this !is IrSimpleFunction) {
            return this
        }
        val function = currentSpecializations[this to type]
        if (function != null) {
            return function
        }
        return (TypeParameterEliminator(context, typeParameters.first(), type).visitFunction(this) as IrFunction).also {
            currentSpecializations[this to type] = it
            newFunctions[it] = this
        }
    }
}
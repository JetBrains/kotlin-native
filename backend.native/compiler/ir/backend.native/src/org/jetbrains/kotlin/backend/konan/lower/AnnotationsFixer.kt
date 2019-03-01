/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// TODO: May be this should be a part of PsiToIr.
internal class AnnotationsFixer : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
                if (element is IrAnnotationContainer) {
                    element.annotations.forEach { fixAnnotation(it) }
                }
            }
        })
    }

    /** TODO: JVM inliner crashed on attempt inline this function from DeepCopyIrTreeWithSymbols.kt with:
     *  j.l.IllegalStateException: Couldn't inline method call 'deepCopyWithSymbols' into
     *  local final fun <anonymous>(it: org.jetbrains.kotlin.ir.declarations.IrValueParameter): kotlin.Unit...
     *  Cause: Not generated
     *  Cause: Couldn't obtain compiled function body for
     *  public inline fun <reified T : org.jetbrains.kotlin.ir.IrElement> T.deepCopyWithSymbols(...)
     **/
    inline fun <reified T : IrElement> T.deepCopyWithSymbols(
            initialParent: IrDeclarationParent? = null,
            descriptorRemapper: DescriptorsRemapper = DescriptorsRemapper.DEFAULT
    ): T {
        val symbolRemapper = DeepCopySymbolRemapper(descriptorRemapper)
        acceptVoid(symbolRemapper)
        val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
        return transform(DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper), null).patchDeclarationParents(initialParent) as T
    }


    fun fixAnnotation(call: IrCall) {
        call.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                expression.symbol.owner.valueParameters.forEach {
                    if (expression.getValueArgument(it.index) == null) {
                        // We need to keep arguments untouched by lowerings.
                        val valueArgument: IrExpression? = (it.defaultValue?.expression?.deepCopyWithSymbols()
                                ?: IrVarargImpl(
                                        startOffset = expression.startOffset,
                                        endOffset = expression.endOffset,
                                        type = it.type,
                                        varargElementType = it.varargElementType!!
                                ))
                        expression.putValueArgument(it.index, valueArgument)
                    }
                }
                super.visitCall(expression)
            }
        })
    }
}
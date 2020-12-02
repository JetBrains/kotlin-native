/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.*

/**
 * Inline accessors of inline class properties.
 * TODO: this lowering is needed because common lowering PropertyAccessorInlineLowering
 * doesn't support properties of inline classes.
 */
internal class InlineClassPropertyAccessorsLowering(private val context: Context) : BodyLoweringPass {
    private inner class AccessorInliner : IrElementTransformerVoid() {

        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)

            val property = expression.symbol.owner.correspondingPropertySymbol?.owner ?: return expression

            property.parent.also {
                if (it is IrClass && it.isInline && property.backingField != null) {
                    return expression.dispatchReceiver ?: expression
                }
            }

            return expression
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(AccessorInliner())
    }
}
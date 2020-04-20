package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.cfg.BasicBlock
import org.jetbrains.kotlin.backend.konan.cfg.reduceOrNull
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.isPrimitiveType

internal object BoxHoisting {

    fun run(functionEntry: BasicBlock): Map<BasicBlock, Set<IrValueSymbol>> {
        val analysisResult = mutableMapOf<BasicBlock, Set<IrValueSymbol>>()

        fun BasicBlock.evaluateLocal() {
            if (this in analysisResult) {
                return
            }
            val prev = incomingEdges.map {
                if (it.from !in analysisResult) {
                    it.from.evaluateLocal()
                }
                analysisResult[it.from]!!
            }.reduceOrNull { acc, next -> acc.intersect(next) }?.toMutableSet() ?: mutableSetOf()

            val gen = mutableSetOf<IrValueSymbol>()
            val kill = mutableSetOf<IrValueSymbol>()
            for (statement in statements) {
                when (statement) {
                    is IrCall -> {
                        val argument = statement.getArgumentIfCallsBoxFunction()
                        if (argument?.isPrimitive() == true) {
                            gen += argument
                        }
                    }

                    is IrSetVariable -> {
                        val argument = statement.symbol
                        if (argument.isPrimitive()) {
                            kill += argument
                        }
                    }
                }
            }
            prev += gen
            prev -= kill
            analysisResult[this] = prev
            for (basicBlock in outgoingEdges.map { it.to }) {
                basicBlock.evaluateLocal()
            }
        }
        functionEntry.evaluateLocal()
        return analysisResult
    }

    private fun IrValueSymbol.isPrimitive() = owner.type.isPrimitiveType()

    private fun IrCall.getArgumentIfCallsBoxFunction(): IrValueSymbol? {
        if (this.isBoxFunctionCall()) {
            when (val argument = getValueArgument(0)!!) {
                is IrGetValue -> return argument.symbol
            }
        }
        return null
    }
    private fun IrCall.isBoxFunctionCall() = symbol.owner.name.asString().endsWith("-box>")
}
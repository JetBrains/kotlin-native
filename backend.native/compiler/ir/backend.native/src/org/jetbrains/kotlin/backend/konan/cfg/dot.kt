package org.jetbrains.kotlin.backend.konan.cfg

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

fun IrFunction.printDotGraph(cfgElementsFilter: CfgElementsFilter): String {
    return Dotifier(name.asString(), buildCfg(cfgElementsFilter).first.decomposed()).dotify()
}

class DotGraph(
        val name: String,
        private val vertices: List<DotVertexDescription>
) {
    override fun toString(): String =
        buildString {
            appendln("digraph $name {")
            vertices.forEach { vertex ->
                vertex.next.forEach { nextVertexName ->
                    appendln("\t${vertex.symbolicName} -> $nextVertexName;")
                }
            }
            vertices.forEach { vertex ->
                appendln("\t${vertex.symbolicName} [label=\"${vertex.label}\"];")
            }
            appendln("}")
        }
}

class DotVertexDescription(
        val symbolicName: String,
        val label: String,
        val next: List<String>
)

class Dotifier(val name: String, val entryBlock: BasicBlock) {
    private val enumeration = entryBlock.enumerate().mapValues { "v${it.value}" }
    private val dotRepresentationVisitor = DotRepresentationVisitor()

    fun dotify(): String {
        val result = mutableListOf<DotVertexDescription>()
        entryBlock.traverseBfs(result, { vertices, block ->
            vertices += DotVertexDescription(
                    symbolicName = enumeration[block]!!,
                    label = block.statements.joinToString("; ") { it.accept(dotRepresentationVisitor, data = null) },
                    next = block.outgoingEdges.map { enumeration[it.to]!! }
            )
        })
        return DotGraph(name, result).toString()
    }
}

class DotRepresentationVisitor: IrElementVisitor<String, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?): String {
        return "unknown: $element"
    }

    override fun visitCall(expression: IrCall, data: Nothing?): String {
        return "call ${expression.symbol.owner.name}"
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): String {
        return "call constructor ${expression.symbol.owner.constructedClass.name}"
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): String {
        return "${expression.operator.name} for type ${expression.typeOperand.render()}"
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String {
        return "variable ${declaration.name}"
    }

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): String {
        return "set to ${expression.symbol.owner.name}"
    }

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String {
        return "get value ${expression.symbol.owner.name}"
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): String {
        return "const ${expression.value}"
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String {
        return "get object ${expression.symbol.owner.name}"
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): String {
        return "return"
    }

    override fun visitBreak(jump: IrBreak, data: Nothing?): String {
        return "break"
    }

    override fun visitContinue(jump: IrContinue, data: Nothing?): String {
        return "continue"
    }
}
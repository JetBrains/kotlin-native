package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.irasdescriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

val dceNotNeeded = mutableSetOf<IrDeclaration>()

internal class IrDeadCode(val context: Context, val moduleDFG: ModuleDFG, val callGraph: CallGraph) : DeclarationContainerLoweringPass, IrElementTransformerVoid() {

    val neededCallGraphNodes: MutableSet<DataFlowIR.FunctionSymbol> by lazy {
        val result = mutableSetOf<DataFlowIR.FunctionSymbol>()
        callGraph.directEdges.forEach {
            val source = it.key
            result.add(source)
            //println("### ClGr: source: ${source.name}")
            // if (it.value.callSites.isEmpty()) println("### ClGr: ${source.name} -> no destinations")
            it.value.callSites.forEach {
                val callee = it.actualCallee
                //println("### ClGr: ${source.name} -> ${callee.name} ${if (it.isVirtual) "[label=\"virtual\"]" else ""}; ")
                result.add(callee)
            }
        }
        result
    }


    fun dceNeededFunction(declaration: IrFunction): Boolean {

        val functionMap: MutableMap<DeclarationDescriptor, DataFlowIR.FunctionSymbol> = moduleDFG.symbolTable.functionMap
        val dfgSymbol = functionMap[declaration]
        if (dfgSymbol == null) return true
        if (neededCallGraphNodes.contains(dfgSymbol)) {
            //println("### dceNEEDED: ${declaration.descriptor} $declaration")

            return true
        } else {
            //println("### dceNotNeeded: ${declaration.descriptor} $declaration")
            dceNotNeeded.add(declaration)
            return false
        }
    }

    fun dceNeeded(declaration: IrDeclaration): Boolean {
        //try {
        //   println("### considering declaration: ${declaration.name}")
        //} catch (e: Throwable) {

        //}

        return when (declaration) {
            is IrFunction -> dceNeededFunction(declaration)
            is IrProperty -> { // TODO: no this is not generic enough. We need to treat inner functions too. Write a normal transformer?
                val getter = declaration.getter
                val setter = declaration.setter
                if (getter != null && !dceNeededFunction(getter)) declaration.getter = null
                if (setter != null && !dceNeededFunction(setter)) declaration.setter = null
                true
            }

            else -> true
        }
    }

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.retainAll {
            dceNeeded(it)
        }
    }

    fun run() {
        //try {
        //if (context.config.produce != CompilerOutputKind.PROGRAM) return
        context.irModule!!.files.forEach { runOnFilePostfix(it) }
        //} catch (e: DeadCodeAbortException) {
        //   println(e.message)
        //}
    }
}
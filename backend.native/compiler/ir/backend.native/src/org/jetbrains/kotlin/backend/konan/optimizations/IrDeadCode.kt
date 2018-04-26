package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.lower.PostInlineLowering
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

val dceNotNeeded = mutableSetOf<IrDeclaration>()

internal class IrDeadCode(val context: Context, val moduleDFG: ModuleDFG, val callGraph: CallGraph):  DeclarationContainerLoweringPass, IrElementTransformerVoid() {

    val neededCallGraphNodes: MutableSet<DataFlowIR.FunctionSymbol> by lazy {
        val result = mutableSetOf<DataFlowIR.FunctionSymbol>()
        callGraph.directEdges.forEach {
            val source = it.key
            result.add(source)
            it.value.callSites.forEach {
                val callee = it.actualCallee
                println("${source.name} -> ${callee.name} ${if (it.isVirtual) "[label=\"virtual\"]" else ""}; ")
                result.add(callee)
            }
        }
        result
    }


    fun dceNeededFunction(declaration: IrFunction): Boolean {


                // A workaround for a CallGraph bug.
                if (declaration.parent is IrClass && (declaration.parent as IrClass).kind == ClassKind.OBJECT && declaration is IrConstructor) {

                  return true
                }

        val functionMap: MutableMap<DeclarationDescriptor, DataFlowIR.FunctionSymbol> =  moduleDFG.symbolTable.functionMap
        val dfgSymbol = functionMap[declaration]
        if (dfgSymbol== null) return true
        if (neededCallGraphNodes.contains(dfgSymbol)) {
            return true
        } else {
            println("### dceNotNeeded: ${declaration.descriptor} $declaration")
            dceNotNeeded.add(declaration)
            return false
        }
    }

    fun dceNeeded(declaration: IrDeclaration): Boolean {
        return when (declaration) {
            is IrFunction ->  dceNeededFunction(declaration)

            else -> true
        }
    }

        override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.retainAll{
            dceNeeded(it)
        }
    }

    fun run() {
        if (context.config.produce != CompilerOutputKind.PROGRAM) return
        context.irModule!!.files.forEach{runOnFilePostfix(it)}
    }
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.fqNameSafe
import org.jetbrains.kotlin.backend.konan.ir.getExternalObjCMethodInfo
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val contextLLVMSetupPhase = makeKonanModuleOpPhase(
        name = "ContextLLVMSetup",
        description = "Set up Context for LLVM Bitcode generation",
        op = { context, _ ->
            // Note that we don't set module target explicitly.
            // It is determined by the target of runtime.bc
            // (see Llvm class in ContextUtils)
            // Which in turn is determined by the clang flags
            // used to compile runtime.bc.
            val llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
            context.llvmModule = llvmModule
            context.debugInfo.builder = DICreateBuilder(llvmModule)
            context.llvmDeclarations = createLlvmDeclarations(context)
            context.lifetimes = mutableMapOf()
            context.codegenVisitor = CodeGeneratorVisitor(context, context.lifetimes)
        }
)

internal val RTTIPhase = makeKonanModuleOpPhase(
        name = "RTTI",
        description = "RTTI generation",
        op = { context, irModule -> irModule.acceptVoid(RTTIGeneratorVisitor(context)) }
)

internal val generateDebugInfoHeaderPhase = makeKonanModuleOpPhase(
        name = "GenerateDebugInfoHeader",
        description = "Generate debug info header",
        op = { context, _ -> generateDebugInfoHeader(context) }
)

internal val buildDFGPhase = makeKonanModuleOpPhase(
        name = "BuildDFG",
        description = "Data flow graph building",
        op = { context, irModule ->
            context.moduleDFG = ModuleDFGBuilder(context, irModule).build()
        }
)

internal val deserializeDFGPhase = makeKonanModuleOpPhase(
        name = "DeserializeDFG",
        description = "Data flow graph deserializing",
        op = { context, _ ->
            context.externalModulesDFG = DFGSerializer.deserialize(
                    context,
                    context.moduleDFG!!.symbolTable.privateTypeIndex,
                    context.moduleDFG!!.symbolTable.privateFunIndex
            )
        }
)

internal val devirtualizationPhase = makeKonanModuleOpPhase(
        name = "Devirtualization",
        description = "Devirtualization",
        prerequisite = setOf(buildDFGPhase),
        op = { context, irModule ->
            context.devirtualizationAnalysisResult = Devirtualization.run(
                    irModule, context, context.moduleDFG!!, ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
            )
        }
)

internal val IrFunction.longName: String
get() = ((this as? IrSimpleFunction)?.let { ((it.parent as? IrClass)?.name?.asString() ?: "<root>") + "." + it.name.asString() } ?: (this as? IrConstructor)?.parentAsClass?.name?.asString()?.plus(".<init>")).toString()

internal val dceClassesPhase = makeKonanModuleOpPhase(
        name = "DCEClassesPhase",
        description = "DCE: Remove unused classes",
        prerequisite = setOf(devirtualizationPhase),
        op = { context, _ ->
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())

            val callGraph = CallGraphBuilder(
                    context, context.moduleDFG!!,
                    externalModulesDFG,
                    context.devirtualizationAnalysisResult,
                    true
            ).build()
            //println("#functions: ${callGraph.directEdges.size}")
            val referencedFunctions = mutableSetOf<IrFunction>()
            for (node in callGraph.directEdges.values) {
                if (!node.symbol.isGlobalInitializer)
                    referencedFunctions.add(node.symbol.irFunction ?: error("No IR for: ${node.symbol}"))
                node.callSites.forEach { referencedFunctions.add(it.actualCallee.irFunction ?: error("No IR for: ${it.actualCallee}")) }
            }
//            println("#referenced functions: ${referencedFunctions.size}")
//            referencedFunctions.forEach {
//                println(((it.parent as? IrClass)?.name?.asString() ?: "<root>") + "." + it.name.asString())
//            }
            val referencedClasses = mutableSetOf<IrClass>()

            fun DataFlowIR.Type.resolved(): DataFlowIR.Type.Declared {
                if (this is DataFlowIR.Type.Declared) return this
                val hash = (this as DataFlowIR.Type.External).hash
                return externalModulesDFG.publicTypes[hash] ?: error("Unable to resolve exported type $hash")
            }

            fun referenceType(type: DataFlowIR.Type.Declared) {
                if (type.irClass == null)
                    println("$type")
                type.irClass?.let { referencedClasses += it }
                type.superTypes.forEach {
                    referenceType(it.resolved())
                }
            }

            context.devirtualizationAnalysisResult!!.instantiatingClasses.forEach { referenceType(it) }

            //println("#referenced classes: ${referencedClasses.size}")
//            referencedClasses.forEach {
//                println(it.fqName)
//            }


//            println("digraph zzz {")
//            val ids = mutableMapOf<DataFlowIR.FunctionSymbol, Int>()
//            var id = 0
//            for (node in callGraph.directEdges.values) {
//                val fromId = ids.getOrPut(node.symbol) { id++ }
//                for (edge in node.callSites.map { it.actualCallee }.distinct()) {
//                    val toId = ids.getOrPut(edge) { id++ }
//                    println("    $fromId -> $toId;")
//                }
//            }
//            ids.forEach { t, u -> println("    $u [label=\"${t.irFunction?.longName ?: t.name}\"];") }
//            println("}")

            context.irModule!!.acceptChildrenVoid(object: IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    // TODO: Generalize somehow.
//                    if (declaration.getExternalObjCMethodInfo() != null) {
//                        //println("Explicitly referencing ${declaration.dump()}")
//                        referencedFunctions.add(declaration)
//                    }
                    if (declaration.name == OperatorNameConventions.INVOKE && declaration.parent.let { it is IrClass && it.defaultType.isFunction() }) {
                        referencedFunctions.add(declaration)
                    }
                    if (declaration.parent.let { it is IrClass && it.isObjCClass() })
                        referencedFunctions.add(declaration)
                    super.visitFunction(declaration)
                }

                override fun visitConstructor(declaration: IrConstructor) {
                    // TODO
                    if (declaration.parentAsClass.name.asString() == InteropFqNames.nativePointedName && declaration.isPrimary)
                        referencedFunctions.add(declaration)
                    super.visitConstructor(declaration)
                }
            })

            //context.referencedClasses = referencedClasses
            context.referencedFunctions = referencedFunctions



//            context.irModule!!.transformChildrenVoid(object: IrElementTransformerVoid() {
//                private fun clearDeclarationContainer(declarationContainer: IrDeclarationContainer) {
//                    declarationContainer.declarations.removeAll {
//                        // TODO: Remove inline classes w/o functions.
//                        (it is IrClass && !referencedClasses.contains(it) && !it.declarations.any { it is IrClass } && !it.isInline).also {
//                            f -> if (f) println("Removing ${(it as IrClass).fqNameSafe}")
//                        }
//                    }
//                }
//
//                override fun visitFile(declaration: IrFile): IrFile {
//                    declaration.transformChildrenVoid(this)
//                    clearDeclarationContainer(declaration)
//                    return declaration
//                }
//
//                override fun visitClass(declaration: IrClass): IrStatement {
//                    declaration.transformChildrenVoid(this)
//                    clearDeclarationContainer(declaration)
//                    return declaration
//                }
//            })
        }
)

internal val dceFunctionsPhase = makeKonanModuleOpPhase(
        name = "DCEFunctionsPhase",
        description = "DCE: Remove unused functions",
        prerequisite = setOf(dceClassesPhase),
        op = { context, _ ->
            val referencedFunctions = context.referencedFunctions ?: return@makeKonanModuleOpPhase
            context.irModule!!.transformChildrenVoid(object: IrElementTransformerVoid() {
                override fun visitFile(declaration: IrFile): IrFile {
                    //if (declaration.fqName.asString() == "kotlin.text.regex" && declaration.declarations.any {it.name.asString() == "RangeSet"})
                      //  println("ZZZ")
                    declaration.declarations.removeAll {
                        (it is IrFunction && it.isReal && !referencedFunctions.contains(it)).also {
                            //f -> if (f) println("Removing ${(it as IrFunction).longName}")
                        }
                    }
                    return super.visitFile(declaration)
                }

                override fun visitClass(declaration: IrClass): IrStatement {
                    if (declaration == context.ir.symbols.nativePointed)
                            return super.visitClass(declaration)
                    declaration.declarations.removeAll {
                        (it is IrFunction && it.isReal && !referencedFunctions.contains(it)).also {
                            //f -> if (f) println("Removing ${(it as IrFunction).longName}")
                        }
                    }
                    return super.visitClass(declaration)
                }

                override fun visitProperty(declaration: IrProperty): IrStatement {
                    if (declaration.getter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                        //println("Removing ${(declaration.getter as IrFunction).longName}")
                        declaration.getter = null
                    }
                    if (declaration.setter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                        //println("Removing ${(declaration.setter as IrFunction).longName}")
                        declaration.setter = null
                    }
                    return super.visitProperty(declaration)
                }
            })

            //println(context.irModule!!.dump())
        }
)

internal val escapeAnalysisPhase = makeKonanModuleOpPhase(
        // Disabled by default !!!!
        name = "EscapeAnalysis",
        description = "Escape analysis",
        prerequisite = setOf(buildDFGPhase, deserializeDFGPhase), // TODO: Requires devirtualization.
        op = { context, _ ->
            context.externalModulesDFG?.let { externalModulesDFG ->
                val callGraph = CallGraphBuilder(
                        context, context.moduleDFG!!,
                        externalModulesDFG,
                        context.devirtualizationAnalysisResult,
                        false
                ).build()
                EscapeAnalysis.computeLifetimes(
                        context, context.moduleDFG!!, externalModulesDFG, callGraph, context.lifetimes
                )
            }
        }
)

internal val serializeDFGPhase = makeKonanModuleOpPhase(
        name = "SerializeDFG",
        description = "Data flow graph serializing",
        prerequisite = setOf(buildDFGPhase), // TODO: Requires escape analysis.
        op = { context, _ ->
            DFGSerializer.serialize(context, context.moduleDFG!!)
        }
)

internal val codegenPhase = makeKonanModuleOpPhase(
        name = "Codegen",
        description = "Code generation",
        op = { context, irModule ->
            var classesCount = 0
            var functionsCount = 0

            irModule.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitClass(declaration: IrClass) {
                    super.visitClass(declaration)

                    ++classesCount
                }

                override fun visitFunction(declaration: IrFunction) {
                    super.visitFunction(declaration)
                    if (declaration.isReal)
                        ++functionsCount
                }
            })

            //println("#classes: $classesCount, #functions: $functionsCount")

            //println(irModule.dump())

//            irModule.acceptVoid(object: IrElementVisitorVoid {
//                override fun visitElement(element: IrElement) {
//                    element.acceptChildrenVoid(this)
//                }
//
//                override fun visitFunction(declaration: IrFunction) {
//                    if (declaration.name.asString() == "testConversions")
//                        println(declaration.dump())
//                    super.visitFunction(declaration)
//                }
//            })

            irModule.acceptVoid(context.codegenVisitor)
        }
)

internal val finalizeDebugInfoPhase = makeKonanModuleOpPhase(
        name = "FinalizeDebugInfo",
        description = "Finalize debug info",
        op = { context, _ ->
            if (context.shouldContainDebugInfo()) {
                DIFinalize(context.debugInfo.builder)
            }
        }
)

internal val cStubsPhase = makeKonanModuleOpPhase(
        name = "CStubs",
        description = "C stubs compilation",
        op = { context, _ -> produceCStubs(context) }
)

/**
 * Runs specific passes over context.llvmModule. The main compilation pipeline
 * is performed by [linkPhase].
 */
internal val bitcodePassesPhase = makeKonanModuleOpPhase(
        name = "BitcodePasses",
        description = "Run custom LLVM passes over bitcode",
        op = { context, _ -> runBitcodePasses(context) }
)

internal val produceOutputPhase = makeKonanModuleOpPhase(
        name = "ProduceOutput",
        description = "Produce output",
        op = { context, _ -> produceOutput(context) }
)

internal val verifyBitcodePhase = makeKonanModuleOpPhase(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { context, _ -> context.verifyBitCode() }
)

internal val printBitcodePhase = makeKonanModuleOpPhase(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = { context, _ ->
            if (context.shouldPrintBitCode()) {
                context.printBitCode()
            }
        }
)
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.common.CompilerPhase
import org.jetbrains.kotlin.backend.common.CompilerPhaseManager
import org.jetbrains.kotlin.backend.common.makePhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

private fun makeBitcodePhase(
        op: CompilerPhaseManager<Context, IrModuleFragment>.(IrModuleFragment) -> Unit,
        description: String,
        name: String,
        prerequisite: Set<CompilerPhase<Context, IrModuleFragment>> = emptySet()
) = makePhase(op, description, name, prerequisite)

internal val ContextLLVMSetupPhase = makeBitcodePhase(
        name = "ContextLLVMSetup",
        description = "Set up Context for LLVM bitcode generation",
        op = { irModule ->
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

internal val RTTIPhase = makeBitcodePhase(
        name = "RTTI",
        description = "RTTI Generation",
        op = { irModule -> irModule.acceptVoid(RTTIGeneratorVisitor(context)) }
)

internal val GenerateDebugInfoHeaderPhase = makeBitcodePhase(
        name = "GenerateDebugInfoHeader",
        description = "Generate debug info header",
        op = { irModule -> generateDebugInfoHeader(context) }
)

internal val BuildDFGPhase = makeBitcodePhase(
        name = "BuildDFG",
        description = "Data flow graph building",
        op = { irModule ->
            context.moduleDFG = ModuleDFGBuilder(context, irModule).build()
        }
)

internal val DeserializeDFGPhase = makeBitcodePhase(
        name = "DeserializeDFG",
        description = "Data flow graph deserializing",
        op = { irModule ->
            context.externalModulesDFG = DFGSerializer.deserialize(
                    context,
                    context.moduleDFG!!.symbolTable.privateTypeIndex,
                    context.moduleDFG!!.symbolTable.privateFunIndex
            )
        }
)

internal val DevirtualizationPhase = makeBitcodePhase(
        name = "Devirtualization",
        description = "Devirtualization",
        prerequisite = setOf(BuildDFGPhase, DeserializeDFGPhase),
        op = { irModule ->
            context.externalModulesDFG?.let { externalModulesDFG ->
                context.devirtualizationAnalysisResult = Devirtualization.run(
                        irModule, context, context.moduleDFG!!, externalModulesDFG
                )

                val privateFunctions = context.moduleDFG!!.symbolTable.getPrivateFunctionsTableForExport()
                privateFunctions.forEachIndexed { index, it ->
                    val function = context.codegenVisitor.codegen.llvmFunction(it.first)
                    LLVMAddAlias(
                            context.llvmModule,
                            function.type,
                            function,
                            irModule.descriptor.privateFunctionSymbolName(index, it.second.name)
                    )!!
                }
                context.privateFunctions = privateFunctions

                val privateClasses = context.moduleDFG!!.symbolTable.getPrivateClassesTableForExport()

                privateClasses.forEachIndexed { index, it ->
                    val typeInfoPtr = context.codegenVisitor.codegen.typeInfoValue(it.first)
                    LLVMAddAlias(
                            context.llvmModule,
                            typeInfoPtr.type,
                            typeInfoPtr,
                            irModule.descriptor.privateClassSymbolName(index, it.second.name)
                    )!!
                }
                context.privateClasses = privateClasses
            }
        }
)

internal val EscapeAnalysisPhase = makeBitcodePhase(
        // Disabled by default !!!!
        name = "EscapeAnalysis",
        description = "Escape analysis",
        prerequisite = setOf(BuildDFGPhase, DeserializeDFGPhase),
        op = { irModule ->
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

internal val SerializeDFGPhase = makeBitcodePhase(
        name = "SerializeDFG",
        description = "Data flow graph serializing",
        prerequisite = setOf(BuildDFGPhase),
        op = { irModule ->
            DFGSerializer.serialize(context, context.moduleDFG!!)
        }
)

internal val CodegenPhase = makeBitcodePhase(
        name = "Codegen",
        description = "Code Generation",
        op = { irModule ->
            irModule.acceptVoid(context.codegenVisitor)
        }
)

internal val FinalizeDebugInfoPhase = makeBitcodePhase(
        name = "FinalizeDebuginfo",
        description = "Finalize debug info",
        op = {
            if (context.shouldContainDebugInfo()) {
                DIFinalize(context.debugInfo.builder)
            }
        }
)

internal val BitcodeLinkerPhase = makeBitcodePhase(
        name = "BitcodeLinker",
        description = "Bitcode linking",
        op = { produceOutput(context) }
)

internal val VerifyBitcodePhase = makeBitcodePhase(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { context.verifyBitCode() }
)

internal val PrintBitcodePhase = makeBitcodePhase(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = {
            if (context.shouldPrintBitCode()) {
                context.printBitCode()
            }
        }
)

internal val bitcodePhaseList = listOf(
        ContextLLVMSetupPhase,
        RTTIPhase,
        GenerateDebugInfoHeaderPhase,
        BuildDFGPhase,
        DeserializeDFGPhase,
        DevirtualizationPhase,
        EscapeAnalysisPhase,
        SerializeDFGPhase,
        CodegenPhase,
        FinalizeDebugInfoPhase,
        BitcodeLinkerPhase,
        VerifyBitcodePhase,
        PrintBitcodePhase
)
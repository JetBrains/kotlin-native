/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal val ContextLLVMSetupPhase = makeKonanModuleOpPhase(
        name = "ContextLLVMSetup",
        description = "Set up Context for LLVM bitcode generation",
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
        description = "RTTI Generation",
        op = { context, irModule -> irModule.acceptVoid(RTTIGeneratorVisitor(context)) }
)

internal val GenerateDebugInfoHeaderPhase = makeKonanModuleOpPhase(
        name = "GenerateDebugInfoHeader",
        description = "Generate debug info header",
        op = { context, _ -> generateDebugInfoHeader(context) }
)

internal val BuildDFGPhase = makeKonanModuleOpPhase(
        name = "BuildDFG",
        description = "Data flow graph building",
        op = { context, irModule ->
            context.moduleDFG = ModuleDFGBuilder(context, irModule).build()
        }
)

internal val DeserializeDFGPhase = makeKonanModuleOpPhase(
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

internal val DevirtualizationPhase = makeKonanModuleOpPhase(
        name = "Devirtualization",
        description = "Devirtualization",
        prerequisite = setOf(BuildDFGPhase, DeserializeDFGPhase),
        op = { context, irModule ->
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

internal val EscapeAnalysisPhase = makeKonanModuleOpPhase(
        // Disabled by default !!!!
        name = "EscapeAnalysis",
        description = "Escape analysis",
        prerequisite = setOf(BuildDFGPhase, DeserializeDFGPhase),
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

internal val SerializeDFGPhase = makeKonanModuleOpPhase(
        name = "SerializeDFG",
        description = "Data flow graph serializing",
        prerequisite = setOf(BuildDFGPhase),
        op = { context, _ ->
            DFGSerializer.serialize(context, context.moduleDFG!!)
        }
)

internal val CodegenPhase = makeKonanModuleOpPhase(
        name = "Codegen",
        description = "Code Generation",
        op = { context, irModule ->
            irModule.acceptVoid(context.codegenVisitor)
        }
)

internal val FinalizeDebugInfoPhase = makeKonanModuleOpPhase(
        name = "FinalizeDebugInfo",
        description = "Finalize debug info",
        op = { context, _ ->
            if (context.shouldContainDebugInfo()) {
                DIFinalize(context.debugInfo.builder)
            }
        }
)

internal val BitcodeLinkerPhase = makeKonanModuleOpPhase(
        name = "BitcodeLinker",
        description = "Bitcode linking",
        op = { context, _ -> produceOutput(context) }
)

internal val VerifyBitcodePhase = makeKonanModuleOpPhase(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { context, _ -> context.verifyBitCode() }
)

internal val PrintBitcodePhase = makeKonanModuleOpPhase(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = { context, _ ->
            if (context.shouldPrintBitCode()) {
                context.printBitCode()
            }
        }
)


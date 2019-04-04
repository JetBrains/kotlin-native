/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

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
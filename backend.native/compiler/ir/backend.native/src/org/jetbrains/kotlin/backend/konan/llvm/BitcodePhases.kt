/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.common.NamedPhase
import org.jetbrains.kotlin.backend.common.then
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal val ContextLLVMSetupPhase = makeKonanPhase<IrModuleFragment>(
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

internal val RTTIPhase = makeKonanPhase<IrModuleFragment>(
        name = "RTTI",
        description = "RTTI Generation",
        op = { context, irModule -> irModule.acceptVoid(RTTIGeneratorVisitor(context)) }
)

internal val GenerateDebugInfoHeaderPhase = makeKonanPhase<IrModuleFragment>(
        name = "GenerateDebugInfoHeader",
        description = "Generate debug info header",
        op = { context, _ -> generateDebugInfoHeader(context) }
)

internal val BuildDFGPhase = makeKonanPhase<IrModuleFragment>(
        name = "BuildDFG",
        description = "Data flow graph building",
        op = { context, irModule ->
            context.moduleDFG = ModuleDFGBuilder(context, irModule).build()
        }
)

internal val DeserializeDFGPhase = makeKonanPhase<IrModuleFragment>(
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

internal val DevirtualizationPhase = makeKonanPhase<IrModuleFragment>(
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

internal val EscapeAnalysisPhase = makeKonanPhase<IrModuleFragment>(
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

internal val SerializeDFGPhase = makeKonanPhase<IrModuleFragment>(
        name = "SerializeDFG",
        description = "Data flow graph serializing",
        prerequisite = setOf(BuildDFGPhase),
        op = { context, _ ->
            DFGSerializer.serialize(context, context.moduleDFG!!)
        }
)

internal val CodegenPhase = makeKonanPhase<IrModuleFragment>(
        name = "Codegen",
        description = "Code Generation",
        op = { context, irModule ->
            irModule.acceptVoid(context.codegenVisitor)
        }
)

internal val FinalizeDebugInfoPhase = makeKonanPhase<IrModuleFragment>(
        name = "FinalizeDebugInfo",
        description = "Finalize debug info",
        op = { context, _ ->
            if (context.shouldContainDebugInfo()) {
                DIFinalize(context.debugInfo.builder)
            }
        }
)

internal val BitcodeLinkerPhase = makeKonanPhase<IrModuleFragment>(
        name = "BitcodeLinker",
        description = "Bitcode linking",
        op = { context, _ -> produceOutput(context) }
)

internal val VerifyBitcodePhase = makeKonanPhase<IrModuleFragment>(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { context, _ -> context.verifyBitCode() }
)

internal val PrintBitcodePhase = makeKonanPhase<IrModuleFragment>(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = { context, _ ->
            if (context.shouldPrintBitCode()) {
                context.printBitCode()
            }
        }
)


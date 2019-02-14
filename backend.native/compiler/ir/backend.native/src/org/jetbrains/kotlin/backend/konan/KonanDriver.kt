/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.ModuleIndex
import org.jetbrains.kotlin.backend.konan.llvm.emitLLVM
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.konan.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

fun runTopLevelPhases(konanConfig: KonanConfig, environment: KotlinCoreEnvironment) {

    val config = konanConfig.configuration

    val targets = konanConfig.targetManager
    if (config.get(KonanConfigKeys.LIST_TARGETS) ?: false) {
        targets.list()
    }

    KonanPhases.config(konanConfig)
    if (config.get(KonanConfigKeys.LIST_PHASES) ?: false) {
        KonanPhases.list()
    }

    if (konanConfig.infoArgsOnly) return

    val context = Context(konanConfig)

    val analyzerWithCompilerReport = AnalyzerWithCompilerReport(context.messageCollector,
            environment.configuration.languageVersionSettings)

    val phaser = PhaseManager(context, null)

    phaser.phase(KonanPhase.FRONTEND) {

        //environment.getSourceFiles().forEach { println("ZZZ: ${it.name}") }

        // Build AST and binding info.
        analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
            TopDownAnalyzerFacadeForKonan.analyzeFiles(environment.getSourceFiles(), konanConfig)
        }
        if (analyzerWithCompilerReport.hasErrors()) {
            throw KonanCompilationException()
        }
        context.moduleDescriptor = analyzerWithCompilerReport.analysisResult.moduleDescriptor
    }

    val bindingContext = analyzerWithCompilerReport.analysisResult.bindingContext

    lateinit var irModules: MutableMap<String, IrModuleFragment>

    phaser.phase(KonanPhase.PSI_TO_IR) {
        // Translate AST to high level IR.
        val translator = Psi2IrTranslator(context.config.configuration.languageVersionSettings,
                Psi2IrConfiguration(false))
        val generatorContext = translator.createGeneratorContext(context.moduleDescriptor, bindingContext)
        @Suppress("DEPRECATION")
        context.psi2IrGeneratorContext = generatorContext

        val forwardDeclarationsModuleDescriptor = context.moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }

        val deserializer = KonanIrModuleDeserializer(
            context.moduleDescriptor,
            context as LoggingContext,
            generatorContext.irBuiltIns,
            generatorContext.symbolTable,
            forwardDeclarationsModuleDescriptor
        )

        println("psi2ir:")

        context.config.librariesWithDependencies(context.moduleDescriptor).forEach { println("DEPENDENCY: ${it.libraryName}") }

        irModules = mutableMapOf()

        var dependenciesCount = 0
        while (true) {
            // context.config.librariesWithDependencies could change at each iteration.
            val dependencies = context.moduleDescriptor.allDependencyModules.filter {
                context.config.librariesWithDependencies(context.moduleDescriptor).contains(it.konanLibrary)
            }
            for (dependency in dependencies) {
                val konanLibrary = dependency.konanLibrary!!
                if (irModules.containsKey(konanLibrary.libraryName)) continue
                println("Deserializing header of ${konanLibrary.libraryName}")
                konanLibrary.irHeader?.let { header ->
                    irModules[konanLibrary.libraryName] = deserializer.deserializeIrModuleHeader(dependency, header)
                }
            }
            if (dependencies.size == dependenciesCount) break
            dependenciesCount = dependencies.size
//            irModules = context.moduleDescriptor.allDependencyModules.map {
//                val library = it.konanLibrary
//                if (library == null || !context.config.librariesWithDependencies(context.moduleDescriptor).contains(library)) {
//                    return@map null
//                }
//                println("    ${library.libraryName}")
//                library.irHeader?.let { header -> library.libraryName to deserializer.deserializeIrModuleHeader(it, header) }
//            }.filterNotNull().associate { it }
        }


        val symbols = KonanSymbols(context, generatorContext.symbolTable, generatorContext.symbolTable.lazyWrapper)
        val module = translator.generateModuleFragment(generatorContext, environment.getSourceFiles(), deserializer)

        //irModules = deserializer.irModules

        irModules.values.forEach {
            it.patchDeclarationParents()
        }

        context.irModule = module
        context.ir.symbols = symbols

//        validateIrModule(context, module)
    }
    phaser.phase(KonanPhase.IR_GENERATOR_PLUGINS) {
        val extensions = IrGenerationExtension.getInstances(context.config.project)
        extensions.forEach { extension ->
            context.irModule!!.files.forEach { irFile -> extension.generate(irFile, context, bindingContext) }
        }
    }

    // TODO: We copy default value expressions from expects to actuals before IR serialization,
    // because the current infrastructure doesn't allow us to get them at deserialization stage.
    // That equires some design and implementation work.
    phaser.phase(KonanPhase.COPY_DEFAULT_VALUES_TO_ACTUAL) {
        context.irModule!!.files.forEach(ExpectToActualDefaultValueCopier(context)::lower)
    }

    context.irModule!!.patchDeclarationParents() // why do we need it?

    phaser.phase(KonanPhase.SERIALIZER) {
        val declarationTable = DeclarationTable(context.irModule!!.irBuiltins, DescriptorTable())
        val serializedIr = IrModuleSerializer(
                context, declarationTable/*, bodiesOnlyForInlines = context.config.isInteropStubs*/).serializedIrModule(context.irModule!!)
        val serializer = KonanSerializationUtil(context, context.config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!, declarationTable)
        context.serializedLinkData =
            serializer.serializeModule(context.moduleDescriptor, /*if (!context.config.isInteropStubs) serializedIr else null*/ serializedIr)
    }
    phaser.phase(KonanPhase.BACKEND) {
        phaser.phase(KonanPhase.LOWER) {

//            KonanLower(context, phaser).lower()
//
//            val irModule = context.irModule!!
//            val files = mutableListOf<IrFile>()
//            files += irModule.files
//            irModule.files.clear()
//            for (libModule in irModules) {
//                irModule.files += libModule.value.files
//                KonanLower(context, phaser).lower()
//                irModule.files.clear()
//            }
//            irModule.files += files

            val irModule = context.irModule!!

            println("LOWERING ${irModule.name}")

            KonanLower(context, phaser).lower()

            val files = mutableListOf<IrFile>()
            files += irModule.files
            irModule.files.clear()

            context.config.librariesWithDependencies(context.moduleDescriptor)
                    //context.config.resolvedLibraries
                    //.getFullList(TopologicalLibraryOrder)
                    .reversed()
                    .forEach {
                        val libModule = irModules[it.libraryName] ?: return@forEach

                        println("LOWERING ${libModule.name}")

                        irModule.files += libModule.files
                        KonanLower(context, phaser).lower()

//                        if (libModule.name.asString().contains("interop") && context.ir.symbols.entryPoint != null)
//                            println(irModule.dump())

                        irModule.files.clear()
                    }

            irModule.files += files

//            validateIrModule(context, context.ir.irModule) // Temporarily disabled until moving to new IR finished.
            context.ir.moduleIndexForCodegen = ModuleIndex(context.ir.irModule)
        }
        phaser.phase(KonanPhase.BITCODE) {
            if (config.get(KonanConfigKeys.PRODUCE) != CompilerOutputKind.LIBRARY) {

                val irModule = context.irModule!!
                val files = mutableListOf<IrFile>()
                files += irModule.files
                irModule.files.clear()

                context.config.librariesWithDependencies(context.moduleDescriptor)
                //context.config.resolvedLibraries
                  //      .getFullList(TopologicalLibraryOrder)
                        .forEach {
                            println("   ${it.libraryName}")
                            val libModule = irModules[it.libraryName] ?: return@forEach
                            irModule.files += libModule.files
                        }
                irModule.files += files

                //println(context.irModule!!.dump())

                emitLLVM(context, phaser)
            }
            produceOutput(context, phaser)
        }
        // We always verify bitcode to prevent hard to debug bugs.
        context.verifyBitCode()

        if (context.shouldPrintBitCode()) {
            context.printBitCode()
        }
    }

    phaser.phase(KonanPhase.LINK_STAGE) {
        LinkStage(context, phaser).linkStage()
    }
}


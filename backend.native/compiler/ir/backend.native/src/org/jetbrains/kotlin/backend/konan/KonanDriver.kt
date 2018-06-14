/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.WithLogger
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.validateIrModule
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.ModuleIndex
import org.jetbrains.kotlin.backend.konan.ir.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.backend.konan.llvm.emitLLVM
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.hasInlineFunctions
import org.jetbrains.kotlin.konan.utils.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.serialization.konan.impl.moduleToLibrary
import org.jetbrains.kotlin.storage.LockBasedStorageManager

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

    phaser.phase(KonanPhase.PSI_TO_IR) {
        // Translate AST to high level IR.
        val translator = Psi2IrTranslator(context.config.configuration.languageVersionSettings,
                Psi2IrConfiguration(false))
        val generatorContext = translator.createGeneratorContext(context.moduleDescriptor, bindingContext)
        @Suppress("DEPRECATION")
        context.psi2IrGeneratorContext = generatorContext

        val deserializer = IrModuleDeserialization(context as WithLogger, context.moduleDescriptor, generatorContext.irBuiltIns, generatorContext.symbolTable)
        val specifics = context.config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!
        //val libraries = context.config.resolvedLibraries.getFullList()

        val irModules = context.moduleDescriptor.allDependencyModules.map {
            val library = moduleToLibrary[it]
            if (library == null) return@map null
            deserializer.deserializedIrModule(it, library.wholeIr, {uniqid -> library.irDeclaration(uniqid.index, uniqid.isLocal)})
        }.filterNotNull()

        irModules.forEach {
            generatorContext.symbolTable.loadModule(it)
        }

        val symbols = KonanSymbols(context, generatorContext.symbolTable, generatorContext.symbolTable.lazyWrapper)
        val module = translator.generateModuleFragment(generatorContext, environment.getSourceFiles(), deserializer)

/*
        println("moving declarations with inlines to the current module")
        val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(context.moduleDescriptor!!, FqName("forinlines"))
        val fileSymbol = IrFileSymbolImpl(packageFragmentDescriptor)
        val inlineFile = IrFileImpl(NaiveSourceBasedFileEntryImpl("forinlines"), fileSymbol, FqName("forinlines"))
        irModules.forEach { module ->
            module.files.forEach { file ->
                //println("adding ${file.fileEntry.name}")
                //module.files.add(file)
                file.declarations.forEach {
                    if (it.hasInlineFunctions()) {
                        inlineFile.declarations.add(it)
                        it.parent = inlineFile
                    }
                }
            }
        }
*/
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
    phaser.phase(KonanPhase.GEN_SYNTHETIC_FIELDS) {
        markBackingFields(context)
    }
    phaser.phase(KonanPhase.SERIALIZER) {
        val declarationTable = DeclarationTable(context.irModule!!.irBuiltins, DescriptorTable())
        val serializedIr = IrModuleSerialization(context, declarationTable, onlyForInlines = false).serializedIrModule(context.irModule!!)

        val serializer = KonanSerializationUtil(context, context.config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!, declarationTable)
        context.serializedLinkData =
            serializer.serializeModule(context.moduleDescriptor, serializedIr)
    }
    phaser.phase(KonanPhase.BACKEND) {
        phaser.phase(KonanPhase.LOWER) {
            KonanLower(context, phaser).lower()
//            validateIrModule(context, context.ir.irModule) // Temporarily disabled until moving to new IR finished.
            context.ir.moduleIndexForCodegen = ModuleIndex(context.ir.irModule)
        }
        phaser.phase(KonanPhase.BITCODE) {
            emitLLVM(context, phaser)
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


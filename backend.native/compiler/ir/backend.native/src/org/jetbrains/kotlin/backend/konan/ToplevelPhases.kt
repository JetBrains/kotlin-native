package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanSerializationUtil
import org.jetbrains.kotlin.backend.konan.serialization.markBackingFields
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import java.util.Collections.emptySet

internal fun konanUnitPhase(
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet(),
        op: Context.() -> Unit
) = namedOpUnitPhase(name, description, prerequisite, op)

internal val FrontendPhase = konanUnitPhase(
        op = {
            val environment = environment
            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector,
                    environment.configuration.languageVersionSettings)

            // Build AST and binding info.
            analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
                TopDownAnalyzerFacadeForKonan.analyzeFiles(environment.getSourceFiles(), this)
            }
            if (analyzerWithCompilerReport.hasErrors()) {
                throw KonanCompilationException()
            }
            moduleDescriptor = analyzerWithCompilerReport.analysisResult.moduleDescriptor
            bindingContext = analyzerWithCompilerReport.analysisResult.bindingContext
        },
        name = "Frontend",
        description = "Frontend builds AST"
)

internal val PsiToIrPhase = konanUnitPhase(
        op = {
            // Translate AST to high level IR.
            val translator = Psi2IrTranslator(config.configuration.languageVersionSettings,
                    Psi2IrConfiguration(false))
            val generatorContext =
                    translator.createGeneratorContext(moduleDescriptor, bindingContext)
            @Suppress("DEPRECATION")
            psi2IrGeneratorContext = generatorContext

            val symbols = KonanSymbols(this, generatorContext.symbolTable, generatorContext.symbolTable.lazyWrapper)

            val module =
                    translator.generateModuleFragment(generatorContext, environment.getSourceFiles())

            irModule = module
            ir.symbols = symbols

//        validateIrModule(context, module)
        },
        name = "Psi2Ir",
        description = "Psi to IR conversion"
)

internal val IrGeneratorPluginsPhase = konanUnitPhase(
        op = {
            val extensions = IrGenerationExtension.getInstances(config.project)
            extensions.forEach { extension ->
                irModule!!.files.forEach {
                    irFile -> extension.generate(irFile, this, bindingContext)
                }
            }
        },
        name = "IrGeneratorPlugins",
        description = "Plugged-in ir generators"
)

internal val GenSyntheticFieldsPhase = konanUnitPhase(
        op = { markBackingFields(this) },
        name = "GenSyntheticFields",
        description = "Generate synthetic fields"
)

internal val SerializerPhase = konanUnitPhase(
        op = {
            val serializer = KonanSerializationUtil(
                    this, config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!
            )
            serializedLinkData = serializer.serializeModule(moduleDescriptor)

        },
        name = "Serializer",
        description = "Serialize descriptor tree and inline IR bodies",
        prerequisite = setOf(GenSyntheticFieldsPhase)
)

internal val SetUpLinkStagePhase = konanUnitPhase(
        op =  { linkStage = LinkStage(this) },
        name = "SetUpLinkStage",
        description = "Set up link stage"
)

internal val ObjectFilesPhase = konanUnitPhase(
        op = { linkStage.makeObjectFiles() },
        name = "ObjectFiles",
        description = "Bitcode to object file"
)

internal val LinkerPhase = konanUnitPhase(
        op = { linkStage.linkStage() },
        name = "Linker",
        description = "Linker"
)

internal val LinkPhase = namedUnitPhase(
        name = "Link",
        description = "Link stage",
        lower = SetUpLinkStagePhase then
                ObjectFilesPhase then
                LinkerPhase
)

internal val ToplevelPhase = namedUnitPhase(
        name = "Compiler",
        description = "The whole compilation process",
        lower = FrontendPhase then
                PsiToIrPhase then
                IrGeneratorPluginsPhase then
                GenSyntheticFieldsPhase then
                SerializerPhase then
                namedUnitPhase(
                        name = "Backend",
                        description = "All backend",
                        lower = takeFromContext<Context, Unit, IrModuleFragment> { it.irModule!! } then
                                namedIrModulePhase(
                                        name = "IrLowering",
                                        description = "IR Lowering",
                                        lower = RemoveExpectDeclarationsPhase then
                                                TestProcessorPhase then
                                                LowerBeforeInlinePhase then
                                                InlinePhase then
                                                LowerAfterInlinePhase then
                                                InteropPart1Phase then
                                                LateinitPhase then
                                                ReplaceUnboundSymbolsPhase then
                                                PatchDeclarationParents1Phase then
                                                performByIrFile(
                                                        name = "IrLowerByFile",
                                                        description = "IR Lowering by file",
                                                        lower = StringConcatenationPhase then
                                                                DataClassesPhase then
                                                                ForLoopsPhase then
                                                                EnumClassPhase then
                                                                PatchDeclarationParents2Phase then
                                                                InitializersPhase then
                                                                SharedVariablesPhase then
                                                                DelegationPhase then
                                                                CallableReferencePhase then
                                                                PatchDeclarationParents3Phase then
                                                                LocalDeclarationsPhase then
                                                                TailrecPhase then
                                                                FinallyBlocksPhase then
                                                                DefaultParameterExtentPhase then
                                                                BuiltinOperatorPhase then
                                                                InnerClassPhase then
                                                                InteropPart2Phase then
                                                                VarargPhase then
                                                                CompileTimeEvaluatePhase then
                                                                CoroutinesPhase then
                                                                TypeOperatorPhase then
                                                                BridgesPhase then
                                                                AutoboxPhase then
                                                                ReturnsInsertionPhase
                                                ) then
                                                CheckDeclarationParentsPhase
                                ) then
                                namedIrModulePhase(
                                        name = "Bitcode",
                                        description = "LLVM BitCode Generation",
                                        lower = ContextLLVMSetupPhase then
                                                RTTIPhase then
                                                GenerateDebugInfoHeaderPhase then
                                                BuildDFGPhase then
                                                DeserializeDFGPhase then
                                                DevirtualizationPhase then
                                                EscapeAnalysisPhase then
                                                SerializeDFGPhase then
                                                CodegenPhase then
                                                FinalizeDebugInfoPhase then
                                                BitcodeLinkerPhase then
                                                VerifyBitcodePhase then
                                                PrintBitcodePhase
                                ) then
                                unitSink()
                ) then
                LinkPhase
)

internal fun PhaseConfig.konanPhasesConfig(config: KonanConfig) {
    with(config.configuration) {
        disable(EscapeAnalysisPhase)

        // Don't serialize anything to a final executable.
        switch(SerializerPhase, config.produce == CompilerOutputKind.LIBRARY)
        switch(LinkPhase, config.produce.isNativeBinary)
        switch(TestProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE)
    }
}
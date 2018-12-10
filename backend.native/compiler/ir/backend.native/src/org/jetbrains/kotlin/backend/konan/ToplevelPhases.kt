package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.EscapeAnalysisPhase
import org.jetbrains.kotlin.backend.konan.llvm.emitLLVM
import org.jetbrains.kotlin.backend.konan.serialization.KonanSerializationUtil
import org.jetbrains.kotlin.backend.konan.serialization.markBackingFields
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

private fun makeToplevelPhase(
        op: CompilerPhaseManager<Context, Unit>.(Context) -> Unit,
        description: String,
        name: String,
        prerequisite: Set<CompilerPhase<Context, Unit>> = emptySet()
) = makePhase<Context, Unit>({ op(context) }, description, name, prerequisite)

private fun makeIrModulePhase(
        op: CompilerPhaseManager<Context, IrModuleFragment>.(IrModuleFragment) -> Unit,
        description: String,
        name: String,
        prerequisite: Set<CompilerPhase<Context, IrModuleFragment>> = emptySet()
) = makePhase(op, description, name, prerequisite)

internal object StartToplevelPhase : CompilerPhase<Context, Unit> {
    override val name = "Start"
    override val description = "Start compilation"
    override fun invoke(manager: CompilerPhaseManager<Context, Unit>, input: Unit) = input
}

internal object EndToplevelPhase : CompilerPhase<Context, Unit> {
    override val name = "End"
    override val description = "End compilation"
    override fun invoke(manager: CompilerPhaseManager<Context, Unit>, input: Unit) = input
}

internal val FrontendPhase = makeToplevelPhase(
        op = {
            val environment = context.environment
            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(context.messageCollector,
                    environment.configuration.languageVersionSettings)

            // Build AST and binding info.
            analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
                TopDownAnalyzerFacadeForKonan.analyzeFiles(environment.getSourceFiles(), context)
            }
            if (analyzerWithCompilerReport.hasErrors()) {
                throw KonanCompilationException()
            }
            context.moduleDescriptor = analyzerWithCompilerReport.analysisResult.moduleDescriptor
            context.bindingContext = analyzerWithCompilerReport.analysisResult.bindingContext
        },
        name = "Frontend",
        description = "Frontend builds AST"
)

internal val PsiToIrPhase = makeToplevelPhase(
        op = {
            // Translate AST to high level IR.
            val translator = Psi2IrTranslator(context.config.configuration.languageVersionSettings,
                    Psi2IrConfiguration(false))
            val generatorContext =
                    translator.createGeneratorContext(context.moduleDescriptor, context.bindingContext)
            @Suppress("DEPRECATION")
            context.psi2IrGeneratorContext = generatorContext

            val symbols = KonanSymbols(context, generatorContext.symbolTable, generatorContext.symbolTable.lazyWrapper)

            val module =
                    translator.generateModuleFragment(generatorContext, context.environment.getSourceFiles())

            context.irModule = module
            context.ir.symbols = symbols

//        validateIrModule(context, module)
        },
        name = "Psi2Ir",
        description = "Psi to IR conversion"
)

internal val IrGeneratorPluginsPhase = makeToplevelPhase(
        op = {
            val extensions = IrGenerationExtension.getInstances(context.config.project)
            extensions.forEach { extension ->
                context.irModule!!.files.forEach {
                    irFile -> extension.generate(irFile, context, context.bindingContext)
                }
            }
        },
        name = "IrGeneratorPlugins",
        description = "Plugged-in ir generators"
)

internal val GenSyntheticFieldsPhase = makeToplevelPhase(
        op = { markBackingFields(context) },
        name = "GenSyntheticFields",
        description = "Generate synthetic fields"
)

internal val SerializerPhase = makeToplevelPhase(
        op = {
            val serializer = KonanSerializationUtil(
                    context, context.config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!
            )
            context.serializedLinkData =
                    serializer.serializeModule(context.moduleDescriptor)

        },
        name = "Serializer",
        description = "Serialize descriptor tree and inline IR bodies",
        prerequisite = setOf(GenSyntheticFieldsPhase)
)

internal val BackendPhase = makeToplevelPhase(
        op = {
            createChildManager(context.irModule!!, KonanIrModulePhaseRunner).runPhases(backendPhaseList)
        },
        name = "Backend",
        description = "All backend"
)

internal val LinkPhase = makeToplevelPhase(
        op = {
            createChildManager(Unit, KonanUnitPhaseRunner).runPhases(linkPhaseList)
        },
        name = "Link",
        description = "Link stage"
)

internal val toplevelPhaseList = listOf(
        StartToplevelPhase,
        FrontendPhase,
        PsiToIrPhase,
        IrGeneratorPluginsPhase,
        GenSyntheticFieldsPhase,
        SerializerPhase,
        BackendPhase,
        LinkPhase,
        EndToplevelPhase
)

internal val LowerPhase = makeIrModulePhase(
        op = {  KonanLower(context, this).lower() },
        name = "Lower",
        description = "IR Lowering"
)

internal val BitcodePhase = makeIrModulePhase(
        op = { emitLLVM(context, this) },
        name = "Bitcode",
        description = "LLVM BitCode Generation"
)

internal val backendPhaseList = listOf(
        LowerPhase,
        BitcodePhase
)

internal val SetUpLinkStagePhase = makeToplevelPhase(
        op =  { context.linkStage = LinkStage(context) },
        name = "SetUpLinkStage",
        description = "Set up link stage"
)

internal val ObjectFilesPhase = makeToplevelPhase(
        op = { context.linkStage.makeObjectFiles() },
        name = "ObjectFiles",
        description = "Bitcode to object file"
)

internal val LinkerPhase = makeToplevelPhase(
        op = { context.linkStage.linkStage() },
        name = "Linker",
        description = "Linker"
)

internal val linkPhaseList = listOf(
        SetUpLinkStagePhase,
        ObjectFilesPhase,
        LinkerPhase
)

internal fun CompilerPhases.konanPhasesConfig(config: KonanConfig) {
    with(config.configuration) {
        disable(EscapeAnalysisPhase)

        // Don't serialize anything to a final executable.
        switch(SerializerPhase, config.produce == CompilerOutputKind.LIBRARY)
        switch(LinkPhase, config.produce.isNativeBinary)
        switch(TestProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE)
    }
}
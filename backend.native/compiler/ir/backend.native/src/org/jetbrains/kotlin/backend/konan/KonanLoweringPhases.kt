package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectDeclarationsRemoving
import org.jetbrains.kotlin.backend.konan.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.lower.LateinitLowering
import org.jetbrains.kotlin.backend.konan.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.konan.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.checkDeclarationParents
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.replaceUnboundSymbols

private fun makeKonanFileLoweringPhase(
        lowering: (Context) -> FileLoweringPass,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = makeIrFilePhase(lowering, name, description, prerequisite)

private fun makeKonanModuleLoweringPhase(
        lowering: (Context) -> FileLoweringPass,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = makeIrModulePhase(lowering, name, description, prerequisite)

internal fun makeKonanFileOpPhase(
        op: (Context, IrFile) -> Unit,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = namedIrFilePhase(
        name, description, prerequisite, nlevels = 0,
        lower = object : SameTypeCompilerPhase<Context, IrFile> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrFile): IrFile {
                op(context, input)
                return input
            }
        }
)

internal fun makeKonanModuleOpPhase(
        op: (Context, IrModuleFragment) -> Unit,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = namedIrModulePhase(
        name, description, prerequisite, nlevels = 0,
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment): IrModuleFragment {
                op(context, input)
                return input
            }
        }
)

internal val RemoveExpectDeclarationsPhase = makeKonanModuleLoweringPhase(
        ::ExpectDeclarationsRemoving,
        name = "RemoveExpectDeclarations",
        description = "Expect declarations removing"
)

internal val TestProcessorPhase = makeKonanModuleOpPhase(
        { context, irModule -> TestProcessor(context).process(irModule) },
        name = "TestProcessor",
        description = "Unit test processor"
)

internal val LowerBeforeInlinePhase = makeKonanModuleLoweringPhase(
        ::PreInlineLowering,
        name = "LowerBeforeInline",
        description = "Special operations processing before inlining"
)

internal val InlinePhase = namedIrModulePhase(
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment): IrModuleFragment {
                FunctionInlining(context).inline(input)
                return input
            }
        },
        name = "Inline",
        description = "Functions inlining",
        prerequisite = setOf(LowerBeforeInlinePhase),
        nlevels = 0
)

internal val LowerAfterInlinePhase = makeKonanModuleOpPhase(
        { context, irModule ->
            irModule.files.forEach(PostInlineLowering(context)::lower)
            // TODO: Seems like this should be deleted in PsiToIR.
            irModule.files.forEach(ContractsDslRemover(context)::lower)
        },
        name = "LowerAfterInline",
        description = "Special operations processing after inlining"
)

internal val InteropPart1Phase = makeKonanModuleLoweringPhase(
        ::InteropLoweringPart1,
        name = "InteropPart1",
        description = "Interop lowering, part 1",
        prerequisite = setOf(InlinePhase)
)

internal val LateinitPhase = makeKonanModuleOpPhase(
        { context, irModule -> irModule.files.forEach(LateinitLowering(context)::lower) },
        name = "Lateinit",
        description = "Lateinit properties lowering",
        prerequisite = setOf(InlinePhase)
)

internal val ReplaceUnboundSymbolsPhase = makeKonanModuleOpPhase(
        { context, irModule ->
            val symbolTable = context.ir.symbols.symbolTable
            do {
                @Suppress("DEPRECATION")
                irModule.replaceUnboundSymbols(context)
            } while (symbolTable.unboundClasses.isNotEmpty())
        },
        name = "ReplaceUnboundSymbols",
        description = "Replace unbound symbols"
)

internal val PatchDeclarationParents1Phase = makeKonanModuleOpPhase(
        { _, irModule -> irModule.patchDeclarationParents() },
        name = "PatchDeclarationParents1",
        description = "Patch declaration parents 1"
)

internal val CheckDeclarationParentsPhase = makeKonanModuleOpPhase(
        { _, irModule -> irModule.checkDeclarationParents() },
        name = "CheckDeclarationParents",
        description = "Check declaration parents"
)

/* IrFile phases */

internal val StringConcatenationPhase = makeKonanFileLoweringPhase(
        ::StringConcatenationLowering,
        name = "StringConcatenation",
        description = "String concatenation lowering"
)

internal val DataClassesPhase = makeKonanFileLoweringPhase(
        ::DataClassOperatorsLowering,
        name = "DataClasses",
        description = "Data classes lowering"
)

internal val ForLoopsPhase = makeKonanFileLoweringPhase(
        ::ForLoopsLowering,
        name = "ForLoops",
        description = "For loops lowering"
)

internal val EnumClassPhase = makeKonanFileOpPhase(
        { context, irFile -> EnumClassLowering(context).run(irFile) },
        name = "Enums",
        description = "Enum classes lowering"
)

internal val PatchDeclarationParents2Phase = makeKonanFileOpPhase(
        { _, irFile ->
            /**
             * TODO:  this is workaround for issue of unitialized parents in IrDeclaration,
             * the last one detected in [EnumClassLowering]. The issue appears in [DefaultArgumentStubGenerator].
             */
            irFile.patchDeclarationParents()
        },
        name = "PatchDeclarationParents2",
        description = "Patch declaration parents 2"
)

internal val InitializersPhase = makeKonanFileLoweringPhase(
        ::InitializersLowering,
        name = "Initializers",
        description = "Initializers lowering",
        prerequisite = setOf(EnumClassPhase)
)

internal val SharedVariablesPhase = makeKonanFileLoweringPhase(
        ::SharedVariablesLowering,
        name = "SharedVariables",
        description = "Shared Variable Lowering",
        prerequisite = setOf(InitializersPhase)
)

internal val DelegationPhase = makeKonanFileLoweringPhase(
        ::PropertyDelegationLowering,
        name = "Delegation",
        description = "Delegation lowering"
)

internal val CallableReferencePhase = makeKonanFileLoweringPhase(
        ::CallableReferenceLowering,
        name = "CallableReference",
        description = "Callable references Lowering",
        prerequisite = setOf(DelegationPhase)
)

internal val PatchDeclarationParents3Phase = makeKonanFileOpPhase(
        { _, irFile ->
            /**
             * TODO:  this is workaround for issue of uninitialized parents in IrDeclaration,
             * the last one detected in [CallableReferenceLowering]. The issue appears in [LocalDeclarationsLowering].
             */
            irFile.patchDeclarationParents()
        },
        name = "PatchdeclarationParents3",
        description = "Patch declaration parents 3"
)

internal val LocalDeclarationsPhase = makeKonanFileOpPhase(
        { context, irFile -> LocalDeclarationsLowering(context).runOnFilePostfix(irFile) },
        name = "LocalDeclarations",
        description = "Local Function Lowering",
        prerequisite = setOf(SharedVariablesPhase, CallableReferencePhase)
)

internal val TailrecPhase = makeKonanFileLoweringPhase(
        ::TailrecLowering,
        name = "Tailrec",
        description = "tailrec lowering",
        prerequisite = setOf(LocalDeclarationsPhase)
)

internal val FinallyBlocksPhase = makeKonanFileLoweringPhase(
        ::FinallyBlocksLowering,
        name = "FinallyBlocks",
        description = "Finally blocks lowering",
        prerequisite = setOf(InitializersPhase, LocalDeclarationsPhase, TailrecPhase)
)

internal val DefaultParameterExtentPhase = makeKonanFileOpPhase(
        { context, irFile ->
            DefaultArgumentStubGenerator(context).runOnFilePostfix(irFile)
            KonanDefaultParameterInjector(context).runOnFilePostfix(irFile)
        },
        name = "DefaultParameterExtent",
        description = "Default Parameter Extent Lowering",
        prerequisite = setOf(TailrecPhase, EnumClassPhase)
)

internal val BuiltinOperatorPhase = makeKonanFileLoweringPhase(
        ::BuiltinOperatorLowering,
        name = "BuiltinOperators",
        description = "BuiltIn Operators Lowering",
        prerequisite = setOf(DefaultParameterExtentPhase)
)

internal val InnerClassPhase = makeKonanFileLoweringPhase(
        ::InnerClassLowering,
        name = "InnerClasses",
        description = "Inner classes lowering",
        prerequisite = setOf(DefaultParameterExtentPhase /*, SyntheticFieldsPhase */ )
)

internal val InteropPart2Phase = makeKonanFileLoweringPhase(
        ::InteropLoweringPart2,
        name = "InteropPart2",
        description = "Interop lowering, part 2",
        prerequisite = setOf(LocalDeclarationsPhase)
)

internal val VarargPhase = makeKonanFileLoweringPhase(
        ::VarargInjectionLowering,
        name = "Vararg",
        description = "Vararg lowering",
        prerequisite = setOf(CallableReferencePhase, DefaultParameterExtentPhase)
)

internal val CompileTimeEvaluatePhase = makeKonanFileLoweringPhase(
        ::CompileTimeEvaluateLowering,
        name = "CompileTimeEvaluate",
        description = "Compile time evaluation lowering",
        prerequisite = setOf(VarargPhase)
)

internal val CoroutinesPhase = makeKonanFileLoweringPhase(
        ::SuspendFunctionsLowering,
        name = "Coroutines",
        description = "Coroutines lowering",
        prerequisite = setOf(LocalDeclarationsPhase)
)

internal val TypeOperatorPhase = makeKonanFileLoweringPhase(
        ::TypeOperatorLowering,
        name = "TypeOperators",
        description = "Type operators lowering",
        prerequisite = setOf(CoroutinesPhase)
)

internal val BridgesPhase = makeKonanFileOpPhase(
        { context, irFile ->
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        },
        name = "Bridges",
        description = "Bridges building",
        prerequisite = setOf(CoroutinesPhase)
)

internal val AutoboxPhase = makeKonanFileOpPhase(
        { context, irFile ->
            // validateIrFile(context, irFile) // Temporarily disabled until moving to new IR finished.
            Autoboxing(context).lower(irFile)
        },
        name = "Autobox",
        description = "Autoboxing of primitive types",
        prerequisite = setOf(BridgesPhase, CoroutinesPhase)
)

internal val ReturnsInsertionPhase = makeKonanFileLoweringPhase(
        ::ReturnsInsertionLowering,
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        prerequisite = setOf(AutoboxPhase, CoroutinesPhase, EnumClassPhase)
)


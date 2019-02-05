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

internal val removeExpectDeclarationsPhase = makeKonanModuleLoweringPhase(
        ::ExpectDeclarationsRemoving,
        name = "RemoveExpectDeclarations",
        description = "Expect declarations removing"
)

internal val testProcessorPhase = makeKonanModuleOpPhase(
        { context, irModule -> TestProcessor(context).process(irModule) },
        name = "TestProcessor",
        description = "Unit test processor"
)

internal val lowerBeforeInlinePhase = makeKonanModuleLoweringPhase(
        ::PreInlineLowering,
        name = "LowerBeforeInline",
        description = "Special operations processing before inlining"
)

internal val inlinePhase = namedIrModulePhase(
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment): IrModuleFragment {
                FunctionInlining(context).inline(input)
                return input
            }
        },
        name = "Inline",
        description = "Functions inlining",
        prerequisite = setOf(lowerBeforeInlinePhase),
        nlevels = 0
)

internal val lowerAfterInlinePhase = makeKonanModuleOpPhase(
        { context, irModule ->
            irModule.files.forEach(PostInlineLowering(context)::lower)
            // TODO: Seems like this should be deleted in PsiToIR.
            irModule.files.forEach(ContractsDslRemover(context)::lower)
        },
        name = "LowerAfterInline",
        description = "Special operations processing after inlining"
)

internal val interopPart1Phase = makeKonanModuleLoweringPhase(
        ::InteropLoweringPart1,
        name = "InteropPart1",
        description = "Interop lowering, part 1",
        prerequisite = setOf(inlinePhase)
)

internal val lateinitPhase = makeKonanModuleOpPhase(
        { context, irModule -> irModule.files.forEach(LateinitLowering(context)::lower) },
        name = "Lateinit",
        description = "Lateinit properties lowering",
        prerequisite = setOf(inlinePhase)
)

internal val replaceUnboundSymbolsPhase = makeKonanModuleOpPhase(
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

internal val patchDeclarationParents1Phase = makeKonanModuleOpPhase(
        { _, irModule -> irModule.patchDeclarationParents() },
        name = "PatchDeclarationParents1",
        description = "Patch declaration parents 1"
)

internal val checkDeclarationParentsPhase = makeKonanModuleOpPhase(
        { _, irModule -> irModule.checkDeclarationParents() },
        name = "CheckDeclarationParents",
        description = "Check declaration parents"
)

/* IrFile phases */

internal val stringConcatenationPhase = makeKonanFileLoweringPhase(
        ::StringConcatenationLowering,
        name = "StringConcatenation",
        description = "String concatenation lowering"
)

internal val dataClassesPhase = makeKonanFileLoweringPhase(
        ::DataClassOperatorsLowering,
        name = "DataClasses",
        description = "Data classes lowering"
)

internal val forLoopsPhase = makeKonanFileLoweringPhase(
        ::ForLoopsLowering,
        name = "ForLoops",
        description = "For loops lowering"
)

internal val enumClassPhase = makeKonanFileOpPhase(
        { context, irFile -> EnumClassLowering(context).run(irFile) },
        name = "Enums",
        description = "Enum classes lowering"
)

internal val patchDeclarationParents2Phase = makeKonanFileOpPhase(
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

internal val initializersPhase = makeKonanFileLoweringPhase(
        ::InitializersLowering,
        name = "Initializers",
        description = "Initializers lowering",
        prerequisite = setOf(enumClassPhase)
)

internal val sharedVariablesPhase = makeKonanFileLoweringPhase(
        ::SharedVariablesLowering,
        name = "SharedVariables",
        description = "Shared Variable Lowering",
        prerequisite = setOf(initializersPhase)
)

internal val delegationPhase = makeKonanFileLoweringPhase(
        ::PropertyDelegationLowering,
        name = "Delegation",
        description = "Delegation lowering"
)

internal val callableReferencePhase = makeKonanFileLoweringPhase(
        ::CallableReferenceLowering,
        name = "CallableReference",
        description = "Callable references Lowering",
        prerequisite = setOf(delegationPhase)
)

internal val patchDeclarationParents3Phase = makeKonanFileOpPhase(
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

internal val localDeclarationsPhase = makeKonanFileOpPhase(
        { context, irFile -> LocalDeclarationsLowering(context).runOnFilePostfix(irFile) },
        name = "LocalDeclarations",
        description = "Local Function Lowering",
        prerequisite = setOf(sharedVariablesPhase, callableReferencePhase)
)

internal val tailrecPhase = makeKonanFileLoweringPhase(
        ::TailrecLowering,
        name = "Tailrec",
        description = "tailrec lowering",
        prerequisite = setOf(localDeclarationsPhase)
)

internal val finallyBlocksPhase = makeKonanFileLoweringPhase(
        ::FinallyBlocksLowering,
        name = "FinallyBlocks",
        description = "Finally blocks lowering",
        prerequisite = setOf(initializersPhase, localDeclarationsPhase, tailrecPhase)
)

internal val defaultParameterExtentPhase = makeKonanFileOpPhase(
        { context, irFile ->
            DefaultArgumentStubGenerator(context).runOnFilePostfix(irFile)
            KonanDefaultParameterInjector(context).runOnFilePostfix(irFile)
        },
        name = "DefaultParameterExtent",
        description = "Default Parameter Extent Lowering",
        prerequisite = setOf(tailrecPhase, enumClassPhase)
)

internal val builtinOperatorPhase = makeKonanFileLoweringPhase(
        ::BuiltinOperatorLowering,
        name = "BuiltinOperators",
        description = "BuiltIn Operators Lowering",
        prerequisite = setOf(defaultParameterExtentPhase)
)

internal val innerClassPhase = makeKonanFileLoweringPhase(
        ::InnerClassLowering,
        name = "InnerClasses",
        description = "Inner classes lowering",
        prerequisite = setOf(defaultParameterExtentPhase /*, SyntheticFieldsPhase */ )
)

internal val interopPart2Phase = makeKonanFileLoweringPhase(
        ::InteropLoweringPart2,
        name = "InteropPart2",
        description = "Interop lowering, part 2",
        prerequisite = setOf(localDeclarationsPhase)
)

internal val varargPhase = makeKonanFileLoweringPhase(
        ::VarargInjectionLowering,
        name = "Vararg",
        description = "Vararg lowering",
        prerequisite = setOf(callableReferencePhase, defaultParameterExtentPhase)
)

internal val compileTimeEvaluatePhase = makeKonanFileLoweringPhase(
        ::CompileTimeEvaluateLowering,
        name = "CompileTimeEvaluate",
        description = "Compile time evaluation lowering",
        prerequisite = setOf(varargPhase)
)

internal val coroutinesPhase = makeKonanFileLoweringPhase(
        ::SuspendFunctionsLowering,
        name = "Coroutines",
        description = "Coroutines lowering",
        prerequisite = setOf(localDeclarationsPhase)
)

internal val typeOperatorPhase = makeKonanFileLoweringPhase(
        ::TypeOperatorLowering,
        name = "TypeOperators",
        description = "Type operators lowering",
        prerequisite = setOf(coroutinesPhase)
)

internal val bridgesPhase = makeKonanFileOpPhase(
        { context, irFile ->
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        },
        name = "Bridges",
        description = "Bridges building",
        prerequisite = setOf(coroutinesPhase)
)

internal val autoboxPhase = makeKonanFileOpPhase(
        { context, irFile ->
            // validateIrFile(context, irFile) // Temporarily disabled until moving to new IR finished.
            Autoboxing(context).lower(irFile)
        },
        name = "Autobox",
        description = "Autoboxing of primitive types",
        prerequisite = setOf(bridgesPhase, coroutinesPhase)
)

internal val returnsInsertionPhase = makeKonanFileLoweringPhase(
        ::ReturnsInsertionLowering,
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase)
)


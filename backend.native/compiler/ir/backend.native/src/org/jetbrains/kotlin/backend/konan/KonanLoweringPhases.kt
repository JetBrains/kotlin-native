package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectDeclarationsRemoving
import org.jetbrains.kotlin.backend.konan.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.lower.LateinitLowering
import org.jetbrains.kotlin.backend.konan.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.konan.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.checkDeclarationParents
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.replaceUnboundSymbols

internal fun <Data : IrElement> makeKonanPhase(
        op: (Context, Data) -> Unit,
        description: String,
        name: String,
        prerequisite: Set<CompilerPhase<Context, *, *>> = emptySet()
) = makeIrPhase<Context, Data>(
        { context, data ->
            op(context, data)
            data
        },
        description,
        name,
        prerequisite,
        { name }
)

private fun makeKonanFilePhase(
        lowering: (Context) -> FileLoweringPass,
        description: String,
        name: String,
        prerequisite: Set<CompilerPhase<Context, *, *>> = emptySet()
) = makeIrFilePhase(lowering, description, name, prerequisite)

private fun makeKonanModulePhase(
        lowering: (Context) -> FileLoweringPass,
        description: String,
        name: String,
        prerequisite: Set<CompilerPhase<Context, *, *>> = emptySet()
) = org.jetbrains.kotlin.backend.common.makeIrModulePhase(lowering, description, name, prerequisite)

internal val RemoveExpectDeclarationsPhase = makeKonanModulePhase(
        ::ExpectDeclarationsRemoving,
        name = "RemoveExpectDeclarations",
        description = "Expect declarations removing"
)

internal val TestProcessorPhase = makeKonanPhase<IrModuleFragment>(
        { context, irModule -> TestProcessor(context).process(irModule) },
        name = "TestProcessor",
        description = "Unit test processor"
)

internal val LowerBeforeInlinePhase = makeKonanModulePhase(
        ::PreInlineLowering,
        name = "LowerBeforeInline",
        description = "Special operations processing before inlining"
)

internal val InlinePhase = object : AbstractIrCompilerPhase<Context, IrModuleFragment>(
        getName = { name.asString() }
) {
    override val name = "Inline"
    override val description = "Functions inlining"
    override val prerequisite = setOf(LowerBeforeInlinePhase)

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment): IrModuleFragment {
        FunctionInlining(context, phaserState).inline(input)
        return input
    }
}

internal val LowerAfterInlinePhase = makeKonanPhase<IrModuleFragment>(
        { context, irModule ->
            irModule.files.forEach(PostInlineLowering(context)::lower)
            // TODO: Seems like this should be deleted in PsiToIR.
            irModule.files.forEach(ContractsDslRemover(context)::lower)
        },
        name = "LowerAfterInline",
        description = "Special operations processing after inlining"
)

internal val InteropPart1Phase = makeKonanModulePhase(
        ::InteropLoweringPart1,
        name = "InteropPart1",
        description = "Interop lowering, part 1",
        prerequisite = setOf(InlinePhase)
)

internal val LateinitPhase = makeKonanPhase<IrModuleFragment>(
        { context, irModule -> irModule.files.forEach(LateinitLowering(context)::lower) },
        name = "Lateinit",
        description = "Lateinit properties lowering",
        prerequisite = setOf(InlinePhase)
)

internal val ReplaceUnboundSymbolsPhase = makeKonanPhase<IrModuleFragment>(
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

internal val PatchDeclarationParents1Phase = makeKonanPhase<IrModuleFragment>(
        { _, irModule -> irModule.patchDeclarationParents() },
        name = "PatchDeclarationParents1",
        description = "Patch declaration parents 1"
)



internal val CheckDeclarationParentsPhase = makeKonanPhase<IrModuleFragment>(
        { _, irModule -> irModule.checkDeclarationParents() },
        name = "CheckDeclarationParents",
        description = "Check declaration parents"
)

/* IrFile phases */

internal val StringConcatenationPhase = makeKonanFilePhase(
        ::StringConcatenationLowering,
        name = "StringConcatenation",
        description = "String concatenation lowering"
)

internal val DataClassesPhase = makeKonanFilePhase(
        ::DataClassOperatorsLowering,
        name = "DataClasses",
        description = "Data classes lowering"
)

internal val ForLoopsPhase = makeKonanFilePhase(
        ::ForLoopsLowering,
        name = "ForLoops",
        description = "For loops lowering"
)

internal val EnumClassPhase = makeKonanPhase<IrFile>(
        { context, irFile -> EnumClassLowering(context).run(irFile) },
        name = "Enums",
        description = "Enum classes lowering"
)

internal val PatchDeclarationParents2Phase = makeKonanPhase<IrFile>(
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

internal val InitializersPhase = makeKonanFilePhase(
        ::InitializersLowering,
        name = "Initializers",
        description = "Initializers lowering",
        prerequisite = setOf(EnumClassPhase)
)

internal val SharedVariablesPhase = makeKonanFilePhase(
        ::SharedVariablesLowering,
        name = "Sharedvariables",
        description = "Shared Variable Lowering",
        prerequisite = setOf(InitializersPhase)
)

internal val DelegationPhase = makeKonanFilePhase(
        ::PropertyDelegationLowering,
        name = "Delegation",
        description = "Delegation lowering"
)

internal val CallableReferencePhase = makeKonanFilePhase(
        ::CallableReferenceLowering,
        name = "CallableRefeence",
        description = "Callable references Lowering",
        prerequisite = setOf(DelegationPhase)
)

internal val PatchDeclarationParents3Phase = makeKonanPhase<IrFile>(
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

internal val LocalDeclarationsPhase = makeKonanPhase<IrFile>(
        { context, irFile -> LocalDeclarationsLowering(context).runOnFilePostfix(irFile) },
        name = "LocalDeclarations",
        description = "Local Function Lowering",
        prerequisite = setOf(SharedVariablesPhase, CallableReferencePhase)
)

internal val TailrecPhase = makeKonanFilePhase(
        ::TailrecLowering,
        name = "Tailrec",
        description = "tailrec lowering",
        prerequisite = setOf(LocalDeclarationsPhase)
)

internal val FinallyBlocksPhase = makeKonanFilePhase(
        ::FinallyBlocksLowering,
        name = "FinallyBlocks",
        description = "Finally blocks lowering",
        prerequisite = setOf(InitializersPhase, LocalDeclarationsPhase, TailrecPhase)
)

internal val DefaultParameterExtentPhase = makeKonanPhase<IrFile>(
        { context, irFile ->
            DefaultArgumentStubGenerator(context).runOnFilePostfix(irFile)
            KonanDefaultParameterInjector(context).runOnFilePostfix(irFile)
        },
        name = "DefaultParameterExtent",
        description = "Default Parameter Extent Lowering",
        prerequisite = setOf(TailrecPhase, EnumClassPhase)
)

internal val BuiltinOperatorPhase = makeKonanFilePhase(
        ::BuiltinOperatorLowering,
        name = "BuiltinOperators",
        description = "BuiltIn Operators Lowering",
        prerequisite = setOf(DefaultParameterExtentPhase)
)

internal val InnerClassPhase = makeKonanFilePhase(
        ::InnerClassLowering,
        name = "InnerClasses",
        description = "Inner classes lowering",
        prerequisite = setOf(DefaultParameterExtentPhase /*, SyntheticFieldsPhase */ )
)

internal val InteropPart2Phase = makeKonanFilePhase(
        ::InteropLoweringPart2,
        name = "InteropPart2",
        description = "Interop lowering, part 2",
        prerequisite = setOf(LocalDeclarationsPhase)
)

internal val VarargPhase = makeKonanFilePhase(
        ::VarargInjectionLowering,
        name = "Vararg",
        description = "Vararg lowering",
        prerequisite = setOf(CallableReferencePhase, DefaultParameterExtentPhase)
)

internal val CompileTimeEvaluatePhase = makeKonanFilePhase(
        ::CompileTimeEvaluateLowering,
        name = "CompileTimeEvaluate",
        description = "Compile time evaluation lowering",
        prerequisite = setOf(VarargPhase)
)

internal val CoroutinesPhase = makeKonanFilePhase(
        ::SuspendFunctionsLowering,
        name = "Coroutines",
        description = "Coroutines lowering",
        prerequisite = setOf(LocalDeclarationsPhase)
)

internal val TypeOperatorPhase = makeKonanFilePhase(
        ::TypeOperatorLowering,
        name = "TypeOperators",
        description = "Type operators lowering",
        prerequisite = setOf(CoroutinesPhase)
)

internal val BridgesPhase = makeKonanPhase<IrFile>(
        { context, irFile ->
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        },
        name = "Bridges",
        description = "Bridges building",
        prerequisite = setOf(CoroutinesPhase)
)

internal val AutoboxPhase = makeKonanPhase<IrFile>(
        { context, irFile ->
            // validateIrFile(context, irFile) // Temporarily disabled until moving to new IR finished.
            Autoboxing(context).lower(irFile)
        },
        name = "Autobox",
        description = "Autoboxing of primitive types",
        prerequisite = setOf(BridgesPhase, CoroutinesPhase)
)

internal val ReturnsInsertionPhase = makeKonanFilePhase(
        ::ReturnsInsertionLowering,
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        prerequisite = setOf(AutoboxPhase, CoroutinesPhase, EnumClassPhase)
)


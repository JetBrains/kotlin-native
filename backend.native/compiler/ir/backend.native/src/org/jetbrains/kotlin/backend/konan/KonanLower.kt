/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectDeclarationsRemoving
import org.jetbrains.kotlin.backend.konan.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.lower.LateinitLowering
import org.jetbrains.kotlin.backend.konan.lower.SharedVariablesLowering
import org.jetbrains.kotlin.backend.konan.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.CheckDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.util.checkDeclarationParents
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.replaceUnboundSymbols

internal class KonanLower(val context: Context, val parentPhaser: PhaseManager) {

    fun lower() {
        val irModule = context.irModule!!

        // Phases to run against whole module.
        lowerModule(irModule, parentPhaser)

        // Phases to run against a file.
        irModule.files.forEach {
            lowerFile(it, PhaseManager(context, parentPhaser))
        }

        irModule.checkDeclarationParents()
    }

    private fun lowerModule(irModule: IrModuleFragment, phaser: PhaseManager) {
        phaser.phase(KonanPhase.REMOVE_EXPECT_DECLARATIONS) {
            irModule.files.forEach(ExpectDeclarationsRemoving(context)::lower)
        }

        phaser.phase(KonanPhase.TEST_PROCESSOR) {
            TestProcessor(context).process(irModule)
        }

        phaser.phase(KonanPhase.LOWER_BEFORE_INLINE) {
            irModule.files.forEach(PreInlineLowering(context)::lower)
        }

        // Inlining must be run before other phases.
        phaser.phase(KonanPhase.LOWER_INLINE) {
            FunctionInlining(context).inline(irModule)
        }

        phaser.phase(KonanPhase.LOWER_AFTER_INLINE) {
            irModule.files.forEach(PostInlineLowering(context)::lower)
            // TODO: Seems like this should be deleted in PsiToIR.
            irModule.files.forEach(ContractsDslRemover(context)::lower)
        }

        phaser.phase(KonanPhase.LOWER_INTEROP_PART1) {
            irModule.files.forEach(InteropLoweringPart1(context)::lower)
        }

        phaser.phase(KonanPhase.LOWER_LATEINIT) {
            irModule.files.forEach(LateinitLowering(context)::lower)
        }

        val symbolTable = context.ir.symbols.symbolTable

        do {
            @Suppress("DEPRECATION")
            irModule.replaceUnboundSymbols(context)
        } while (symbolTable.unboundClasses.isNotEmpty())

        irModule.patchDeclarationParents()

//        validateIrModule(context, irModule) // Temporarily disabled until moving to new IR finished.
    }

    private fun lowerFile(irFile: IrFile, phaser: PhaseManager) {
        phaser.phase(KonanPhase.LOWER_STRING_CONCAT, irFile) {
            StringConcatenationLowering(context).lower(irFile)
        }

        phaser.phase(KonanPhase.LOWER_DATA_CLASSES, irFile) {
            DataClassOperatorsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_FOR_LOOPS, irFile) {
            ForLoopsLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_ENUMS, irFile) {
            EnumClassLowering(context).run(irFile)
        }
        irFile.patchDeclarationParents() /* TODO: fix .parent on in [EnumClassLowering] */

        phaser.phase(KonanPhase.LOWER_INITIALIZERS, irFile) {
            InitializersLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_SHARED_VARIABLES, irFile) {
            SharedVariablesLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DELEGATION, irFile) {
            PropertyDelegationLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_CALLABLES, irFile) {
            CallableReferenceLowering(context).lower(irFile)
        }

        phaser.phase(KonanPhase.LOWER_LOCAL_FUNCTIONS, irFile) {
            LocalDeclarationsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_TAILREC, irFile) {
            TailrecLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_FINALLY, irFile) {
            FinallyBlocksLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DEFAULT_PARAMETER_EXTENT, irFile) {
            DefaultArgumentStubGenerator(context).runOnFilePostfix(irFile)
            KonanDefaultParameterInjector(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_BUILTIN_OPERATORS, irFile) {
            BuiltinOperatorLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INNER_CLASSES, irFile) {
            InnerClassLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INTEROP_PART2, irFile) {
            InteropLoweringPart2(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_VARARG, irFile) {
            VarargInjectionLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_COMPILE_TIME_EVAL, irFile) {
            CompileTimeEvaluateLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_COROUTINES, irFile) {
            SuspendFunctionsLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_TYPE_OPERATORS, irFile) {
            TypeOperatorLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.BRIDGES_BUILDING, irFile) {
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        }
        phaser.phase(KonanPhase.AUTOBOX, irFile) {
            // validateIrFile(context, irFile) // Temporarily disabled until moving to new IR finished.
            Autoboxing(context).lower(irFile)
        }
        phaser.phase(KonanPhase.RETURNS_INSERTION, irFile) {
            ReturnsInsertionLowering(context).lower(irFile)
        }
    }

    private fun PhaseManager.phase(phase: KonanPhase, file: IrFile, function: () -> Unit) {
        if (context.config.configuration.getBoolean(KonanConfigKeys.CHECK_IR_PARENTS)) file.accept(CheckDeclarationParentsVisitor, null)
        phase(phase, function)
    }
}

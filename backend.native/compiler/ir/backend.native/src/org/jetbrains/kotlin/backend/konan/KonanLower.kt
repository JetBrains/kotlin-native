package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.ir.declarations.IrFile

internal class KonanLower(val context: Context) {

    fun lower() {
        context.irModule!!.files.forEach {
            lower(it)
        }
    }

    fun lower(irFile: IrFile) {
        val phaser = PhaseManager(context)

        phaser.phase(KonanPhase.LOWER_IR_CORRECTOR) {
            FakeCallableDescriptorReplacer().runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INLINE) {
            FunctionInlining(context).inline(irFile)
        }
        phaser.phase(KonanPhase.LOWER_STRING_CONCAT) {
            StringConcatenationLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_ENUMS) {
            EnumClassLowering(context).run(irFile)
        }
        phaser.phase(KonanPhase.LOWER_SHARED_VARIABLES) {
            SharedVariablesLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INITIALIZERS) {
            InitializersLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DELEGATION) {
            PropertyDelegationLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_TYPE_OPERATORS) {
            TypeOperatorLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_LOCAL_FUNCTIONS) {
            LocalDeclarationsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_TAILREC) {
            TailrecLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_DEFAULT_PARAMETER_EXTENT) {
            DefaultArgumentStubGenerator(context).runOnFilePostfix(irFile)
            DefaultParameterInjector(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_BUILTIN_OPERATORS) {
            BuiltinOperatorLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INNER_CLASSES) {
            InnerClassLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_INTEROP) {
            InteropLowering(context).lower(irFile)
        }
        phaser.phase(KonanPhase.LOWER_CALLABLES) {
            CallableReferenceLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_VARARG) {
            VarargInjectionLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.BRIDGES_BUILDING) {
            BridgesBuilding(context).runOnFilePostfix(irFile)
            DirectBridgesCallsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.AUTOBOX) {
            Autoboxing(context).lower(irFile)
        }
    }
}

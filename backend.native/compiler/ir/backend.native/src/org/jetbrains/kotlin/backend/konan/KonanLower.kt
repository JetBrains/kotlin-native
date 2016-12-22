package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class KonanLower(val context: Context) {

    fun lower() {
        context.irModule!!.files.forEach {
            lower(it)
        }
    }

    fun lower(irFile: IrFile) {
        val phaser = PhaseManager(context)

        phaser.phase(KonanPhase.LOWER_BUILTIN_OPERATORS) {
            BuiltinOperatorLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_SHARED_VARIABLES) {
            SharedVariablesLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_LOCAL_FUNCTIONS) {
            LocalFunctionsLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.LOWER_CALLABLES) {
            CallableReferenceLowering(context).runOnFilePostfix(irFile)
        }
        phaser.phase(KonanPhase.AUTOBOX) {
            Autoboxing(context).lower(irFile)
        }
    }
}

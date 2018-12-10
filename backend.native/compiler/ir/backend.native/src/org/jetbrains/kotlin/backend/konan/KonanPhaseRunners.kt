package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name

internal object KonanIrModulePhaseRunner : DefaultIrPhaseRunner<Context, IrModuleFragment>() {
    override fun phases(context: Context) = context.phases
    override fun elementName(input: IrModuleFragment) = input.name.asString()
    override fun configuration(context: Context) = context.config.configuration
}

internal object KonanIrFilePhaseRunner : DefaultIrPhaseRunner<Context, IrFile>() {
    override fun phases(context: Context) = context.phases
    override fun elementName(input: IrFile) = input.name
    override fun configuration(context: Context) = context.config.configuration
}

internal object KonanUnitPhaseRunner : DefaultPhaseRunner<Context, Unit>() {
    override fun dumpElement(input: Unit, phase: CompilerPhase<Context, Unit>, context: Context, beforeOrAfter: BeforeOrAfter) {
        println("Nothing to dump for ${phase.name}")
    }

    override fun phases(context: Context) = context.phases
    override fun elementName(input: Unit) = ""
    override fun configuration(context: Context) = context.config.configuration
}

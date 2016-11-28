package org.jetbrains.kotlin.backend.konan.optimizer

import org.jetbrains.kotlin.ir.declarations.*

fun optimizeIR(module: IrModuleFragment) {
    specializer(module)
}



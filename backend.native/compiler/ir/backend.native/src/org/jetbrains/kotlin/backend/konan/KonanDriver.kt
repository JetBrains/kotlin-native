/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CompilerPhaseManager
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.runPhases
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.ModuleIndex
import org.jetbrains.kotlin.backend.konan.llvm.emitLLVM
import org.jetbrains.kotlin.backend.konan.serialization.KonanSerializationUtil
import org.jetbrains.kotlin.backend.konan.serialization.markBackingFields
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

fun runTopLevelPhases(konanConfig: KonanConfig, environment: KotlinCoreEnvironment) {

    val config = konanConfig.configuration

    val targets = konanConfig.targetManager
    if (config.get(KonanConfigKeys.LIST_TARGETS) ?: false) {
        targets.list()
    }

    val context = Context(konanConfig)
    context.environment = environment
    context.phases.konanPhasesConfig(konanConfig) // TODO: Wrong place to call it

    if (config.get(KonanConfigKeys.LIST_PHASES) ?: false) {
        context.phases.list()
    }

    if (konanConfig.infoArgsOnly) return

    CompilerPhaseManager(context, context.phases, Unit, KonanUnitPhaseRunner).runPhases(toplevelPhaseList)
}


package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.util.*

enum class KonanPhase(val description: String,
                      val prerequisite: Set<KonanPhase> = setOf<KonanPhase>(),
                      var enabled: Boolean = true, var verbose: Boolean = false) {

    /* */ FRONTEND("Frontend builds AST"),
    /* */ PSI_TO_IR("Psi to IR conversion"),
    /* */ BACKEND("All backend"),
    /* ... */ LOWER("IR Lowering"),
    /* ... ... */ LOWER_INLINE("Functions inlining"),
    /* ... ... */ LOWER_INTEROP("Interop lowering"),
    /* ... ... */ LOWER_SHARED_VARIABLES("Shared Variable Lowering"),
    /* ... ... */ LOWER_ENUMS("Enum classes lowering"),
    /* ... ... */ LOWER_DELEGATION("Delegation lowering"),
    /* ... ... */ LOWER_INITIALIZERS("Initializers lowering", setOf(LOWER_ENUMS)),
    /* ... ... */ LOWER_CALLABLES("Callable references Lowering", setOf(
                        LOWER_INTEROP, LOWER_INITIALIZERS, LOWER_DELEGATION)),
    /* ... ... */ LOWER_VARARG("Vararg lowering", setOf(LOWER_CALLABLES)),
    /* ... ... */ LOWER_LOCAL_FUNCTIONS("Local Function Lowering", setOf(LOWER_INITIALIZERS)),
    /* ... ... */ LOWER_TAILREC("tailrec lowering", setOf(LOWER_LOCAL_FUNCTIONS)),
    /* ... ... */ LOWER_DEFAULT_PARAMETER_EXTENT("Default Parameter Extent Lowering", setOf(
                        LOWER_TAILREC, LOWER_ENUMS)),
    /* ... ... */ LOWER_INNER_CLASSES("Inner classes lowering", setOf(LOWER_DEFAULT_PARAMETER_EXTENT)),
    /* ... ... */ LOWER_BUILTIN_OPERATORS("BuiltIn Operators Lowering", setOf(
                        LOWER_DEFAULT_PARAMETER_EXTENT)),
    /* ... ... */ LOWER_TYPE_OPERATORS("Type operators lowering"),
    /* ... ... */ BRIDGES_BUILDING("Bridges building"),
    /* ... ... */ LOWER_STRING_CONCAT("String concatenation lowering"),
    /* ... ... */ AUTOBOX("Autoboxing of primitive types", setOf(BRIDGES_BUILDING)),
    /* ... */ BITCODE("LLVM BitCode Generation"),
    /* ... ... */ RTTI("RTTI Generation"),
    /* ... ... */ CODEGEN("Code Generation"),
    /* ... ... */ METADATOR("Metadata Generation"),
    /* */ LINKER("Link Stage")
}

object KonanPhases {
    val phases = KonanPhase.values().associate { it.name.toLowerCase() to it }

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use -list to see the list of phases.")
        }
        return name
    }

    fun config(config: KonanConfig) {
        with (config.configuration) { with (KonanConfigKeys) { 
            val disabled = get(DISABLED_PHASES)
            disabled?.forEach { phases[known(it)]!!.enabled = false }

            val enabled = get(ENABLED_PHASES)
            enabled?.forEach { phases[known(it)]!!.enabled = true }

            val verbose = get(VERBOSE_PHASES)
            verbose?.forEach { phases[known(it)]!!.verbose = true }

            if (get(NOLINK) ?: false ) {
                KonanPhase.LINKER.enabled = false
            }
        }}
    }

    fun list() {
        phases.forEach { key, phase ->
            val enabled = if (phase.enabled) "(Enabled)" else ""
            val verbose = if (phase.verbose) "(Verbose)" else ""
            
            println(String.format("%1$-30s%2$-30s%3$-10s", "${key}:", phase.description, "$enabled $verbose"))
        }
    }
}

internal class PhaseManager(val context: Context)  {

    val previousPhases = mutableSetOf<KonanPhase>()

    internal fun phase(phase: KonanPhase, body: () -> Unit) {

        if (!phase.enabled) return

        phase.prerequisite.forEach {
            if (!previousPhases.contains(it))
                throw Error("$phase requires $it")
        }

        previousPhases.add(phase)

        val savePhase = context.phase
        context.phase = phase
        context.depth ++

        with (context) {
            profileIf(shouldProfilePhases(), "Phase ${nTabs(depth)} ${phase.name}") {
                body() 
            }

            if (shouldVerifyDescriptors()) {
                verifyDescriptors()
            }
            if (shouldVerifyIr()) {
                verifyIr()
            }

            if (shouldPrintDescriptors()) {
                printDescriptors()
            }
            if (shouldPrintIr()) {
                printIr()
            }
        }

        context.depth --
        context.phase = savePhase
    }
}



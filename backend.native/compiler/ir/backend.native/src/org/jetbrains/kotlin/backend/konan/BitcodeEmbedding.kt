package org.jetbrains.kotlin.backend.konan


import org.jetbrains.kotlin.backend.konan.BitcodeEmbedding.bitcodeEmbeddingMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

object BitcodeEmbedding {

    enum class Mode {
        NONE, MARKER, FULL
    }

    internal fun getLinkerOptions(config: KonanConfig): List<String> = when (config.bitcodeEmbeddingMode) {
        Mode.NONE -> emptyList()
        Mode.MARKER -> listOf("-bitcode_bundle", "-bitcode_process_mode", "marker")
        Mode.FULL -> listOf("-bitcode_bundle")
    }

    internal fun getClangOptions(config: KonanConfig): List<String> = when (config.bitcodeEmbeddingMode) {
        Mode.NONE -> listOf("-fembed-bitcode=off")
        Mode.MARKER -> listOf("-fembed-bitcode=marker")
        Mode.FULL -> listOf("-fembed-bitcode=all")
    }

    // TODO: We cannot produce programs due to `-alias` linked flag.
    private fun KonanConfig.shouldForceBitcodeEmbedding() =
            target.isTvOs && produce != CompilerOutputKind.PROGRAM

    private val KonanConfig.bitcodeEmbeddingMode: Mode
        get() = when {
            shouldForceBitcodeEmbedding() -> if (debug) Mode.MARKER else Mode.FULL
            else -> configuration.get(KonanConfigKeys.BITCODE_EMBEDDING_MODE)!!
        }.also {
            require(it == Mode.NONE || this.produce == CompilerOutputKind.FRAMEWORK) {
                "${it.name.toLowerCase()} bitcode embedding mode is not supported when producing ${this.produce.name.toLowerCase()}"
            }
        }
}
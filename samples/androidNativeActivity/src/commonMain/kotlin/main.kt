package sample.androidnative

import kotlinx.cinterop.*
import platform.android.*

fun main(args: Array<String>) {
    logInfo("Entering main().")
    memScoped {
        val state = alloc<NativeActivityState>()
        getNativeActivityState(state.ptr)
        val engine = Engine(state)
        try {
            engine.mainLoop()
        } finally {
            engine.dispose()
        }
    }
    kotlin.system.exitProcess(0)
}

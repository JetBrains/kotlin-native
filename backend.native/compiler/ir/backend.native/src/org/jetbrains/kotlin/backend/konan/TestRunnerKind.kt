package org.jetbrains.kotlin.backend.konan

enum class TestRunnerKind(val option_name: String) {
    NONE("none"),
    MAIN_THREAD("main_thread"),
    WORKER("worker")
}
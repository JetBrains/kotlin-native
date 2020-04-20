package samples

import samples.cfg.*

val callees = mapOf(
        "classes" to { runClasses() },
        "lib" to { runLib() },
        "nested" to { runNested() },
        "receiver" to { runReceiver() },

        "cfg_when" to { cfgWhen() },
        "cfg_return" to { cfgReturn() },
        "cfg_loop" to { cfgLoop() },
        "cfg_classes" to { cfgClasses() },
        "cfg_dup" to { cfgDup(42, 31) },
        "cfg_dup2" to { cfgDup2(42, 31) }
)

fun main() {
    callees.forEach {
        print("${it.key}: ")
        it.value.invoke()
    }
}
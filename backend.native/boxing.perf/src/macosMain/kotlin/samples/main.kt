package samples

import samples.cfg.*

val callees = mapOf(
        "classes" to { runClasses() },
        "lib" to { runLib() },
        "nested" to { runNested() },
        "receiver" to { runReceiver() },

        "cfg_when" to { cfgWhen() }
)

fun main() {
    callees.forEach {
        print("${it.key}: ")
        it.value.invoke()
    }
}
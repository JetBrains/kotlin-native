package samples

val callees = mapOf(
        "lib" to { runLib() },
        "nested" to { runNested() },
        "receiver" to { runReceiver() }
)

fun main() {
    callees.forEach {
        print("${it.key}: ")
        it.value.invoke()
    }
}
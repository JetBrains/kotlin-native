
var previousResult = 0

val callees = mapOf<String, () -> Int>(
        "lib" to { lib.runCount() },
        "nested" to { nested.runCount() }
)

fun main(args: Array<String>) {
    listOf("lib", "nested").forEach(::showInfo)
}

fun showInfo(name: String) {
    println("\nname: $name")
    println("stdout:")
    val callee = callees[name] ?: return
    val result: Int = callee()
    println("----------")
    println("boxings: ${result - previousResult}")
    previousResult = result
}
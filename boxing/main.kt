
val callees = mapOf<String, () -> Int>(
        "lib" to { lib.runCount() }
)

fun main(args: Array<String>) {
    listOf("lib").forEach(::showInfo)
}

fun showInfo(name: String) {
    println("\nname: $name")
    println("stdout:")
    val callee = callees[name] ?: return
    val result: Int = callee()
    println("----------")
    println("boxings: ${result - 2}")
}
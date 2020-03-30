package library

interface I {
    val data: String
}

class A(override val data: String): I

enum class E(val data: String) {
    A("Enum entry A"),
    B("Enum entry B"),
    C("Enum entry C")
}
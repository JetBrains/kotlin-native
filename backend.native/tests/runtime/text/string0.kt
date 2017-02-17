public fun String.capitalize2(): String {
    return if (isNotEmpty() && this[0].isLowerCase())
        substring(0, 1).toUpperCase().plus2(substring(1)) else this
}

private val kNullString = "<null>"

public fun kotlin.String?.plus2(other: kotlin.Any?): kotlin.String =
        this?.plus(other?.toString() ?: kNullString) ?: other?.toString2() ?: kNullString


public fun Any?.toString2() = this?.toString() ?: kNullString

fun main(args: Array<String>) {
    val str = "hello"
    println(str.equals("HElLo", true))
    val strI18n = "Привет"
    println(strI18n.equals("прИВет", true))
    println(strI18n.toUpperCase())
    println(strI18n.toLowerCase())
    println("пока".capitalize())
    println("http://jetbrains.com".startsWith("http://"))
}
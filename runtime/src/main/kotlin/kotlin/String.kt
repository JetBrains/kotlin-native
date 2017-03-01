package kotlin

// TODO: in big Kotlin this operations are in kotlin.kotlin_builtins.
private val kNullString = "null"

public operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String =
    this?.plus(other?.toString() ?: kNullString) ?: other?.toString() ?: kNullString


public fun Any?.toString() = this?.toString() ?: kNullString

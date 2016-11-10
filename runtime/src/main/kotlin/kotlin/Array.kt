package kotlin

class Array<T> : Cloneable {
    // Constructors are handled with compiler magic.
    private constructor() {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_Array_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_Array_get")
    external public operator fun get(index: Int): T

    @SymbolName("Kotlin_Array_set")
    external public operator fun set(index: Int, value: T): Unit

    @SymbolName("Kotlin_Array_getArrayLength")
    external private fun getArrayLength(): Int
}
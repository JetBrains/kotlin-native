package kotlin_native

public interface Cloneable {
    public fun clone(): Any
}

class ByteArray(size: Int) : Cloneable {
    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_ByteArray_get")
    external public operator fun get(index: Int): Byte

    @SymbolName("Kotlin_ByteArray_set")
    external public operator fun set(index: Int, value: Byte): Unit

    @SymbolName("Kotlin_ByteArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    external private fun getArrayLength(): Int
}

class CharArray(size: Int) : Cloneable {
    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_CharArray_get")
    external public operator fun get(index: Int): Char

    @SymbolName("Kotlin_CharArray_set")
    external public operator fun set(index: Int, value: Char): Unit

    @SymbolName("Kotlin_CharArray_clone")
    external public override fun clone(): CharArray

    @SymbolName("Kotlin_CharArray_getArrayLength")
    external private fun getArrayLength(): Int
}

class IntArray(size: Int) : Cloneable {
    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_IntArray_get")
    external public operator fun get(index: Int): Char

    @SymbolName("Kotlin_IntArray_set")
    external public operator fun set(index: Int, value: Char): Unit

    @SymbolName("Kotlin_IntArray_clone")
    external public override fun clone(): CharArray

    @SymbolName("Kotlin_IntArray_getArrayLength")
    external private fun getArrayLength(): Int
}


class String {
    companion object {
        external fun fromUtf8Array(array: ByteArray) : String
    }
    external public operator fun plus(other: Any?): String

    public val length: Int
        get() = getStringLength()

    // Can be O(N).
    external public fun get(index: Int): Char

    // external public fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    external public fun compareTo(other: String): Int

    external private fun getStringLength(): Int
}
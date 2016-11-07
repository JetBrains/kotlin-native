package kotlin

@SymbolName("Kotlin_String_fromUtf8Array")
external fun fromUtf8Array(array: ByteArray) : String

@ExportTypeInfo("theStringTypeInfo")
class String : Comparable<String>/* , CharSequence */ {
    @SymbolName("Kotlin_String_hashCode")
    external public override fun hashCode(): Int

/* TODO: calling to virtual method (plusImpl) results in link error; uncomment after supporting
calling virtual methods in translator */
    public operator fun plus(other: String): String {
        return plusImpl(other.toString())
    }

    public /* override */ val length: Int
        get() = getStringLength()

    // Can be O(N).
    @SymbolName("Kotlin_String_get")
    external /* override */ public fun get(index: Int): Char

    @SymbolName("Kotlin_String_subSequence")
    external /* override */ public fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @SymbolName("Kotlin_String_compareTo")
    override external public fun compareTo(other: String): Int

    @SymbolName("Kotlin_String_getStringLength")
    external private fun getStringLength(): Int

    @SymbolName("Kotlin_String_plusImpl")
    external private fun plusImpl(other:Any): String

    @SymbolName("Kotlin_String_equals")
    external public override fun equals(other: Any?): Boolean
}
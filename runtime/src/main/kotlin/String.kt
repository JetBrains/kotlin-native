package native.kotlin

interface ConstantByteArray {
    public val length : Int
    public operator fun get(index: Int): Byte
}

class CompilerLiteral(val ptr : Long, val size : Int) : ConstantByteArray {
    override public val length : Int
        get() = this.size

    external override public operator fun get(index: Int): Byte
}

abstract class KString {
    abstract public operator fun plus(other: Any?): KString

    abstract public val length: Int

    // Can be O(N) in subclasses for UTF-8 strings.
    abstract public fun get(index: Int): Char

    // public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence
    external public fun compareTo(other: KString): Int
}

// TODO: sealed?
class Latin1KString : KString {
    private constructor(data: ConstantByteArray) { this.data = data }

    private val data : ConstantByteArray

    public external override operator fun plus(other: Any?): KString

    public override val length: Int
        get() = this.data.length

    public override fun get(index: Int): Char {
        return data[index].toChar()
    }
}

class Utf8KString : KString {
    private constructor(data : ConstantByteArray) { this.data = data }

    private val data : ConstantByteArray

    public external override operator fun plus(other: Any?): KString

    public override val length: Int
        get() = getStringLength()

    external public override fun get(index: Int): Char

    external private fun getStringLength(): Int

}
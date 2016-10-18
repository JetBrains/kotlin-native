package kotlin_native

interface ConstantByteArray {
    public val length : Int
    public operator fun get(index: Int): Byte
}

class CompilerLiteral(val ptr : Long, val size : Int) : ConstantByteArray {
    override public val length : Int
        get() = this.size

    external override public operator fun get(index: Int): Byte
}

class RuntimeData : ConstantByteArray {
    private val data : ByteArray

    constructor(size : Int) {
        data = ByteArray(size)
    }

    override public val length : Int
        get() = data.size


    override public operator fun get(index: Int): Byte {
        return this.data[index]
    }
}

abstract class String {
    abstract public operator fun plus(other: Any?): String

    abstract public val length: Int

    // Can be O(N) in subclasses for UTF-8 strings.
    abstract public fun get(index: Int): Char

    // public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence
    external public fun compareTo(other: String): Int
}

// TODO: sealed?
class Latin1String : String {
    private constructor(data: ConstantByteArray) { this.data = data }

    private val data : ConstantByteArray

    public external override operator fun plus(other: Any?): String

    public override val length: Int
        get() = this.data.length

    public override fun get(index: Int): Char {
        return data[index].toChar()
    }
}

class Utf8String : String {
    private constructor(data : ConstantByteArray) { this.data = data }

    private val data : ConstantByteArray

    public external override operator fun plus(other: Any?): Utf8String

    public override val length: Int
        get() = getStringLength()

    // Both those are O(N)!
    external public override fun get(index: Int): Char
    external private fun getStringLength(): Int

}
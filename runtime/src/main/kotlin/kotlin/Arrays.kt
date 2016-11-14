package kotlin

import kotlin.collections.*

/**
 * An array of bytes.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theByteArrayTypeInfo")
public final class ByteArray : Cloneable {
    // Constructors are handled with compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

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

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): kotlin.collections.ByteIterator {
        return ByteIteratorImpl(this)
    }
}

private class ByteIteratorImpl(val collection: ByteArray) : ByteIterator {
    var index : Int = 0

    public override fun nextByte(): Byte {
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

/**
 * An array of chars.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theCharArrayTypeInfo")
public final class CharArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_CharArray_get")
    external public operator fun get(index: Int): Char

    @SymbolName("Kotlin_CharArray_set")
    external public operator fun set(index: Int, value: Char): Unit

    @SymbolName("Kotlin_CharArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_CharArray_getArrayLength")
    external private fun getArrayLength(): Int
}

/**
 * An array of shorts.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theShortArrayTypeInfo")
public final class ShortArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_ShortArray_get")
    external public operator fun get(index: Int): Short

    @SymbolName("Kotlin_ShortArray_set")
    external public operator fun set(index: Int, value: Short): Unit

    @SymbolName("Kotlin_ShortArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_ShortArray_getArrayLength")
    external private fun getArrayLength(): Int
}

/**
 * An array of ints. When targeting the JVM, instances of this class are represented as `int[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theIntArrayTypeInfo")
public final class IntArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_IntArray_get")
    external public operator fun get(index: Int): Int

    @SymbolName("Kotlin_IntArray_set")
    external public operator fun set(index: Int, value: Int): Unit

    @SymbolName("Kotlin_IntArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_IntArray_getArrayLength")
    external private fun getArrayLength(): Int
}

/**
 * An array of longs.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theLongArrayTypeInfo")
public final class LongArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_LongArray_get")
    external public operator fun get(index: Int): Long

    @SymbolName("Kotlin_LongArray_set")
    external public operator fun set(index: Int, value: Long): Unit

    @SymbolName("Kotlin_LongArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_LongArray_getArrayLength")
    external private fun getArrayLength(): Int
}

/**
 * An array of floats.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
@ExportTypeInfo("theFloatArrayTypeInfo")
public final class FloatArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_FloatArray_get")
    external public operator fun get(index: Int): Float

    @SymbolName("Kotlin_FloatArray_set")
    external public operator fun set(index: Int, value: Float): Unit

    @SymbolName("Kotlin_FloatArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_FloatArray_getArrayLength")
    external private fun getArrayLength(): Int
}

@ExportTypeInfo("theDoubleArrayTypeInfo")
public final class DoubleArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_DoubleArray_get")
    external public operator fun get(index: Int): Double

    @SymbolName("Kotlin_DoubleArray_set")
    external public operator fun set(index: Int, value: Double): Unit

    @SymbolName("Kotlin_DoubleArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_DoubleArray_getArrayLength")
    external private fun getArrayLength(): Int
}

@ExportTypeInfo("theBooleanArrayTypeInfo")
public final class BooleanArray : Cloneable {
    // Constructors are handled with the compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_BooleanArray_get")
    external public operator fun get(index: Int): Boolean

    @SymbolName("Kotlin_BooleanArray_set")
    external public operator fun set(index: Int, value: Boolean): Unit

    @SymbolName("Kotlin_BooleanArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_BooleanArray_getArrayLength")
    external private fun getArrayLength(): Int
}
package kotlin
import konan.internal.ExportForCompiler

// TODO: remove that, as RTTI shall be per instantiation.
@ExportTypeInfo("theArrayTypeInfo")
public final class Array<T> : Cloneable {
    // Constructors are handled with compiler magic.
    public constructor(size: Int, init: (Int) -> T) {
        var index = 0
        while (index < size) {
            this[index] = init(index)
            index++
        }
    }

    @ExportForCompiler
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}
    // TOD: This is a puncture
    //internal constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_Array_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_Array_get")
    external public operator fun get(index: Int): T

    @SymbolName("Kotlin_Array_set")
    external public operator fun set(index: Int, value: T): Unit

    public operator fun iterator(): kotlin.collections.Iterator<T> {
        return IteratorImpl(this)
    }

    // Konan-specific.
    @SymbolName("Kotlin_Array_getArrayLength")
    external private fun getArrayLength(): Int
}

private class IteratorImpl<T>(val collection: Array<T>) : Iterator<T> {
    var index : Int = 0

    public override fun next(): T {
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}



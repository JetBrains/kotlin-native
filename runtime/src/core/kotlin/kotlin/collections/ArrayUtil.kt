package kotlin.collections


@SymbolName("Kotlin_Array_copyImpl")
external private fun copyImpl(array: Array<Any>, fromIndex: Int,
                         destination: Array<Any>, toIndex: Int, count: Int)

@SymbolName("Kotlin_ByteArray_copyImpl")
external private fun copyImpl(array: ByteArray, fromIndex: Int,
                              destination: ByteArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_ShortArray_copyImpl")
external private fun copyImpl(array: ShortArray, fromIndex: Int,
                              destination: ShortArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_CharArray_copyImpl")
external private fun copyImpl(array: CharArray, fromIndex: Int,
                              destination: CharArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_IntArray_copyImpl")
external private fun copyImpl(array: IntArray, fromIndex: Int,
                              destination: IntArray, toIndex: Int, count: Int)
@SymbolName("Kotlin_LongArray_copyImpl")
external private fun copyImpl(array: LongArray, fromIndex: Int,
                              destination: LongArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_FloatArray_copyImpl")
external private fun copyImpl(array: FloatArray, fromIndex: Int,
                              destination: FloatArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_DoubleArray_copyImpl")
external private fun copyImpl(array: DoubleArray, fromIndex: Int,
                              destination: DoubleArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_BooleanArray_copyImpl")
external private fun copyImpl(array: BooleanArray, fromIndex: Int,
                              destination: BooleanArray, toIndex: Int, count: Int)

/**
 * Copies a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to another [destination] array starting at [destinationIndex].
 */
fun <E> Array<E>.copyRangeTo(destination: Array<E>, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(@Suppress("UNCHECKED_CAST") (this as Array<Any>), fromIndex,
             @Suppress("UNCHECKED_CAST") (destination as Array<Any>),
             destinationIndex, toIndex - fromIndex)
}

fun ByteArray.copyRangeTo(destination: ByteArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun ShortArray.copyRangeTo(destination: ShortArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun CharArray.copyRangeTo(destination: CharArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun IntArray.copyRangeTo(destination: IntArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun LongArray.copyRangeTo(destination: LongArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun FloatArray.copyRangeTo(destination: FloatArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun DoubleArray.copyRangeTo(destination: DoubleArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun BooleanArray.copyRangeTo(destination: BooleanArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}


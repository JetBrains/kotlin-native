import kotlinx.cinterop.*
import torch.*

abstract class FloatTensor(val raw: CPointer<THFloatTensor>) {
    private val storage: CPointer<THFloatStorage> get() = raw.pointed.storage!!
    private val elements get() = storage.pointed
    private val data: CPointer<FloatVar> get() = elements.data!!
    private val size: CPointer<LongVar> get() = raw.pointed.size!!
    private val nDimension: Int get() = raw.pointed.nDimension
    val shape: List<Int> = (0 until nDimension).map { size[it].toInt() }

    fun sum() = THFloatTensor_sumall(raw)
    fun flatten() = (0 until elements.size).map { data[it] }.toTypedArray()

    fun dispose() {
        THFloatTensor_free(raw)
    }

    abstract override fun toString(): String
}

class FloatVector(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    operator fun get(i: Int) = THFloatTensor_get1d(raw, i.toLong())
    operator fun set(i: Int, value: Float) = THFloatTensor_set1d(raw, i.toLong(), value)
    fun toArray() = (0 until shape[0]).map { i0 -> this[i0] }.toTypedArray()

    operator fun times(other: FloatVector) = THFloatTensor_dot(raw, other.raw)

    fun abs() = kotlin.math.sqrt(this * this)

    override fun toString() = "[${toArray().joinToString { it.toString() }}]"
}

class FloatMatrix(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    operator fun get(i0: Int, i1: Int) = THFloatTensor_get2d(raw, i0.toLong(), i1.toLong())
    operator fun set(i0: Int, i1: Int, value: Float) = THFloatTensor_set2d(raw, i0.toLong(), i1.toLong(), value)
    fun toArray() = (0 until shape[0]).map { i0 -> (0 until shape[1]).map { i1 -> this[i0, i1] }.toTypedArray() }.toTypedArray()

    operator fun times(vector: FloatVector): FloatVector {
        val result = THFloatTensor_newWithSize1d(shape[0].toLong())!!
        val added = zeros(shape[0])
        THFloatTensor_addmv(result, 1f, added.raw, 1f, raw, vector.raw)
        added.dispose()
        return FloatVector(result)
    }

    operator fun times(matrix: FloatMatrix): FloatMatrix {
        val result = THFloatTensor_newWithSize2d(shape[0].toLong(), matrix.shape[1].toLong())!!
        val added = zeros(shape[0], matrix.shape[1])
        THFloatTensor_addmm(result, 1f, added.raw, 1f, raw, matrix.raw)
        added.dispose()
        return FloatMatrix(result)
    }

    override fun toString() = "[${toArray().joinToString(",\n") { "[${it.joinToString { it.toString() }}]" }}]"
}

fun tensor(vararg values: Float): FloatVector {
    val tensor = FloatVector(THFloatTensor_newWithSize1d(values.size.toLong())!!)

    for ((i, value) in values.withIndex()) {
        tensor[i] = value
    }

    return tensor
}

fun tensor(vararg values: Array<Float>): FloatMatrix {
    val tensor = FloatMatrix(THFloatTensor_newWithSize2d(values.size.toLong(), values.first().size.toLong())!!)

    for ((i0, row) in values.withIndex()) {
        for ((i1, value) in row.withIndex()) {
            tensor[i0, i1] = value
        }
    }

    return tensor
}

fun full(constant: Float, size: Int) = tensor(*(FloatArray(size) { constant }))
fun full(constant: Float, size0: Int, size1: Int) = tensor(*(Array(size0) { Array(size1) { constant } }))

fun zeros(size: Int) = full(0f, size)
fun zeros(size0: Int, size1: Int) = full(0f, size0, size1)

fun ones(size: Int) = full(1f, size)
fun ones(size0: Int, size1: Int) = full(1f, size0, size1)

fun main(args: Array<String>) {
    val x = tensor(0f, 1f, 2f)
    val y = tensor(0f, -1f, -2f)
    val m = tensor(
            arrayOf(1f, -1f, 0f),
            arrayOf(0f, -1f, 0f),
            arrayOf(0f, 0f, -.5f))

    println("Hello, Torch!\nx = $x\ny = $y\n" +
            "|x| = ${x.abs()}\n|y| = ${y.abs()}" +
            "x·y = ${x * y}\nm=\n$m\nm·y = ${m * y}\nm·m =\n${m * m}")

    x.dispose()
    y.dispose()
    m.dispose()
}
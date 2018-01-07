import kotlinx.cinterop.*
import torch.*

abstract class FloatTensor(val raw: CPointer<THFloatTensor>) {
    private val storage: CPointer<THFloatStorage> get() = raw.pointed.storage!!
    private val elements get() = storage.pointed
    private val data: CPointer<FloatVar> get() = elements.data!!
    private val size: CPointer<LongVar> get() = raw.pointed.size!!
    private val nDimension: Int get() = raw.pointed.nDimension
    val shape: List<Int> = (0 until nDimension).map { size[it].toInt() }

    protected operator fun plus(other: FloatTensor) = initialized(shape) {
        THFloatTensor_cadd(it.raw, raw, 1f, other.raw)
    }

    protected operator fun minus(other: FloatTensor) = initialized(shape) {
        THFloatTensor_cadd(it.raw, raw, -1f, other.raw)
    }

    fun sum() = THFloatTensor_sumall(raw)
    fun flatten() = (0 until elements.size).map { data[it] }.toTypedArray()

    fun dispose() {
        THFloatTensor_free(raw)
    }

    fun asVector() = FloatVector(raw)
    fun asMatrix() = FloatMatrix(raw)

    abstract override fun toString(): String
}

class FloatVector(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    operator fun get(i: Int) = THFloatTensor_get1d(raw, i.toLong())
    operator fun set(i: Int, value: Float) = THFloatTensor_set1d(raw, i.toLong(), value)
    fun toArray() = (0 until shape[0]).map { i0 -> this[i0] }.toTypedArray()

    operator fun plus(other: FloatVector) = super.plus(other).asVector()
    operator fun minus(other: FloatVector) = super.minus(other).asVector()
    operator fun times(other: FloatVector) = THFloatTensor_dot(raw, other.raw)

    fun abs() = kotlin.math.sqrt(this * this)

    override fun toString() = "[${toArray().joinToString { it.toString() }}]"
}

class FloatMatrix(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    operator fun get(i0: Int, i1: Int) = THFloatTensor_get2d(raw, i0.toLong(), i1.toLong())
    operator fun set(i0: Int, i1: Int, value: Float) = THFloatTensor_set2d(raw, i0.toLong(), i1.toLong(), value)
    fun toArray() = (0 until shape[0]).map { i0 -> (0 until shape[1]).map { i1 -> this[i0, i1] }.toTypedArray() }.toTypedArray()

    operator fun plus(other: FloatMatrix) = super.plus(other).asMatrix()
    operator fun minus(other: FloatMatrix) = super.minus(other).asMatrix()

    operator fun times(vector: FloatVector): FloatVector {
        val result = THFloatTensor_newWithSize1d(shape[0].toLong())!!
        val zeros = zeros(shape[0])
        THFloatTensor_addmv(result, 1f, zeros.raw, 1f, raw, vector.raw)
        zeros.dispose()
        return FloatVector(result)
    }

    operator fun times(matrix: FloatMatrix): FloatMatrix {
        val result = THFloatTensor_newWithSize2d(shape[0].toLong(), matrix.shape[1].toLong())!!
        val zeros = zeros(shape[0], matrix.shape[1])
        THFloatTensor_addmm(result, 1f, zeros.raw, 1f, raw, matrix.raw)
        zeros.dispose()
        return FloatMatrix(result)
    }

    override fun toString() = "[${toArray().joinToString(",\n") { "[${it.joinToString { it.toString() }}]" }}]"
}

fun uninitialized(size: Int) = FloatVector(THFloatTensor_newWithSize1d(size.toLong())!!)
fun uninitialized(size0: Int, size1: Int) = FloatMatrix(THFloatTensor_newWithSize2d(size0.toLong(), size1.toLong())!!)
fun uninitialized(shape: List<Int>) = when (shape.size) {
    1 -> uninitialized(shape.single())
    2 -> uninitialized(shape[0], shape[1])
    else -> throw Error("Tensors with ${shape.size} dimensions are not supported yet.")
}

fun <T> initialized(size: Int, initializer: (FloatVector) -> T) = uninitialized(size).apply { initializer(this) }
fun <T> initialized(size0: Int, size1: Int, initializer: (FloatMatrix) -> T) = uninitialized(size0, size1).apply { initializer(this) }
fun <T> initialized(shape: List<Int>, initializer: (FloatTensor) -> T) = uninitialized(shape).apply { initializer(this) }

fun tensor(vararg values: Float) = initialized(values.size) {
    for ((i, value) in values.withIndex()) {
        it[i] = value
    }
}

fun tensor(vararg values: Array<Float>) = initialized(values.size, values.first().size) {
    for ((i0, row) in values.withIndex()) {
        for ((i1, value) in row.withIndex()) {
            it[i0, i1] = value
        }
    }
}

fun full(constant: Float, size: Int) = tensor(*(FloatArray(size) { constant }))
fun full(constant: Float, size0: Int, size1: Int) = tensor(*(Array(size0) { Array(size1) { constant } }))

fun zeros(size: Int) = full(0f, size)
fun zeros(size0: Int, size1: Int) = full(0f, size0, size1)

fun ones(size: Int) = full(1f, size)
fun ones(size0: Int, size1: Int) = full(1f, size0, size1)


object Abs {
    operator fun invoke(input: FloatVector) = initialized(input.shape.single()) {
        THNN_FloatAbs_updateOutput(cValuesOf<FloatVar>(), input.raw, it.raw)
    }

    fun inputGradient(input: FloatVector, outputGradient: FloatVector = ones(input.shape[0])) = initialized(input.shape[0]) {
        THNN_FloatAbs_updateGradInput(cValuesOf<FloatVar>(), input.raw, outputGradient.raw, it.raw)
    }
}

object Relu {
    operator fun invoke(input: FloatVector) = initialized(input.shape.single()) {
        THNN_FloatLeakyReLU_updateOutput(cValuesOf<FloatVar>(), input.raw, it.raw, 0.0, false)
    }

    fun inputGradient(input: FloatVector, outputGradient: FloatVector = ones(input.shape.single())) = initialized(input.shape[0]) {
        THNN_FloatLeakyReLU_updateGradInput(cValuesOf<FloatVar>(), input.raw, outputGradient.raw, it.raw, 0.0, false)
    }
}

object MeanSquaredError {
    operator fun invoke(input: FloatVector, target: FloatVector) = initialized(1) {
        THNN_FloatMSECriterion_updateOutput(cValuesOf<FloatVar>(), input.raw, target.raw, it.raw, sizeAverage = true, reduce = true)
    }

    fun inputGradient(input: FloatVector, target: FloatVector, outputGradient: FloatVector = ones(1)) = initialized(input.shape[0]) {
        THNN_FloatMSECriterion_updateGradInput(cValuesOf<FloatVar>(), input.raw, target.raw, outputGradient.raw, it.raw, sizeAverage = true, reduce = true)
    }
}

class Linear(var weight: FloatMatrix, var bias: FloatVector) {
    val inputSize = weight.shape[1]
    val outputSize = weight.shape[0]
    val addBuffer = uninitialized(outputSize)

    operator fun invoke(input: FloatVector) = initialized(outputSize) {
        THNN_FloatLinear_updateOutput(cValuesOf<FloatVar>(), input.raw, it.raw, weight.raw, bias.raw, addBuffer.raw)
    }

    fun inputGradient(input: FloatVector, outputGradient: FloatVector) = initialized(inputSize) {
        THNN_FloatLinear_updateGradInput(cValuesOf<FloatVar>(), input.raw, outputGradient.raw, it.raw, weight.raw)
    }

    fun parameterGradient(input: FloatVector, outputGradient: FloatVector, inputGradient: FloatVector): Pair<FloatMatrix, FloatVector> {
        val biasGradient = zeros(outputSize)
        val weightGradient = zeros(weight.shape[0], weight.shape[1]).also {
            THNN_FloatLinear_accGradParameters(cValuesOf<FloatVar>(), input.raw, outputGradient.raw, inputGradient.raw, weight.raw, bias.raw, it.raw, biasGradient.raw, addBuffer.raw, 1.0)
        }

        return weightGradient to biasGradient
    }
}

fun main(args: Array<String>) {
    val input = tensor(-1f)
    println("abs of $input is ${Abs(input)}, gradient is ${Abs.inputGradient(input)}")
    println("relu of $input is ${Relu(input)}, gradient is ${Relu.inputGradient(input)}")

    val weight = tensor(arrayOf(1f, 0f), arrayOf(0f, -1f), arrayOf(0f, 3f))
    val bias = tensor(0f, 0f, -3f)
    println("weight: $weight, bias: $bias")
    val linear = Linear(weight = weight, bias = bias)
    val v = tensor(1f, -1f)
    val target = tensor(5f, 5f, 5f)
    val learningRate = .1f

    for (i in 0 until 100) {
        val output = linear(v)
        val mse = MeanSquaredError(output, target)
        val outputGradient = MeanSquaredError.inputGradient(output, target, outputGradient = tensor(learningRate))
        val inputGradient = linear.inputGradient(v, outputGradient = outputGradient)
        val parameterGradient = linear.parameterGradient(v, outputGradient = outputGradient, inputGradient = inputGradient)
        println("input: $v, output: $output, target: $target, MSE: $mse, output gradient: $outputGradient, input gradient: $inputGradient, parameter gradient: $parameterGradient")
        linear.weight -= parameterGradient.first
        linear.bias -= parameterGradient.second
    }
    val x = tensor(0f, 1f, 2f)
    val y = tensor(0f, -1f, -2f)
    val m = tensor(
            arrayOf(1f, -1f, 0f),
            arrayOf(0f, -1f, 0f),
            arrayOf(0f, 0f, -.5f))

    println("Hello, Torch!\nx = $x\ny = $y\n" +
            "|x| = ${x.abs()}\n|y| = ${y.abs()}\n" +
            "x+y = ${x + y}\nx-y = ${x - y}\nx·y = ${x * y}\n" +
            "m=\n$m\nm·y = ${m * y}\nm+m =\n${m + m}\nm·m =\n${m * m}")

    x.dispose()
    y.dispose()
    m.dispose()
}
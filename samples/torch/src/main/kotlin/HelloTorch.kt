import kotlinx.cinterop.*
import torch.*

abstract class FloatTensor(val raw: CPointer<THFloatTensor>) {
    private val storage: CPointer<THFloatStorage> get() = raw.pointed.storage!!
    private val elements get() = storage.pointed
    private val data: CPointer<FloatVar> get() = elements.data!!
    private val size: CPointer<LongVar> get() = raw.pointed.size!!
    protected val nDimension: Int get() = raw.pointed.nDimension

    val shape: List<Int> get() = (0 until nDimension).map { size[it].toInt() }

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
    inline fun <reified T : FloatTensor> asTensor(): T = when (T::class) {
        FloatVector::class -> asVector() as T
        FloatMatrix::class -> asMatrix() as T
        FloatTensor::class -> this as T
        else -> throw Error("Unexpected class ${T::class}")
    }

    abstract override fun toString(): String
}

class FloatVector(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    init {
        if (super.nDimension != 1)
            throw Error("A vector must have exactly 1 dimension.")
    }

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
    init {
        if (super.nDimension != 2)
            throw Error("A matrix must have exactly 2 dimensions.")
    }

    operator fun get(i0: Int, i1: Int) = THFloatTensor_get2d(raw, i0.toLong(), i1.toLong())
    operator fun set(i0: Int, i1: Int, value: Float) = THFloatTensor_set2d(raw, i0.toLong(), i1.toLong(), value)
    fun toArray() = (0 until shape[0]).map { i0 -> (0 until shape[1]).map { i1 -> this[i0, i1] }.toTypedArray() }.toTypedArray()

    operator fun plus(other: FloatMatrix) = super.plus(other).asMatrix()
    operator fun minus(other: FloatMatrix) = super.minus(other).asMatrix()

    operator fun times(vector: FloatVector) = initialized(shape[0]) {
        THFloatTensor_addmv(it.raw, 0f, it.raw, 1f, raw, vector.raw)
    }

    operator fun times(matrix: FloatMatrix) = initialized(shape[0], matrix.shape[1]) {
        THFloatTensor_addmm(it.raw, 0f, it.raw, 1f, raw, matrix.raw)
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
fun full(constant: Float, shape: List<Int>) = when (shape.size) {
    1 -> full(constant, shape.single())
    2 -> full(constant, shape[0], shape[1])
    else -> throw Error("Tensors with ${shape.size} dimensions are not supported yet.")
}

fun zeros(size: Int) = full(0f, size)
fun zeros(size0: Int, size1: Int) = full(0f, size0, size1)
fun zeros(shape: List<Int>) = full(0f, shape)

fun ones(size: Int) = full(1f, size)
fun ones(size0: Int, size1: Int) = full(1f, size0, size1)
fun ones(shape: List<Int>) = full(1f, shape)


object Abs {
    operator fun invoke(input: FloatVector) = initialized(input.shape.single()) {
        THNN_FloatAbs_updateOutput(cValuesOf<FloatVar>(), input.raw, it.raw)
    }

    fun inputGradient(input: FloatVector, outputGradient: FloatVector = ones(input.shape[0])) =
            initialized(input.shape[0]) {
                THNN_FloatAbs_updateGradInput(null, input.raw, outputGradient.raw, it.raw)
            }
}

object Relu {
    operator fun invoke(input: FloatVector) = initialized(input.shape.single()) {
        THNN_FloatLeakyReLU_updateOutput(null, input.raw, it.raw, 0.0, false)
    }

    fun inputGradient(input: FloatVector, outputGradient: FloatVector = ones(input.shape.single())) = initialized(input.shape[0]) {
        THNN_FloatLeakyReLU_updateGradInput(null, input.raw, outputGradient.raw, it.raw, 0.0, false)
    }
}

object MeanSquaredError {
    operator fun invoke(input: FloatMatrix, target: FloatMatrix) = initialized(1) {
        THNN_FloatMSECriterion_updateOutput(null, input.raw, target.raw, it.raw, sizeAverage = true, reduce = true)
    }

    fun inputGradient(input: FloatMatrix, target: FloatMatrix, outputGradient: FloatVector = ones(1)) = initialized(input.shape[0], input.shape[1]) {
        THNN_FloatMSECriterion_updateGradInput(null, input.raw, target.raw, outputGradient.raw, it.raw, sizeAverage = true, reduce = true)
    }
}

class Linear(var weight: FloatMatrix, var bias: FloatVector) {
    val inputSize = weight.shape[1]
    val outputSize = weight.shape[0]
    val addBuffer = uninitialized(outputSize)

    operator fun invoke(input: FloatMatrix) = initialized(input.shape[0], outputSize) {
        THNN_FloatLinear_updateOutput(null, input.raw, it.raw, weight.raw, bias.raw, addBuffer.raw)
    }

    fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix) = initialized(input.shape[0], inputSize) {
        THNN_FloatLinear_updateGradInput(null, input.raw, outputGradient.raw, it.raw, weight.raw)
    }

    fun parameterGradient(input: FloatMatrix, outputGradient: FloatMatrix, inputGradient: FloatMatrix): Pair<FloatMatrix, FloatVector> {
        val biasGradient = zeros(outputSize)
        val weightGradient = zeros(weight.shape[0], weight.shape[1]).also {
            THNN_FloatLinear_accGradParameters(null, input.raw, outputGradient.raw, inputGradient.raw, weight.raw, bias.raw, it.raw, biasGradient.raw, addBuffer.raw, 1.0)
        }

        return weightGradient to biasGradient
    }
}

private fun demonstrateMatrixAndVectorOperations() {
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

fun main(args: Array<String>) {
    demonstrateMatrixAndVectorOperations()

    val input = tensor(-1f)
    println("abs of $input is ${Abs(input)}, gradient is ${Abs.inputGradient(input)}")
    println("relu of $input is ${Relu(input)}, gradient is ${Relu.inputGradient(input)}")

    val weight = tensor(arrayOf(1f, 0f), arrayOf(0f, -1f), arrayOf(0f, 3f))
    val bias = tensor(0f, 0f, -3f)
    println("weight: $weight, bias: $bias")
    val linear = Linear(weight = weight, bias = bias)
    val inputs = tensor(arrayOf(1f, -1f), arrayOf(1f, -1f))
    val targets = tensor(arrayOf(5f, 5f, 5f), arrayOf(5f, 5f, 5f))
    val learningRate = .1f

    for (i in 0 until 100) {
        val output = linear(inputs)
        val mse = MeanSquaredError(output, targets)
        val outputGradient = MeanSquaredError.inputGradient(output, targets, outputGradient = tensor(learningRate))
        val inputGradient = linear.inputGradient(inputs, outputGradient = outputGradient)
        val parameterGradient = linear.parameterGradient(inputs, outputGradient = outputGradient, inputGradient = inputGradient)
        println("input: $inputs, output: $output, target: $targets, MSE: $mse, output gradient: $outputGradient, input gradient: $inputGradient, parameter gradient: $parameterGradient")
        linear.weight -= parameterGradient.first
        linear.bias -= parameterGradient.second
    }
}
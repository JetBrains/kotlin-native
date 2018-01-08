import kotlinx.cinterop.*
import torch.*

abstract class FloatTensor(val raw: CPointer<THFloatTensor>) {
    private val storage: CPointer<THFloatStorage> get() = raw.pointed.storage!!
    private val elements get() = storage.pointed
    private val data: CPointer<FloatVar> get() = elements.data!!
    private val size: CPointer<LongVar> get() = raw.pointed.size!!
    protected val nDimension: Int get() = raw.pointed.nDimension

    val shape: List<Int> get() = (0 until nDimension).map { size[it].toInt() }

    operator fun plus(other: FloatTensor) = initialized(shape) {
        THFloatTensor_cadd(it.raw, raw, 1f, other.raw)
    }

    operator fun minus(other: FloatTensor) = initialized(shape) {
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

    fun getRow(i0: Int) = (0 until shape[1]).map { i1 -> this[i0, i1] }.toTypedArray()
    operator fun get(i0: Int, i1: Int) = THFloatTensor_get2d(raw, i0.toLong(), i1.toLong())
    operator fun set(i0: Int, i1: Int, value: Float) = THFloatTensor_set2d(raw, i0.toLong(), i1.toLong(), value)
    fun toArray() = (0 until shape[0]).map { getRow(it) }.toTypedArray()

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

fun tensor(size: Int, initializer: (Int) -> Float) = initialized(size) {
    for (i in 0 until size) {
        it[i] = initializer(i)
    }
}

fun tensor(size0: Int, size1: Int, initializer: (Int, Int) -> Float) = initialized(size0, size1) {
    for (i0 in 0 until size0) {
        for (i1 in 0 until size1) {
            it[i0, i1] = initializer(i0, i1)
        }
    }
}

fun tensor(vararg values: Float) = tensor(values.size) { values[it] }
fun tensor(vararg values: Array<Float>) = tensor(values.size, values.first().size) { i0, i1 -> values[i0][i1] }

fun full(constant: Float, size: Int) = tensor(size) { constant }
fun full(constant: Float, size0: Int, size1: Int) = tensor(size0, size1) { _, _ -> constant }
fun full(constant: Float, shape: List<Int>) = when (shape.size) {
    1 -> full(constant, shape.single())
    2 -> full(constant, shape[0], shape[1])
    else -> throw Error("Tensors with ${shape.size} dimensions are not supported yet.")
}

val randomGenerator = THGenerator_new()
fun random(min: Float, max: Float) = THRandom_uniformFloat(randomGenerator, min, max)
fun randomInt(count: Int, min: Int = 0) = random(min.toFloat(), count.toFloat()).toInt()
fun random(min: Double, max: Double, size: Int) = initialized(size) { THFloatTensor_uniform(it.raw, randomGenerator, min, max) }
fun random(min: Double, max: Double, size0: Int, size1: Int) = initialized(size0, size1) { THFloatTensor_uniform(it.raw, randomGenerator, min, max) }

fun zeros(size: Int) = full(0f, size)
fun zeros(size0: Int, size1: Int) = full(0f, size0, size1)
fun zeros(shape: List<Int>) = full(0f, shape)

fun ones(size: Int) = full(1f, size)
fun ones(size0: Int, size1: Int) = full(1f, size0, size1)
fun ones(shape: List<Int>) = full(1f, shape)


object Abs : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initialized(input.shape[0], input.shape[1]) {
        THNN_FloatAbs_updateOutput(cValuesOf<FloatVar>(), input.raw, it.raw)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix) =
            initialized(input.shape[0], input.shape[1]) {
                THNN_FloatAbs_updateGradInput(null, input.raw, outputGradient.raw, it.raw)
            }
}

object Relu : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initialized(input.shape[0], input.shape[1]) {
        THNN_FloatLeakyReLU_updateOutput(null, input.raw, it.raw, 0.0, false)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix) = initialized(input.shape[0], input.shape[1]) {
        THNN_FloatLeakyReLU_updateGradInput(null, input.raw, outputGradient.raw, it.raw, 0.0, false)
    }
}

class MeanSquaredError(val target: FloatMatrix) : ParameterFreeModule<FloatMatrix, FloatVector>() {
    override operator fun invoke(input: FloatMatrix) = initialized(1) {
        THNN_FloatMSECriterion_updateOutput(null, input.raw, target.raw, it.raw, sizeAverage = true, reduce = true)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatVector) = initialized(input.shape[0], input.shape[1]) {
        THNN_FloatMSECriterion_updateGradInput(null, input.raw, target.raw, outputGradient.raw, it.raw, sizeAverage = true, reduce = true)
    }
}

abstract class Backpropagatable<Input, Output> {
    abstract inner class ForwardResults(val input: Input) {
        abstract val output: Output
        abstract fun backpropagate(outputGradient: Output): BackpropagationResults
    }

    abstract inner class BackpropagationResults(val input: Input, val output: Output, val outputGradient: Output) {
        abstract val inputGradient: Input
        abstract fun descend()
    }

    abstract fun forwardPass(input: Input): ForwardResults
}

abstract class Module<Input, Output, Parameters> : Backpropagatable<Input, Output>() {
    abstract var parameters: Parameters
    abstract fun parametersToList(parameters: Parameters): List<FloatTensor>
    abstract fun parametersFromList(list: List<FloatTensor>): Parameters
    val parameterList get() = parametersToList(parameters)

    abstract operator fun invoke(input: Input): Output
    abstract fun inputGradient(input: Input, outputGradient: Output): Input
    abstract fun parameterGradient(input: Input, outputGradient: Output, inputGradient: Input): Parameters

    override fun forwardPass(input: Input) = object : ForwardResults(input) {
        override val output = this@Module(input)
        override fun backpropagate(outputGradient: Output) = object : Backpropagatable<Input, Output>.BackpropagationResults(input, output, outputGradient) {
            override val inputGradient = this@Module.inputGradient(input, outputGradient)
            val parameterGradient = this@Module.parameterGradient(input, outputGradient = outputGradient, inputGradient = inputGradient)

            override fun descend() = this@Module.descend(parameterGradient)
        }
    }

    open fun descend(parameterGradient: Parameters) {
        parameters = parametersFromList(parameterList.zip(parametersToList(parameterGradient)) { parameter, gradient -> parameter - gradient })
    }
}

abstract class ParameterFreeModule<Input, Output> : Module<Input, Output, Unit>() {
    override var parameters = Unit
    override fun parametersToList(parameters: Unit) = emptyList<FloatTensor>()
    override fun parametersFromList(list: List<FloatTensor>) = Unit
    override fun parameterGradient(input: Input, outputGradient: Output, inputGradient: Input) = Unit
    override fun descend(parameterGradient: Unit) {}
}

class Chain<Input, Hidden, Output>(
        val module1: Backpropagatable<Input, Hidden>, val module2: Backpropagatable<Hidden, Output>) : Backpropagatable<Input, Output>() {
    override fun forwardPass(input: Input) = ChainForwardResults(input)

    inner class ChainForwardResults(input: Input) : ForwardResults(input) {
        val result1 = module1.forwardPass(input)
        val hidden = result1.output
        val result2 = module2.forwardPass(result1.output)
        override val output = result2.output
        override fun backpropagate(outputGradient: Output) = object : Backpropagatable<Input, Output>.BackpropagationResults(input, output, outputGradient) {
            val backpropResults2 = result2.backpropagate(outputGradient)
            val hiddenGradient = backpropResults2.inputGradient
            val backpropResults1 = result1.backpropagate(hiddenGradient)

            override val inputGradient = backpropResults1.inputGradient

            override fun descend() {
                backpropResults1.descend()
                backpropResults2.descend()
            }
        }
    }

    override fun toString() = "$module1 before $module2"
}

infix fun <Input, Hidden, Output> Backpropagatable<Input, Hidden>.before(other: Backpropagatable<Hidden, Output>) = Chain(this, other)

data class Linear(var weight: FloatMatrix, var bias: FloatVector) : Module<FloatMatrix, FloatMatrix, Pair<FloatMatrix, FloatVector>>() {
    val inputSize = weight.shape[1]
    val outputSize = weight.shape[0]
    val addBuffer = uninitialized(outputSize)

    override operator fun invoke(input: FloatMatrix) = initialized(input.shape[0], outputSize) {
        THNN_FloatLinear_updateOutput(null, input.raw, it.raw, weight.raw, bias.raw, addBuffer.raw)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix) = initialized(input.shape[0], inputSize) {
        THNN_FloatLinear_updateGradInput(null, input.raw, outputGradient.raw, it.raw, weight.raw)
    }

    override fun parameterGradient(input: FloatMatrix, outputGradient: FloatMatrix, inputGradient: FloatMatrix): Pair<FloatMatrix, FloatVector> {
        val biasGradient = zeros(outputSize)
        val weightGradient = zeros(weight.shape[0], weight.shape[1]).also {
            THNN_FloatLinear_accGradParameters(null, input.raw, outputGradient.raw, inputGradient.raw, weight.raw, bias.raw, it.raw, biasGradient.raw, addBuffer.raw, 1.0)
        }

        return weightGradient to biasGradient
    }

    override var parameters: Pair<FloatMatrix, FloatVector>
        get() = weight to bias
        set(value) {
            weight = value.first
            bias = value.second
        }

    override fun parametersToList(parameters: Pair<FloatMatrix, FloatVector>) = listOf(parameters.first, parameters.second)
    override fun parametersFromList(list: List<FloatTensor>) = list.first().asMatrix() to list.last().asVector()
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

private fun demonstrateManualLayers() {
    val input = tensor(arrayOf(-1f))
    println("abs of $input is ${Abs(input)}, gradient is ${Abs.inputGradient(input, outputGradient = tensor(arrayOf(1f)))}")
    println("relu of $input is ${Relu(input)}, gradient is ${Relu.inputGradient(input, outputGradient = tensor(arrayOf(1f)))}")
}

private fun demonstrateManualBackprop(inputs: FloatMatrix = tensor(arrayOf(1f, -1f), arrayOf(1f, -1f)),
                                      labels: FloatMatrix = tensor(arrayOf(5f, 5f, 5f), arrayOf(5f, 5f, 5f)),
                                      learningRate: Float = .1f) {
    val linear = Linear(weight = tensor(arrayOf(1f, 0f), arrayOf(0f, -1f), arrayOf(0f, 3f)), bias = tensor(0f, 0f, -3f))
    println(linear)
    val error = MeanSquaredError(labels)

    for (i in 0 until 100) {
        val output = linear(inputs)
        val mse = error(output)
        val outputGradient = error.inputGradient(output, outputGradient = tensor(learningRate))
        val inputGradient = linear.inputGradient(inputs, outputGradient = outputGradient)
        val parameterGradient = linear.parameterGradient(inputs, outputGradient = outputGradient, inputGradient = inputGradient)
        println("input: $inputs, output: $output, target: $labels, MSE: $mse, output gradient: $outputGradient, input gradient: $inputGradient, parameter gradient: $parameterGradient")
        linear.weight -= parameterGradient.first
        linear.bias -= parameterGradient.second
    }
}

fun randomInit(size: Int) = random(-.1, .1, size)
fun randomInit(size0: Int, size1: Int) = random(-.1, .1, size0, size1)

fun demonstrateBackprop(inputs: FloatMatrix = tensor(1000, 1) { i0, i1 -> i0.toFloat() / 1000 },
                        labels: FloatMatrix = tensor(1000, 2) { i0, i1 -> (if (i1 == 0) i0 else -i0 / 2).toFloat() },
                        learningRate: Float = 1e-3f,
                        batchSize: Int = 32) {
    fun linear(inputSize: Int, outputSize: Int) = Linear(weight = randomInit(outputSize, inputSize), bias = randomInit(outputSize))
    val predictorNetwork = linear(1, 5) before Relu before linear(5, 2)
    fun errorNetwork(labelBatch: FloatMatrix) = predictorNetwork before MeanSquaredError(labelBatch)

    print(errorNetwork(labels))

    val datasetSize = inputs.shape[0]

    for (i in 0 until 500) {
        val randomIndices = (0 until batchSize).map { randomInt(datasetSize) }
        val inputBatch = tensor(*(randomIndices.map { inputs.getRow(it) }.toTypedArray()))
        val labelBatch = tensor(*(randomIndices.map { labels.getRow(it) }.toTypedArray()))

        val forwardResults = errorNetwork(labelBatch).forwardPass(inputBatch)
        println("Iteration $i:\ninput batch: $inputBatch \nresult batch: ${forwardResults.hidden}\nlabel batch: $labelBatch\nerror: ${forwardResults.output}")
        val backpropResults = forwardResults.backpropagate(outputGradient = tensor(learningRate))
        backpropResults.descend()
    }

    println(errorNetwork(labels))
}

fun main(args: Array<String>) {
    demonstrateMatrixAndVectorOperations()
    demonstrateManualLayers()
    // demonstrateManualBackprop()
    demonstrateBackprop()
}
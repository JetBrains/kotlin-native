import kotlinx.cinterop.*
import torch.*
import platform.posix.*

abstract class FloatTensor(val raw: CPointer<THFloatTensor>) {
    private val storage: CPointer<THFloatStorage> get() = raw.pointed.storage!!
    private val elements get() = storage.pointed
    private val data: CPointer<FloatVar> get() = elements.data!!
    private val size: CPointer<LongVar> get() = raw.pointed.size!!
    protected val nDimension: Int get() = raw.pointed.nDimension

    val shape: List<Int> get() = (0 until nDimension).map { size[it].toInt() }

    operator fun plus(other: FloatTensor) = initializedTensor(shape) {
        THFloatTensor_cadd(it.raw, raw, 1f, other.raw)
    }

    operator fun minus(other: FloatTensor) = initializedTensor(shape) {
        THFloatTensor_cadd(it.raw, raw, -1f, other.raw)
    }

    open operator fun times(factor: Float) = initializedTensor(shape) {
        THFloatTensor_mul(it.raw, raw, factor)
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

    operator fun get(i: Int) = THFloatTensor_get1d(raw, i.signExtend())
    operator fun set(i: Int, value: Float) = THFloatTensor_set1d(raw, i.signExtend(), value)
    fun toArray() = (0 until shape[0]).map { i0 -> this[i0] }.toTypedArray()

    operator fun plus(other: FloatVector) = super.plus(other).asVector()
    operator fun minus(other: FloatVector) = super.minus(other).asVector()
    override operator fun times(factor: Float) = super.times(factor).asVector()
    operator fun times(other: FloatVector) = THFloatTensor_dot(raw, other.raw)

    fun abs() = kotlin.math.sqrt(this * this)

    override fun toString() = "[${toArray().joinToString { it.toString() }}]"
}

class FloatMatrix(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    init {
        if (super.nDimension != 2)
            throw Error("A matrix must have exactly 2 dimensions.")
    }

    fun getRow(i0: Int) = (0 until shape[1]).map { i1 -> this[i0, i1] }
    operator fun get(i0: Int, i1: Int) = THFloatTensor_get2d(raw, i0.signExtend(), i1.signExtend())
    operator fun set(i0: Int, i1: Int, value: Float) = THFloatTensor_set2d(raw, i0.signExtend(), i1.signExtend(), value)
    fun toList() = (0 until shape[0]).map { getRow(it) }

    operator fun plus(other: FloatMatrix) = super.plus(other).asMatrix()
    operator fun minus(other: FloatMatrix) = super.minus(other).asMatrix()
    override operator fun times(factor: Float) = super.times(factor).asMatrix()

    operator fun times(vector: FloatVector) = initializedTensor(shape[0]) {
        THFloatTensor_addmv(it.raw, 0f, it.raw, 1f, raw, vector.raw)
    }

    operator fun times(matrix: FloatMatrix) = initializedTensor(shape[0], matrix.shape[1]) {
        THFloatTensor_addmm(it.raw, 0f, it.raw, 1f, raw, matrix.raw)
    }

    override fun toString() = "[${toList().joinToString(",\n") { "[${it.joinToString { it.toString() }}]" }}]"
}

fun uninitializedTensor(size: Int) =
        FloatVector(THFloatTensor_newWithSize1d(size.signExtend())!!)

fun uninitializedTensor(size0: Int, size1: Int) =
        FloatMatrix(THFloatTensor_newWithSize2d(size0.signExtend(), size1.signExtend())!!)

fun uninitializedTensor(shape: List<Int>) = when (shape.size) {
    1 -> uninitializedTensor(shape.single())
    2 -> uninitializedTensor(shape[0], shape[1])
    else -> throw Error("Tensors with ${shape.size} dimensions are not supported yet.")
}

fun <T> initializedTensor(size: Int, initializer: (FloatVector) -> T) =
        uninitializedTensor(size).apply { initializer(this) }

fun <T> initializedTensor(size0: Int, size1: Int, initializer: (FloatMatrix) -> T) =
        uninitializedTensor(size0, size1).apply { initializer(this) }

fun <T> initializedTensor(shape: List<Int>, initializer: (FloatTensor) -> T) =
        uninitializedTensor(shape).apply { initializer(this) }

fun tensor(size: Int, initializer: (Int) -> Float) = initializedTensor(size) {
    for (i in 0 until size) {
        it[i] = initializer(i)
    }
}

fun tensor(size0: Int, size1: Int, initializer: (Int, Int) -> Float) = initializedTensor(size0, size1) {
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
fun random(min: Double, max: Double, size: Int) =
        initializedTensor(size) { THFloatTensor_uniform(it.raw, randomGenerator, min, max) }

fun random(min: Double, max: Double, size0: Int, size1: Int) =
        initializedTensor(size0, size1) { THFloatTensor_uniform(it.raw, randomGenerator, min, max) }

fun zeros(size: Int) = full(0f, size)
fun zeros(size0: Int, size1: Int) = full(0f, size0, size1)
fun zeros(shape: List<Int>) = full(0f, shape)

fun ones(size: Int) = full(1f, size)
fun ones(size0: Int, size1: Int) = full(1f, size0, size1)
fun ones(shape: List<Int>) = full(1f, shape)


private fun demonstrateMatrixAndVectorOperations() {
    val x = tensor(0f, 1f, 2f)
    val y = tensor(0f, -1f, -2f)
    val m = tensor(
            arrayOf(1f, -1f, 0f),
            arrayOf(0f, -1f, 0f),
            arrayOf(0f, 0f, -.5f))

    println("Hello, Torch!\nx = $x\ny = $y\n" +
            "|x| = ${x.abs()}\n|y| = ${y.abs()}\n" +
            "2x=${x * 2f}\nx+y = ${x + y}\nx-y = ${x - y}\nxy = ${x * y}\n" +
            "m=\n$m\nm·y = ${m * y}\nm+m =\n${m + m}\nm·m =\n${m * m}")

    x.dispose()
    y.dispose()
    m.dispose()
}

// The part up until here only depends on TH.h, while the following part also depends on THNN.h

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
    abstract fun inputGradient(input: Input, outputGradient: Output, output: Output): Input
    abstract fun parameterGradient(input: Input, outputGradient: Output, inputGradient: Input): Parameters

    override fun forwardPass(input: Input) = object : ForwardResults(input) {
        override val output = this@Module(input)
        override fun backpropagate(outputGradient: Output) =
                object : Backpropagatable<Input, Output>.BackpropagationResults(input, output, outputGradient) {
                    override val inputGradient = this@Module.inputGradient(input, outputGradient, output)
                    val parameterGradient = this@Module.parameterGradient(input,
                            outputGradient = outputGradient, inputGradient = inputGradient)

                    override fun descend() = this@Module.descend(parameterGradient)
                }
    }

    open fun descend(parameterGradient: Parameters) {
        parameters = parametersFromList(parameterList.zip(
                parametersToList(parameterGradient)) { parameter, gradient -> parameter - gradient })
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

infix fun <Input, Hidden, Output> Backpropagatable<Input, Hidden>.before(other: Backpropagatable<Hidden, Output>) =
        Chain(this, other)

object Abs : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatAbs_updateOutput(cValuesOf<FloatVar>(), input.raw, it.raw)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) =
            initializedTensor(input.shape[0], input.shape[1]) {
                THNN_FloatAbs_updateGradInput(null, input.raw, outputGradient.raw, it.raw)
            }
}

object Relu : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatLeakyReLU_updateOutput(null, input.raw, it.raw, 0.0, false)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatLeakyReLU_updateGradInput(null, input.raw, outputGradient.raw, it.raw, 0.0, false)
    }
}

object Softmax : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatSoftMax_updateOutput(null, input.raw, it.raw, 1)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatSoftMax_updateGradInput(null, input.raw, outputGradient.raw, it.raw, output.raw, 1)
    }
}

class MeanSquaredError(val labels: FloatMatrix) : ParameterFreeModule<FloatMatrix, FloatVector>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(1) {
        THNN_FloatMSECriterion_updateOutput(null, input.raw, labels.raw, it.raw, sizeAverage = true, reduce = true)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatVector, output: FloatVector) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatMSECriterion_updateGradInput(null, input.raw, labels.raw, outputGradient.raw, it.raw, sizeAverage = true, reduce = true)
    }
}

class CrossEntropyLoss(val labels: FloatMatrix) : ParameterFreeModule<FloatMatrix, FloatVector>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(1) {
        THNN_FloatBCECriterion_updateOutput(null, input.raw, labels.raw, it.raw, sizeAverage = true, reduce = true, weights = null)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatVector, output: FloatVector) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatBCECriterion_updateGradInput(null, input.raw, labels.raw, outputGradient.raw, it.raw, sizeAverage = true, reduce = true, weights = null)
    }
}

data class Linear(
        var weight: FloatMatrix,
        var bias: FloatVector) : Module<FloatMatrix, FloatMatrix, Pair<FloatMatrix, FloatVector>>() {
    val inputSize = weight.shape[1]
    val outputSize = weight.shape[0]
    val addBuffer = uninitializedTensor(outputSize)

    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], outputSize) {
        THNN_FloatLinear_updateOutput(null, input.raw, it.raw, weight.raw, bias.raw, addBuffer.raw)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) =
            initializedTensor(input.shape[0], inputSize) {
                THNN_FloatLinear_updateGradInput(null, input.raw, outputGradient.raw, it.raw, weight.raw)
            }

    override fun parameterGradient(input: FloatMatrix, outputGradient: FloatMatrix,
                                   inputGradient: FloatMatrix): Pair<FloatMatrix, FloatVector> {
        val biasGradient = zeros(outputSize)
        val weightGradient = zeros(weight.shape[0], weight.shape[1]).also {
            THNN_FloatLinear_accGradParameters(null, input.raw, outputGradient.raw, inputGradient.raw, weight.raw,
                    bias.raw, it.raw, biasGradient.raw, addBuffer.raw, 1.0)
        }

        return weightGradient to biasGradient
    }

    override var parameters: Pair<FloatMatrix, FloatVector>
        get() = weight to bias
        set(value) {
            weight = value.first
            bias = value.second
        }

    override fun parametersToList(parameters: Pair<FloatMatrix, FloatVector>) =
            listOf(parameters.first, parameters.second)

    override fun parametersFromList(list: List<FloatTensor>) = list.first().asMatrix() to list.last().asVector()
}

private fun demonstrateManualLayers() {
    val input = tensor(arrayOf(-1f))
    val abs = Abs(input)
    println("abs of $input is $abs, gradient is ${Abs.inputGradient(input, tensor(arrayOf(1f)), abs)}")
    val relu = Relu(input)
    println("relu of $input is $relu, gradient is ${Relu.inputGradient(input, tensor(arrayOf(1f)), relu)}")
}

private fun demonstrateManualBackpropagation(inputs: FloatMatrix = tensor(arrayOf(1f, -1f), arrayOf(1f, -1f)),
                                             labels: FloatMatrix = tensor(arrayOf(5f, 5f, 5f), arrayOf(5f, 5f, 5f)),
                                             learningRate: Float = .1f) {
    val linear = Linear(
            weight = tensor(arrayOf(1f, 0f), arrayOf(0f, -1f), arrayOf(0f, 3f)),
            bias = tensor(0f, 0f, -3f))
    println(linear)
    val error = MeanSquaredError(labels)

    for (i in 0 until 100) {
        val output = linear(inputs)
        val mse = error(output)
        val outputGradient = error.inputGradient(output, outputGradient = tensor(learningRate), output = mse)
        val inputGradient = linear.inputGradient(inputs, outputGradient, output)
        val parameterGradient = linear.parameterGradient(inputs, outputGradient, inputGradient)
        println("input: $inputs, output: $output, labels: $labels, MSE: $mse, output gradient: $outputGradient, " +
                "input gradient: $inputGradient, parameter gradient: $parameterGradient")
        linear.weight -= parameterGradient.first
        linear.bias -= parameterGradient.second
    }
}

fun List<Float>.maxIndex() = withIndex().maxBy { it.value }!!.index

fun accuracy(predictionBatch: FloatMatrix, labelBatch: FloatMatrix): Float {
    val resultIndexes = predictionBatch.toList().map { it.maxIndex() }
    val labelBatchIndexes = labelBatch.toList().map { it.maxIndex() }
    return resultIndexes.zip(labelBatchIndexes).
            count { (result, label) -> result == label }.toFloat() / resultIndexes.size
}

data class Dataset(val inputs: List<FloatArray>, val labels: List<FloatArray>) {
    fun batch(indices: List<Int>): Pair<FloatMatrix, FloatMatrix> {
        val inputBatch = tensor(*(indices.map { inputs[it].toTypedArray() }.toTypedArray()))
        val labelBatch = tensor(*(indices.map { labels[it].toTypedArray() }.toTypedArray()))
        return inputBatch to labelBatch
    }

    fun sampleBatch(batchSize: Int) = batch((0 until batchSize).map { randomInt(inputs.size) })
    fun batchAt(batchIndex: Int, batchSize: Int) =
            batch((0 until inputs.size).drop(batchSize + batchIndex).take(batchSize))

    fun testBatches(batchSize: Int) = (0 until inputs.size / batchSize).map { batchAt(it, batchSize = batchSize) }
}

fun Float.toRoundedString(digits: Int = 0): String {
    var factor = 1

    for (i in 0 until digits) {
        factor *= 10
    }

    return (kotlin.math.round(this * factor) / factor).toString()
}

fun Float.toPercentageString(roundToDigits: Int = 1) = (this * 100).toRoundedString(roundToDigits)

fun Backpropagatable<FloatMatrix, FloatMatrix>.trainClassifier(
        dataset: Dataset = exampleDataset,
        lossByLabels: (FloatMatrix) -> Backpropagatable<FloatMatrix, FloatVector> = { CrossEntropyLoss(labels = it) },
        learningRateByProgress: (Float) -> Float = { 5f * kotlin.math.exp(-it * 3) },
        batchSize: Int = 64,
        iterations: Int = 500) {

    for (i in 0 until iterations) {
        val (inputBatch, labelBatch) = dataset.sampleBatch(batchSize)
        val errorNetwork = this before lossByLabels(labelBatch)
        val forwardResults = errorNetwork.forwardPass(inputBatch)
        val accuracy = accuracy(forwardResults.hidden, labelBatch)
        val progress = i.toFloat() / iterations
        val learningRate = learningRateByProgress(progress)
        val backpropResults = forwardResults.backpropagate(outputGradient = tensor(learningRate))
        val crossEntropy = forwardResults.output[0]
        backpropResults.descend()
        println("Iteration ${i + 1}/$iterations: " +
                "${accuracy.toPercentageString()}% training accuracy, " +
                "cross entropy loss = ${crossEntropy.toRoundedString(5)}, " +
                "learning rate = ${learningRate.toRoundedString(4)}")
    }
}

fun Backpropagatable<FloatMatrix, FloatMatrix>.testClassifier(dataset: Dataset, batchSize: Int = 100): Float {
    val testBatches = dataset.testBatches(batchSize)
    return testBatches.withIndex().map { (i, batchPair) ->
        val (inputBatch, outputBatch) = batchPair
        val accuracy = accuracy(this.forwardPass(inputBatch).output, outputBatch)
        println("test batch ${i + 1}/${testBatches.size}: ${accuracy.toPercentageString()}% accuracy")
        accuracy * inputBatch.shape[0]
    }.sum() / dataset.inputs.size
}

val exampleDataset = Dataset(
        (0 until 1000).map { floatArrayOf(it.toFloat() / 1000) },
        (0 until 1000).map { floatArrayOf(it.toFloat(), -it.toFloat() / 2) })

/**
 * Provides the MNIST labeled handwritten digit dataset, described at http://yann.lecun.com/exdb/mnist/
 */
class MNIST(val directory: String = "./samples/torch/") {
    fun <T> read(fileName: String, action: (Int, CPointer<ByteVar>) -> T): T {
        val filePath = directory + fileName
        val file = fopen(fileName, "rb") ?: throw Error("Cannot read input file $filePath")

        try {
            memScoped {
                fseek(file, 0, SEEK_END)
                val fileSize = ftell(file)
                println("Reading $fileSize bytes from $filePath.")
                val buffer = allocArray<ByteVar>(fileSize)
                rewind(file)
                fread(buffer, fileSize, 1, file)

                return action(fileSize.toInt(), buffer)
            }
        } finally {
            fclose(file)
        }
    }

    fun Byte.reinterpretAsUnsigned() = this.toInt().let { it + if (it < 0) 256 else 0 }

    private fun unsignedBytesToInt(bytes: List<Byte>) =
            bytes.withIndex().map { (i, value) -> value.reinterpretAsUnsigned().shl(8 * (3 - i)) }.sum()

    val intSize = 4
    fun CPointer<ByteVar>.getIntAt(index: Int) = unsignedBytesToInt((index until (index + intSize)).map { this[it] })

    val imageLength = 28
    val imageSize = imageLength * imageLength

    fun CPointer<ByteVar>.getImageAt(index: Int) =
            FloatArray(imageSize) { this[index + it].reinterpretAsUnsigned().toFloat() / 255 }

    fun oneHot(size: Int, index: Int) = FloatArray(size) { if (it == index) 1f else 0f }

    fun readLabels(fileName: String, totalLabels: Int = 10) =
            read(fileName) { fileSize, buffer ->
                val check = buffer.getIntAt(0)
                val expectedCheck = 2049
                if (check != 2049) throw Error("File should start with int $expectedCheck, but was $check.")

                val count = buffer.getIntAt(4)

                val offset = 8

                if (count + offset != fileSize) throw Error("Unexpected file size: $fileSize.")

                (0 until count).map { oneHot(totalLabels, index = buffer[offset + it].reinterpretAsUnsigned()) }
            }

    fun readImages(fileName: String) =
            read(fileName) { fileSize, buffer ->
                val check = buffer.getIntAt(0)
                val expectedCheck = 2051
                if (check != expectedCheck) throw Error("File should start with int $expectedCheck, but was $check.")

                val count = buffer.getIntAt(4)
                val width = buffer.getIntAt(8)
                val height = buffer.getIntAt(12)

                val offset = 16

                if (width != imageLength) throw Error()
                if (height != imageLength) throw Error()

                if (count * imageSize + offset != fileSize) throw Error("Unexpected file size: $fileSize.")

                (0 until count).map { buffer.getImageAt(offset + imageSize * it) }
            }

    fun labeledTrainingImages() = Dataset(
            inputs = readImages("train-images-idx3-ubyte"),
            labels = readLabels("train-labels-idx1-ubyte"))

    fun labeledTestImages() = Dataset(
            inputs = readImages("t10k-images-idx3-ubyte"),
            labels = readLabels("t10k-labels-idx1-ubyte"))
}

fun randomInit(size: Int) = random(-.01, .01, size)
fun randomInit(size0: Int, size1: Int) = random(-.1, .1, size0, size1)

fun linear(inputSize: Int, outputSize: Int) = Linear(randomInit(outputSize, inputSize), randomInit(outputSize))
fun twoLayerClassifier(dataset: Dataset, hiddenSize: Int = 64) =
        linear(dataset.inputs[0].size, hiddenSize) before Relu before
                linear(hiddenSize, dataset.labels[0].size) before Softmax

private fun trainMnistClassifier() {
    val trainingDataset = MNIST().labeledTrainingImages()
    val predictionNetwork = twoLayerClassifier(trainingDataset)
    predictionNetwork.trainClassifier(trainingDataset)

    val testDataset = MNIST().labeledTestImages()
    val averageAccuracy = predictionNetwork.testClassifier(testDataset)
    println("Accuracy on the test set: ${averageAccuracy.toPercentageString()}")
}

fun main(args: Array<String>) {
    demonstrateMatrixAndVectorOperations()
    demonstrateManualLayers()
    // demonstrateManualBackpropagation()
    trainMnistClassifier()
}
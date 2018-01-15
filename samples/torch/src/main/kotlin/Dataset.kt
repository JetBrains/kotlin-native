import kotlinx.cinterop.*
import platform.posix.*

data class Dataset(val inputs: List<FloatArray>, val labels: List<FloatArray>) {
    fun batch(indices: List<Int>): Pair<FloatMatrix, FloatMatrix> {
        val inputBatch = tensor(*(indices.map { inputs[it].toTypedArray() }.toTypedArray()))
        val labelBatch = tensor(*(indices.map { labels[it].toTypedArray() }.toTypedArray()))
        return inputBatch to labelBatch
    }

    fun sampleBatch(batchSize: Int) = batch((0 until batchSize).map { randomInt(inputs.size) })
    private fun batchAt(batchIndex: Int, batchSize: Int) =
            batch((0 until inputs.size).drop(batchSize + batchIndex).take(batchSize))

    fun testBatches(batchSize: Int) = (0 until inputs.size / batchSize).map { batchAt(it, batchSize = batchSize) }
}

/**
 * Provides the MNIST labeled handwritten digit dataset, described at http://yann.lecun.com/exdb/mnist/
 */
class MNIST(val directory: String = "./samples/torch/") {
    private fun <T> read(fileName: String, action: (Int, CPointer<ByteVar>) -> T): T {
        val filePath = directory + fileName
        val file = fopen(fileName, "rb") ?: throw Error("Cannot read input file $filePath")

        try {
            memScoped {
                fseek(file, 0, SEEK_END)
                val fileSize = ftell(file)
                println("Reading $fileSize bytes from $filePath...")
                val buffer = allocArray<ByteVar>(fileSize)
                rewind(file)
                fread(buffer, fileSize, 1, file)

                return action(fileSize.toInt(), buffer)
            }
        } finally {
            fclose(file)
        }
    }

    private fun Byte.reinterpretAsUnsigned() = this.toInt().let { it + if (it < 0) 256 else 0 }

    private fun unsignedBytesToInt(bytes: List<Byte>) =
            bytes.withIndex().map { (i, value) -> value.reinterpretAsUnsigned().shl(8 * (3 - i)) }.sum()

    private val intSize = 4
    private fun CPointer<ByteVar>.getIntAt(index: Int) =
            unsignedBytesToInt((index until (index + intSize)).map { this[it] })

    private val imageLength = 28
    private val imageSize = imageLength * imageLength

    private fun CPointer<ByteVar>.getImageAt(index: Int) =
            FloatArray(imageSize) { this[index + it].reinterpretAsUnsigned().toFloat() / 255 }

    private fun oneHot(size: Int, index: Int) = FloatArray(size) { if (it == index) 1f else 0f }

    private fun readLabels(fileName: String, totalLabels: Int = 10) =
            read(fileName) { fileSize, buffer ->
                val check = buffer.getIntAt(0)
                val expectedCheck = 2049
                if (check != 2049) throw Error("File should start with int $expectedCheck, but was $check.")

                val count = buffer.getIntAt(4)

                val offset = 8

                if (count + offset != fileSize) throw Error("Unexpected file size: $fileSize.")

                (0 until count).map { oneHot(totalLabels, index = buffer[offset + it].reinterpretAsUnsigned()) }
            }

    private fun readImages(fileName: String) =
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
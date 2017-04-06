import kotlinx.cinterop.*
import tensorflow.*

fun main(args: Array<String>) {
    println("Hello, TensorFlow ${TF_Version()!!.toKString()}!")
}
package sample.dateformat

import kotlinx.cinterop.*
import platform.posix.*

fun main(args: Array<String>) {
    val timeSeconds: Long = time(null)
    memScoped {
        val timeRef: CPointer<tm>? = localtime(cValuesOf(timeSeconds))
        val buffer: CArrayPointer<ByteVar> = allocArray<ByteVar>(128)
        if (strftime(buffer, 128, "%A, %e of %B, %Y", timeRef) == 0UL) {
            throw Exception("Failed to format date")
        }
        if (printf("Today is ${buffer.toKString()}\n") < 0) {
            throw Exception("Failed to print to stdout")
        }
    }
}

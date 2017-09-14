import sysstat.*
import kotlinx.cinterop.*

fun main(args: Array<String>) {
    val statBuf = nativeHeap.alloc<stat>()
    val res = stat("/", statBuf.ptr)
    println(res)
    println(statBuf.st_uid)
}
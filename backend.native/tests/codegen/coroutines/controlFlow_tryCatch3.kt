package codegen.coroutines.controlFlow_tryCatch3

import kotlin.test.*

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

suspend fun s1(): Int = suspendCoroutineOrReturn { x ->
    println("s1")
    x.resume(42)
    COROUTINE_SUSPENDED
}

suspend fun s2(): Int = suspendCoroutineOrReturn { x ->
    println("s2")
    x.resumeWithException(Error())
    COROUTINE_SUSPENDED
}

fun f1(): Int {
    println("f1")
    return 117
}

fun f2(): Int {
    println("f2")
    return 1
}

fun f3(x: Int, y: Int): Int {
    println("f3")
    return x + y
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Test fun runTest() {
    var result = 0

    builder {
        result = try {
            s2()
        } catch (t: Throwable) {
            f2()
        }
    }

    println(result)
}
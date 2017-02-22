// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    var ok = false
    var v  = "fail"
    suspend fun suspendHere(v: String): Unit = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        this.v = v
        x.resume(Unit)
        CoroutineIntrinsics.SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.ok = true
    })
    if (!controller.ok) throw RuntimeException("Fail 1")
    return controller.v
}

fun box(): String {

    return builder {
        suspendHere("OK")
    }
}

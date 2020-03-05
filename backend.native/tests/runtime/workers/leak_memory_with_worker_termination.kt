import kotlin.native.concurrent.*
import kotlinx.cinterop.*

fun main() {
    val worker = Worker.start()
    Worker.current.park(1000 * 1000);
    StableRef.create(Any())
    worker.requestTermination().result
}

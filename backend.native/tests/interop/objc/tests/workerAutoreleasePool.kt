import kotlin.native.concurrent.*
import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test
fun testExecute1() =
        test(Execute, InMain, Park)

@Test
fun testExecute2() =
        test(Execute, InMain, ProcessQueue)

@Test
fun testExecute3() =
        test(Execute, InWorker, Park)

@Test
fun testExecute4() =
        test(Execute, InWorker, ProcessQueue)

@Test
fun testExecute5() =
        test(Execute, InMainToWorker, NoYield)

@Test
fun testExecuteAfter1() =
        test(ExecuteAfter, InMain, Park)

@Test
fun testExecuteAfter2() =
        test(ExecuteAfter, InMain, ProcessQueue)

@Test
fun testExecuteAfter3() =
        test(ExecuteAfter, InWorker, Park)

@Test
fun testExecuteAfter4() =
        test(ExecuteAfter, InWorker, ProcessQueue)

@Test
fun testExecuteAfter5() =
        test(ExecuteAfter, InMainToWorker, NoYield)

private fun <Job> test(method: ExecuteMethod<Job>, context: Context, yieldMethod: Yield) {
    context.withWorker { worker ->
        fun execute(block: () -> Unit) {
            val job = method.submit(worker, block)
            yieldMethod.yield()
            method.wait(job)
        }

        val deallocated = CreateAutoreleaseDeallocated()

        execute {
            CreateAutorelease.createAutorelease(deallocated)
            // Object is still in autorelease pool:
            assertFalse(deallocated.value)
        }

        // autorelease pool is processed after the job is finished, so the object should be deallocated;
        // Checking in a job to make sure previous job is completely processed:
        execute {
            assertTrue(deallocated.value)
        }
    }
}

interface ExecuteMethod<Job> {
    fun submit(worker: Worker, block: () -> Unit): Job
    fun wait(job: Job)
}

object Execute : ExecuteMethod<Future<Unit>> {
    override fun submit(worker: Worker, block: () -> Unit) = worker.execute(TransferMode.SAFE, { block.freeze() }) {
        it()
    }

    override fun wait(job: Future<Unit>) {
        job.result // Throws on failure.
    }
}

object ExecuteAfter : ExecuteMethod<AtomicReference<Any?>> {
    override fun submit(worker: Worker, block: () -> Unit): AtomicReference<Any?> {
        val result = AtomicReference<Any?>(null)

        worker.executeAfter(0L, {
            try {
                block()
                result.value = true
            } catch (e: Throwable) {
                result.value = e.freeze()
            }
        }.freeze())

        return result
    }

    override fun wait(job: AtomicReference<Any?>) {
        while (true) {
            when (val it = job.value) {
                null -> continue
                true -> return
                else -> throw it as Throwable
            }
        }
    }
}

interface Context {
    fun withWorker(block: (worker: Worker) -> Unit)
}

object InMain : Context {
    override fun withWorker(block: (worker: Worker) -> Unit) = block(Worker.current)
}

object InMainToWorker : Context {
    override fun withWorker(block: (Worker) -> Unit) = kotlin.native.concurrent.withWorker {
        block(this)
    }
}

object InWorker : Context {
    override fun withWorker(block: (Worker) -> Unit) = kotlin.native.concurrent.withWorker {
        val method = Execute
        method.wait(method.submit(this) {
            block(this)
        })
    }
}

interface Yield {
    fun yield()
}

object Park : Yield {
    override fun yield() {
        Worker.current.park(0L, process = true)
    }
}

object ProcessQueue : Yield {
    override fun yield() {
        Worker.current.processQueue()
    }
}

object NoYield : Yield {
    override fun yield() {}
}

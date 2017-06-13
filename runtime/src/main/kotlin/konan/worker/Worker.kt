/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package konan.worker

import konan.SymbolName
import konan.internal.ExportForCppRuntime
import kotlinx.cinterop.*

/**
 *      Workers: theory of operations.
 *
 *  Worker represent asynchronous and concurrent computation, usually performed by other threads
 * in the same process. Object passing between workers is performed using 'transfer()' operation.
 * See 'Object Transfer Basics' below for more details on how objects shall be transferred.
 * Once object is transferrable, it is provable disjoint from object graph of owning worker and
 * could be implanted into object graph of another worker.
 */

// Unique per-process identifier of worker.
typealias WorkerId = Int
// Unique per-process identifier of the future.
typealias FutureId = Int

enum class FutureState(val value: Int) {
    INVALID(0),
    // Future is scheduled for execution.
    SCHEDULED(1),
    // Future result is computed.
    COMPUTED(2),
    // Future is cancelled.
    CANCELLED(3)
}

/**
 * Class representing abstract computation, whose result may become available in the future.
 */
// TODO: make me value class!
class Future<T> {

    val id: FutureId

    internal constructor(id: FutureId) {
        this.id = id
    }

    /**
     * Blocks execution of the current worker, until future is ready.
     */
    fun consume(code: (T) -> Unit) {
        when (state) {
            FutureState.SCHEDULED, FutureState.COMPUTED -> {
                val value = consumeFuture(id) as T
                code(value)
            }
            FutureState.INVALID ->
                throw IllegalStateException("Future is in an invalid state: $state")
            FutureState.CANCELLED ->
                throw IllegalStateException("Future is cancelled")
        }
    }

    val state: FutureState
            get() = FutureState.values()[stateOfFuture(id)]
}

/**
 * Placeholder class denoting that certain object subgraph is ready for transfer to another
 * worker (i.e. reachability analysis is complete and proven that object is unreachable from this worker).
 * Inside we store a stable native pointer usable to perform actual transfer (depending on memory
 * management scheme either direct pointer to the root of subgraph or stable pointer to place
 * where movable pointer is stored).
 */
// TODO: make me value class!
internal class Transferrable<T>(val value: NativePtr)

/**
 * Class representing worker.
 */
// TODO: make me value class!
class Worker(val id: WorkerId) {
    /**
     * Requests termination of the worker after current job is done.
     */
    fun requestTermination() = Future<Any?>(requestTerminationInternal(id))
}

/**
 * Schedule a job for further execution in the worker.
 */
fun <T1, T2> schedule(worker: Worker, mode: TransferMode, producer: () -> T1,
                      @VolatileLambda job: (T1) -> T2): Future<T2> =
    /**
     * This function is a magical operation, handled by lowering in the compiler, and replaced with call to
     *   scheduleImpl(worker, mode, producer, job)
     * but first ensuring that `job` parameter  doesn't capture any state.
     */
    throw RuntimeException("Shall not be called directly")

/**
 * Actual implementation of schedule.
 */
@konan.internal.ExportForCompiler
internal fun scheduleImpl(worker: Worker, mode: TransferMode, producer: () -> Any?,
                          job: CPointer<CFunction<*>>) : Future<Any?> =
        Future<Any?>(scheduleInternal(worker.id, mode.value, producer, job))

fun startWorker() : Worker = Worker(startInternal())

/**
 * Wait for availability of futures in the set. Returns iterable over all futures which has
 * value available for consumption.
 */
fun waitForMultipleFutures(futures: Set<Future<*>>, millis: Int) : Iterable<Future<*>> = TODO()

/**
 *   Object Transfer Basics.
 *
 *   Objects can be passed between threads in one of three possible modes.
 *
 *    * CHECKED - object is checked to be not reachable other globals or locals, and passed
 *      if so, otherwise an exception is thrown
 *    * SAFE - object is checked to be not reachable other globals or locals, and passed
 *      if so, otherwise a deep copy is being created
 *    * UNCHECKED - object is blindly passed to another worker, if there are references
 *      left in the passing worker - it may lead to crash or program malfunction
 *
 *    Checked mode checks if object is no longer used in passing worker, using memory-management
 *    specific algorithm (ARC implementation relies on trial deletion on object graph rooted in
 *    passed object), and throws IllegalStateException if object graph rooted in transferred object
 *    is reachable by some other means,
 *
 *    Safe mode checks same invariant, but instead of throwing an exception creates deep object graph
 *    copy, if object ownership cannot be transferred.
 *
 *    Unchecked mode, intended for most performance crititcal operations, where object graph ownership
 *    is expected to be correct (such as application debugged earlier in CHECKED mode), just transfers
 *    ownership without further checks.
 *
 *    When object is in transferrable state, it means that it has an unique pointer, and could be consumed
 *    easily as result of the Future.consume() operation by in some other worker.
 */
enum class TransferMode(val value: Int) {
    CHECKED(0),
    SAFE(1),
    UNCHECKED(2) // USE UNCHECKED MODE ONLY IF ABSOLUTELY SURE WHAT YOU'RE DOING!!!
}

/**
 * Creates verbatim *shallow* copy of passed object, use carefully to create disjoint object graph.
 */
fun <T> T.shallowCopy(): T = shallowCopyInternal(this) as T

// Implementation details.
@SymbolName("Kotlin_Worker_startInternal")
external internal fun startInternal() : WorkerId

@SymbolName("Kotlin_Worker_requestTerminationWorkerInternal")
external internal fun requestTerminationInternal(id: WorkerId): FutureId

@SymbolName("Kotlin_Worker_scheduleInternal")
external internal fun scheduleInternal(
        id: WorkerId, mode: Int, producer: () -> Any?, job: CPointer<CFunction<*>>) : FutureId

@SymbolName("Kotlin_Worker_shallowCopyInternal")
external internal fun shallowCopyInternal(value: Any?) : Any?

@SymbolName("Kotlin_Worker_stateOfFuture")
external internal fun stateOfFuture(id: FutureId): Int

@SymbolName("Kotlin_Worker_consumeFuture")
external internal fun consumeFuture(id: FutureId): Any?

@ExportForCppRuntime
internal fun ThrowWorkerUnsupported(): Nothing =
        throw UnsupportedOperationException("Workers are not supported")

@ExportForCppRuntime
internal fun ThrowWorkerInvalidState(): Nothing =
        throw IllegalStateException("Illegal transfer state")

@ExportForCppRuntime
internal fun WorkerLaunchpad(function: () -> Any?) = function()

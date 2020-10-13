/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.native.concurrent.*
import kotlinx.cinterop.NativePtr

public interface Cleaner

/**
 * Creates an object with a cleanup associated.
 *
 * After the resulting object ("cleaner") gets deallocated by memory manager,
 * [block] is eventually called once with [argument].
 *
 * Example of usage:
 * ```
 * class ResourceWrapper {
 *     private val resource = Resource()
 *
 *     private val cleaner = createCleaner(resource) { it.dispose() }
 * }
 * ```
 *
 * When `ResourceWrapper` becomes unused and gets deallocated, its `cleaner`
 * is also deallocated, and the resource is disposed later.
 *
 * It is not specified which thread runs [block], as well as whether two or more
 * blocks from different cleaners can be run in parallel.
 *
 * Note: if [argument] refers (directly or indirectly) the cleaner, then both
 * might leak, and the [block] will not be called in this case.
 * For example, the code below has a leak:
 * ```
 * class LeakingResourceWrapper {
 *     private val resource = Resource()
 *     private val cleaner = createCleaner(this) { it.resource.dispose() }
 * }
 * ```
 * In this case cleaner's argument (`LeakingResourceWrapper`) can't be deallocated
 * until cleaner's block is executed, which can happen only strictly after
 * the cleaner is deallocated, which can't happen until `LeakingResourceWrapper`
 * is deallocated. So the requirements on object deallocations are contradictory
 * in this case, which can't be handled gracefully. The cleaner's block
 * is not executed then, and cleaner and its argument might leak
 * (depending on the implementation).
 *
 * [block] should not use `@ThreadLocal` globals, because it may
 * be executed on a different thread.
 *
 * If [block] throws an exception, the behavior is unspecified.
 *
 * Cleaners should not be kept in globals, because if cleaner is not deallocated
 * before exiting main(), it'll never get executed.
 * Use `Platform.isCleanersLeakCheckerActive` to warn about unexecuted cleaners.
 *
 * If cleaners are not GC'd before main() exits, then it's not guaranteed that
 * they will be run. Moreover, it depends on `Platform.isCleanersLeakCheckerActive`.
 * With the checker enabled, cleaners will be run (and therefore not reported as
 * unexecuted cleaners); with the checker disabled - they might not get run.
 *
 * @param argument must be shareable
 * @param block must not capture anything
 */
// TODO: Consider just annotating the lambda argument rather than using intrinsic.
@ExperimentalStdlibApi
@TypedIntrinsic(IntrinsicType.CREATE_CLEANER)
external fun <T> createCleaner(argument: T, block: (T) -> Unit): Cleaner

/**
 * Perform GC on a worker that executes Cleaner blocks.
 */
@ExperimentalStdlibApi
fun performGCOnCleanerWorker() =
    CleanerWorker.worker.execute(TransferMode.SAFE, {}) {
        GC.collect()
    }.result

private object CleanerWorker {
    // Worker is an int. Putting it inside an object to make sure it's initialized
    // lazily.
    val worker = Worker.start(errorReporting = false, name = "Cleaner worker")
}

@ExportForCppRuntime("Kotlin_CleanerImpl_shutdownCleanerWorker")
private fun shutdownCleanerWorker(executeScheduledCleaners: Boolean) {
    // TODO: This starts the worker even if no cleaners were ever created.
    CleanerWorker.worker.requestTermination(executeScheduledCleaners).result
}

@NoReorderFields
@ExportTypeInfo("theCleanerImplTypeInfo")
@HasFinalizer
private class CleanerImpl(
    private val worker: Worker,
    private val clean: () -> Unit,
): Cleaner {}

@SymbolName("Kotlin_Any_isShareable")
external private fun Any?.isShareable(): Boolean

@SymbolName("CreateStablePointer")
external private fun createStablePointer(obj: Any): NativePtr

@SymbolName("AdoptStablePointer")
external private fun adoptStablePointer(ptr: NativePtr): Any

@ExportForCompiler
private fun <T> createCleanerImpl(argument: T, block: (T) -> Unit): Cleaner {
    if (!argument.isShareable())
        throw IllegalArgumentException("$argument must be shareable")

    val ref = createStablePointer(argument as Any)

    val clean = {
        @Suppress("UNCHECKED_CAST")
        val arg = adoptStablePointer(ref) as T
        // TODO: Maybe if this fails with exception, it should be (optionally) reported.
        block(arg)
    }.freeze()

    return CleanerImpl(CleanerWorker.worker, clean)
}

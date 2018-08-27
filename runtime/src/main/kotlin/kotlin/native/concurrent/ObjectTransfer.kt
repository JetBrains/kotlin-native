/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlinx.cinterop.*

/**
 *  Object Transfer Basics.
 *
 *  Objects can be passed between threads in one of two possible modes.
 *
 *    - CHECKED - object subgraph is checked to be not reachable by other globals or locals, and passed
 *      if so, otherwise an exception is thrown
 *    - UNCHECKED - object is blindly passed to another worker, if there are references
 *      left in the passing worker - it may lead to crash or program malfunction
 *
 *   Checked mode checks if object is no longer used in passing worker, using memory-management
 *  specific algorithm (ARC implementation relies on trial deletion on object graph rooted in
 *  passed object), and throws IllegalStateException if object graph rooted in transferred object
 *  is reachable by some other means,
 *
 *   Unchecked mode, intended for most performance crititcal operations, where object graph ownership
 *  is expected to be correct (such as application debugged earlier in CHECKED mode), just transfers
 *  ownership without further checks.
 *
 *   Note, that for some cases cycle collection need to be done to ensure that dead cycles do not affect
 *  reachability of passed object graph. See `konan.internal.GC.collect()`.
 *
 */
enum class TransferMode(val value: Int) {
    CHECKED(0),
    UNCHECKED(1) // USE UNCHECKED MODE ONLY IF ABSOLUTELY SURE WHAT YOU'RE DOING!!!
}

/**
 * Creates stable pointer to object, ensuring associated object subgraph is disjoint in specified mode
 * ([TransferMode.CHECKED] by default).
 * It could be stored to C variable or passed to another thread, where it could be retrieved with [attachObjectGraph].
 */
inline fun <reified T> detachObjectGraph(mode: TransferMode = TransferMode.CHECKED, noinline producer: () -> T): COpaquePointer? =
        detachObjectGraphInternal(mode.value, producer as () -> Any?)

/**
 * Attaches previously detached with [detachObjectGraph] object subgraph.
 * Please note, that once object graph is attached, the stable pointer does not have sense anymore,
 * and shall be discarded.
 */
inline fun <reified T> attachObjectGraph(stable: COpaquePointer?): T =
        attachObjectGraphInternal(stable) as T

// Private APIs.
@PublishedApi
@SymbolName("Kotlin_Worker_detachObjectGraphInternal")
external internal fun detachObjectGraphInternal(mode: Int, producer: () -> Any?): COpaquePointer?

@PublishedApi
@SymbolName("Kotlin_Worker_attachObjectGraphInternal")
external internal fun attachObjectGraphInternal(stable: COpaquePointer?): Any?


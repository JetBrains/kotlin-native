/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.coroutines.cancellation

/**
 * Thrown by cancellable suspending functions if the [Job] of the coroutine is cancelled while it is suspending.
 * It indicates _normal_ cancellation of a coroutine.
 * **It is not printed to console/log by default uncaught exception handler**.
 * (see [CoroutineExceptionHandler]).
 */
public actual open class CancellationException(
        message: String?,
        cause: Throwable?
) : IllegalStateException(message, cause) {
    public actual constructor(message: String?) : this(message, null)
}

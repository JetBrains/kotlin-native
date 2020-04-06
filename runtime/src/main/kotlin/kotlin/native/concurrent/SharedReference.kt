/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*

@SymbolName("Kotlin_SharedReference_createSharedRef")
external private fun createSharedRef(value: Any): NativePtr

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but can only be accessed on the worker it was created on,
 * unless the referred object is frozen too
 *
 * Note: Garbage collector currently cannot free any reference cycles with [SharedReference] or [DisposableSharedReference] in them.
 * Consider using [DisposableSharedReference] to manually resolve cycles with explicit calls to [DisposableSharedReference.dispose]
 */
@Frozen
@NoReorderFields
@ExportTypeInfo("theSharedReferenceTypeInfo")
public class SharedReference<out T : Any>(value: T) {

    private val ptr = createSharedRef(value)

    /**
     * Returns the object this reference was created for.
     * @throws IncorrectDereferenceException if referred object is not frozen and this is called from a different worker, than [SharedReference] was created on
     */
    @SymbolName("Kotlin_SharedReference_derefSharedRef")
    external fun get(): T
}

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but can only be accessed on the worker it was created on,
 * unless the referred object is frozen too.
 * Garbage collector currently cannot free any reference cycles with [SharedReference] or [DisposableSharedReference] in them.
 * Call [dispose] manually to resolve cycles
 *
 * Note: This class has more expensive [get] than [SharedReference.get]. If you don't have reference
 * cycles with [SharedReference] or [DisposableSharedReference], consider using [SharedReference]
 */
@Frozen
public class DisposableSharedReference<out T : Any>(value: T) {

    private val ref: AtomicReference<SharedReference<T>?> = AtomicReference(SharedReference(value))

    /**
     * Free the reference. Any call to [DisposableSharedReference.get] after that will
     * fail with [IllegalStateException]
     */
    fun dispose() {
        ref.value = null
    }

    /**
     * Returns the object this reference was created for.
     * @throws IncorrectDereferenceException if referred object is not frozen and this is called from a different worker, than [DisposableSharedReference] was created on
     * @throws IllegalStateException if [DisposableSharedReference.dispose] was called on this reference.
     */
    fun get(): T {
        return ref.value?.get() ?: throw IllegalStateException("illegal attempt to dereference disposed $this")
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*

@SymbolName("Kotlin_SharedRef_createSharedRef")
external private fun createSharedRef(value: Any): NativePtr

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but can only be accessed on the worker it was created on,
 * unless the referred object is frozen too
 *
 * Note: Garbage collector currently cannot free any reference cycles with [SharedRef] or [DisposableSharedRef] in them.
 * Consider using [DisposableSharedRef] to manually resolve cycles with explicit calls to [DisposableSharedRef.dispose]
 */
@Frozen
@NoReorderFields
@ExportTypeInfo("theSharedRefTypeInfo")
public class SharedRef<out T : Any>(value: T) {

    private val ptr = createSharedRef(value)

    /**
     * Returns the object this reference was created for.
     * @throws IncorrectDereferenceException if referred object is not frozen and this is called from a different worker, than [SharedRef] was created on
     */
    @SymbolName("Kotlin_SharedRef_derefSharedRef")
    external fun get(): T
}

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but can only be accessed on the worker it was created on,
 * unless the referred object is frozen too.
 * Garbage collector currently cannot free any reference cycles with [SharedRef] or [DisposableSharedRef] in them.
 * Call [dispose] manually to resolve cycles
 *
 * Note: This class has more expensive [get] than [SharedRef.get]. If you don't have reference
 * cycles with [SharedRef] or [DisposableSharedRef], consider using [SharedRef]
 */
@Frozen
public class DisposableSharedRef<out T : Any>(value: T) {

    private val ref: AtomicReference<SharedRef<T>?> = AtomicReference(SharedRef(value))

    /**
     * Free the reference. Any call to [DisposableSharedRef.get] after that will
     * fail with [IllegalStateException]
     */
    fun dispose() {
        ref.value = null
    }

    /**
     * Returns the object this reference was created for.
     * @throws IncorrectDereferenceException if referred object is not frozen and this is called from a different worker, than [DisposableSharedRef] was created on
     * @throws IllegalStateException if [DisposableSharedRef.dispose] was called on this reference.
     */
    fun get(): T {
        return ref.value?.get() ?: throw IllegalStateException("illegal attempt to dereference disposed $this")
    }
}

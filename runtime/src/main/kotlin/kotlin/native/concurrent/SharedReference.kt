/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*

@SymbolName("Kotlin_SharedReference_createSharedRef")
external private fun createSharedRef(value: Any): NativePtr

@SymbolName("Kotlin_SharedReference_derefSharedRef")
external private fun derefSharedRef(ref: NativePtr): Any

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
     * The referenced value.
     * @throws IncorrectDereferenceException if referred object is not frozen and this is called from a different worker, than [SharedReference] was created on
     */
    val value: T
        get() = @Suppress("UNCHECKED_CAST") (derefSharedRef(ptr) as T)
}

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but can only be accessed on the worker it was created on,
 * unless the referred object is frozen too.
 * Garbage collector currently cannot free any reference cycles with [SharedReference] or [DisposableSharedReference] in them.
 * Call [dispose] manually to resolve cycles
 *
 * Note: This class has more expensive [value] getter than [SharedReference]. If you don't have reference
 * cycles with [SharedReference] or [DisposableSharedReference], consider using [SharedReference]
 */
@Frozen
public class DisposableSharedReference<out T : Any>(value: T) {

    private val ref: AtomicReference<SharedReference<T>?> = AtomicReference(SharedReference(value))

    /**
     * Free the reference. Any call to [DisposableSharedReference.value] after that will
     * fail with [IllegalStateException]
     */
    fun dispose() {
        ref.value = null
    }

    /**
     * The referenced value.
     * @throws IncorrectDereferenceException if referred object is not frozen and this is called from a different worker, than [DisposableSharedReference] was created on
     * @throws IllegalStateException if [DisposableSharedReference.dispose] was called on this reference.
     */
    val value: T
        get() = ref.value?.value ?: throw IllegalStateException("illegal attempt to dereference disposed $this")
}

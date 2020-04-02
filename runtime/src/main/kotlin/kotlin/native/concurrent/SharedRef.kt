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
 * Can be safely passed between workers, but can only be accessed on the worker it was created on
 *
 * Note: Garbage collector currently cannot detect reference cycles between [SharedRef]s. Consider
 * using [DisposableSharedRef] to manually resolve cycles with explicit calls to [DisposableSharedRef.dispose]
 */
@Frozen
@NoReorderFields
@ExportTypeInfo("theSharedRefTypeInfo")
public class SharedRef<out T : Any> private constructor(private var ptr: NativePtr) {

    companion object {
        /**
         * Creates shared reference to given object
         */
        fun <T : Any> create(value: T) = SharedRef<T>(createSharedRef(value))
    }

    /**
     * Returns the object this reference was [created][SharedRef.create] for.
     */
    @SymbolName("Kotlin_SharedRef_derefSharedRef")
    external fun get(): T
}

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but can only be accessed on the worker it was created on.
 * Garbage collector currently cannot detect reference cylces between [DisposableSharedRef]s.
 * Call [dispose] manually to resolve cycles
 *
 * Note: This class has more expensive [get] than [SharedRef.get]. If you don't have reference
 * cycles between [SharedRef]s, consider using [SharedRef]
 */
@Frozen
public class DisposableSharedRef<out T : Any> private constructor(
        private val ref: AtomicReference<SharedRef<T>?>
) {

    companion object {
        /**
         * Creates shared reference to given object
         */
        fun <T : Any> create(value: T): DisposableSharedRef<T> =
                DisposableSharedRef(AtomicReference(SharedRef.create(value)))
    }

    /**
     * Free the reference. Any call to [DisposableSharedRef.get] after that will
     * fail with [NullPointerException]
     */
    fun dispose() {
        ref.value = null
    }

    /**
     * Returns the object this reference was [created][DisposableSharedRef.create] for.
     * @throws NullPointerException if [DisposableSharedRef.dispose] was called on this reference.
     */
    fun get(): T {
        return ref.value?.get() ?: throw NullPointerException()
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen
import kotlin.native.internal.NonNullNativePtr

@SymbolName("Kotlin_SharedRef_createSharedRef")
internal external fun createSharedRef(ref: SharedRef<*>, value: Any): NonNullNativePtr

@SymbolName("Kotlin_SharedRef_disposeSharedRef")
internal external fun disposeSharedRef(ref: SharedRef<*>, ptr: NonNullNativePtr)

@SymbolName("Kotlin_SharedRef_derefSharedRef")
internal external fun derefSharedRef(ptr: NonNullNativePtr): Any

@Frozen
public class SharedRef<out T : Any> private constructor() {

    private lateinit var ptr: NonNullNativePtr

    companion object {
        fun <T : Any> create(value: T): SharedRef<T> {
            val ref = SharedRef<T>()
            ref.ptr = createSharedRef(ref, value)
            return ref
        }
    }

    fun dispose() {
        disposeSharedRef(this, ptr)
    }

    @Suppress("UNCHECKED_CAST")
    fun get() = derefSharedRef(ptr) as T
}

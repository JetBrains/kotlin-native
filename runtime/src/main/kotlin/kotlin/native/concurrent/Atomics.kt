/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen
import kotlin.native.internal.NoReorderFields
import kotlin.native.SymbolName
import kotlinx.cinterop.NativePtr

/**
 * Atomic values and freezing: atomics [AtomicInt], [AtomicLong], [AtomicNativePtr] and [AtomicReference]
 * are unique types with regard to freezing. Namely, they provide mutating operations, while can participate
 * in frozen subgraphs. So shared frozen objects can have fields of atomic types.
 */
@Frozen
public class AtomicInt(private var value_: Int) {

    public var value: Int
            get() = getImpl()
            set(new) = setImpl(new)

    /**
     * Increments the value by [delta] and returns the new value.
     */
    @SymbolName("Kotlin_AtomicInt_addAndGet")
    external public fun addAndGet(delta: Int): Int

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicInt_compareAndSwap")
    external public fun compareAndSwap(expected: Int, new: Int): Int

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns true if successful.
     */
    @SymbolName("Kotlin_AtomicInt_compareAndSet")
    external public fun compareAndSet(expected: Int, new: Int): Boolean

    /**
     * Increments value by one.
     */
    public fun increment(): Unit {
        addAndGet(1)
    }

    /**
     * Decrements value by one.
     */
    public fun decrement(): Unit {
        addAndGet(-1)
    }

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = value.toString()

    // Implementation details.
    @SymbolName("Kotlin_AtomicInt_set")
    private external fun setImpl(new: Int): Unit

    @SymbolName("Kotlin_AtomicInt_get")
    private external fun getImpl(): Int
}

@Frozen
public class AtomicLong(private var value_: Long = 0)  {

    public var value: Long
        get() = getImpl()
        set(new) = setImpl(new)

    /**
     * Increments the value by [delta] and returns the new value.
     */
    @SymbolName("Kotlin_AtomicLong_addAndGet")
    external public fun addAndGet(delta: Long): Long

    /**
     * Increments the value by [delta] and returns the new value.
     */
    public fun addAndGet(delta: Int): Long = addAndGet(delta.toLong())

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicLong_compareAndSwap")
    external public fun compareAndSwap(expected: Long, new: Long): Long

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns true if successful.
     */
    @SymbolName("Kotlin_AtomicLong_compareAndSet")
    external public fun compareAndSet(expected: Long, new: Long): Boolean

    /**
     * Increments value by one.
     */
    public fun increment(): Unit {
        addAndGet(1L)
    }

    /**
     * Decrements value by one.
     */
    fun decrement(): Unit {
        addAndGet(-1L)
    }

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = value.toString()

    // Implementation details.
    @SymbolName("Kotlin_AtomicLong_set")
    private external fun setImpl(new: Long): Unit

    @SymbolName("Kotlin_AtomicLong_get")
    private external fun getImpl(): Long
}

@Frozen
public class AtomicNativePtr(private var value_: NativePtr) {

    public var value: NativePtr
        get() = getImpl()
        set(new) = setImpl(new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicNativePtr_compareAndSwap")
    external public fun compareAndSwap(expected: NativePtr, new: NativePtr): NativePtr

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     */
    @SymbolName("Kotlin_AtomicNativePtr_compareAndSet")
    external public fun compareAndSet(expected: NativePtr, new: NativePtr): Boolean

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = value.toString()

    // Implementation details.
    @SymbolName("Kotlin_AtomicNativePtr_set")
    private external fun setImpl(new: NativePtr): Unit

    @SymbolName("Kotlin_AtomicNativePtr_get")
    private external fun getImpl(): NativePtr
}

/**
 * An atomic reference to a frozen Kotlin object. Can be used in concurrent scenarious
 * but frequently shall be of nullable type and be zeroed out (with `compareAndSwap(get(), null)`)
 * once no longer needed. Otherwise memory leak could happen.
 */
@Frozen
@NoReorderFields
public class AtomicReference<T>(private var value_: T) {
    // A spinlock to fix potential ARC race.
    private var lock: Int = 0

    /**
     * Creates a new atomic reference pointing to given [ref]. If reference is not frozen,
     * @InvalidMutabilityException is thrown.
     */
    init {
        checkIfFrozen(value)
    }

    /**
     * Sets the value to [new] value
     * If [new] value is not null, it must be frozen or permanent object, otherwise an
     * @InvalidMutabilityException is thrown.
     */
    public var value: T
        get() = @Suppress("UNCHECKED_CAST")(getImpl() as T)
        set(new) = setImpl(new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * If [new] value is not null, it must be frozen or permanent object, otherwise an
     * @InvalidMutabilityException is thrown.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicReference_compareAndSwap")
    external public fun compareAndSwap(expected: T, new: T): T

    @SymbolName("Kotlin_AtomicReference_compareAndSet")
    external public fun compareAndSet(expected: T, new: T): Boolean

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = "Atomic reference to $value"

    // Implementation details.
    @SymbolName("Kotlin_AtomicReference_set")
    private external fun setImpl(new: Any?): Unit

    @SymbolName("Kotlin_AtomicReference_get")
    private external fun getImpl(): Any?

}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

/**
 * Exception thrown whenever freezing is not possible.
 *
 * @param toFreeze an object intended to be frozen.
 * @param blocker an object preventing freezing, usually one marked with [ensureNeverFrozen] earlier.
 */
public class FreezingException(toFreeze: Any, blocker: Any) :
        RuntimeException("freezing of $toFreeze has failed, first blocker is $blocker")

/**
 * Exception thrown whenever we attempt to mutate frozen objects.
 *
 * @param where a frozen object that was attempted to mutate
 */
public class InvalidMutabilityException(message: String) : RuntimeException(message)

/**
 * Freezes object subgraph reachable from this object. Frozen objects can be freely
 * shared between threads/workers. Note, that state is transitively frozen, so it may be rather
 * intrusive operation. If you do not want to actually freeze your state,
 * but willing to create a frozen copy of class' mutable state, use [toFrozen] operation instead.
 * It will not copy frozen state, but will copy all mutable state explicitly agreed on that.
 *
 * @throws FreezingException if freezing is not possible
 * @return the object itself
 * @see ensureNeverFrozen
 * @see toFrozen
 */
public fun <T> T.freeze(): T {
    freezeInternal(this)
    return this
}

/**
 * Checks if given object is null or frozen or permanent (i.e. instantiated at compile-time).
 *
 * @return true if given object is null or frozen or permanent
 */
public val Any?.isFrozen
    get() = isFrozenInternal(this)


/**
 * Transforms an object to the frozen form by creating frozen version of the object, by
 * deep copy of mutable objects and reusing frozen references. Note, that consequent calls
 * of this function may return different instances. To make a clone class must be eitehr data class or
 * explicitly marked by [kotlin.ShareByValue] annotation.
 *
 * @return the frozen object form
 * @see ensureNeverFrozen
 * @see ensureNeverCloned
 * @see kotlin.ShareByValue
 */
public inline fun <reified T> T.toFrozen(): T =
        @Suppress("UNCHECKED_CAST")
        (toFrozenInternal(this) as T)

/**
 * This function ensures that if we see such an object during freezing attempt - freeze fails and
 * [FreezingException] is thrown.
 *
 * @throws FreezingException thrown immediately if this object is already frozen
 * @see freeze
 */
@SymbolName("Kotlin_Concurrent_ensureNeverFrozen")
public external fun Any.ensureNeverFrozen()

/**
 * This function ensures that if we see such an object during object cloning part of [toFrozen]
 * [IllegalArgumentException] is thrown.
 *
 * @see toFrozen
 */
@SymbolName("Kotlin_Concurrent_ensureNeverCloned")
public external fun Any.ensureNeverCloned()
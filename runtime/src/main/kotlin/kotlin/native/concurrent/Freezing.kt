/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

/**
 * Exception thrown whenever freezing is not possible.
 *
 * @param toFreeze an object tried to be freezed
 * @param blocker an object preventing freezing, usually one marked with [ensureNeverFrozen] earlier.
 */
public class FreezingException(toFreeze: Any, blocker: Any) :
        RuntimeException("freezing of $toFreeze has failed, first blocker is $blocker")

/**
 * Exception thrown whenever we attempt to mutate frozen objects.
 *
 * @param where a frozen object that was attempted to mutate
 */
public class InvalidMutabilityException(where: Any) :
        RuntimeException("mutation attempt of frozen $where (hash is 0x${where.hashCode().toString(16)})")

/**
 * Freezes object subgraph reachable from this object. Frozen objects can be freely
 * shared between threads/workers.
 *
 * @throws FreezingException if freezing is not possible.
 * @return the object itself
 */
public fun <T> T.freeze(): T {
    freezeInternal(this)
    return this
}

/**
 * Checks if given object is null or frozen or permanent (i.e. instantiated at compile-time).
 *
 * @return true if given object is null or frozen or permanent.
 */
public val Any?.isFrozen
    get() = isFrozenInternal(this)

/**
 * This function ensures that if we see such an object during freezing attempt - freeze fails and
 * [FreezingException] is thrown.
 *
 * @throws FreezingException thrown immediately if this object is already frozen
 */
@SymbolName("Kotlin_Worker_ensureNeverFrozen")
public external fun Any.ensureNeverFrozen()
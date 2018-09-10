/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

// Note:
// Right now we don't want to have neither 'volatile' nor 'synchronized' at runtime, as it has different
// concurrency approach.

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
@Deprecated("@Volatile is meaningless in Native", level = DeprecationLevel.WARNING)
public annotation class Volatile

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
@Deprecated("@Synchronized is unsupported", level = DeprecationLevel.ERROR)
public annotation class Synchronized

@kotlin.internal.InlineOnly
@Deprecated("synchronized() is unsupported", level = DeprecationLevel.ERROR)
public actual inline fun <R> synchronized(@Suppress("UNUSED_PARAMETER") lock: Any, block: () -> R): R =
        throw UnsupportedOperationException("synchronized() is unsupported")
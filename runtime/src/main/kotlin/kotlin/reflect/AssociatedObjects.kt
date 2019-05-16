/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.reflect

import kotlin.native.internal.*

/**
 * This annotation marks the associated objects API that is considered experimental and is not subject to the
 * [general compatibility guarantees](https://kotlinlang.org/docs/reference/evolution/components-stability.html) given for the standard library:
 * the behavior of such API may be changed or the API may be removed completely in any further release.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalAssociatedObjects` must be accepted either by
 * annotating that usage with the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalAssociatedObjects::class)`,
 * or by using the compiler argument `-Xuse-experimental=kotlin.reflect.ExperimentalAssociatedObjects`.
 */
@Experimental(level = Experimental.Level.ERROR)
@Retention(value = AnnotationRetention.BINARY)
public annotation class ExperimentalAssociatedObjects

/**
 * Makes the annotated annotation class an associated object key.
 *
 * An associated object key annotation should have single [KClass] parameter.
 * When applied to a class with reference to a singleton as an argument, it binds
 * the singleton instance to the class, making this binding discoverable at runtime using [findAssociatedObject].
 */
@ExperimentalAssociatedObjects
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class AssociatedObjectKey

/**
 * If [T] is an @[AssociatedObjectKey]-annotated annotation class and [this] class is annotatated with @[T] (`S::class`),
 * returns the instance of singleton `S`.
 *
 * Otherwise returns `null`.
 */
@ExperimentalAssociatedObjects
public inline fun <reified T> KClass<*>.findAssociatedObject(): Any? =
        this.findAssociatedObject(T::class)

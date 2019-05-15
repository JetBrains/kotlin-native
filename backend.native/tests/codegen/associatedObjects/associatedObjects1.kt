/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.associatedObjects.associatedObjects1

import kotlin.test.*
import kotlin.reflect.*

@Test
@UseExperimental(ExperimentalAssociatedObjects::class)
fun runTest() {
    assertSame(Bar, Foo::class.findAssociatedObject<Associated1>())
    assertSame(Baz, Foo::class.findAssociatedObject<Associated2>())
    assertSame(null, Foo::class.findAssociatedObject<Associated3>())

    assertSame(null, Bar::class.findAssociatedObject<Associated1>())
}

@UseExperimental(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated1(val kClass: KClass<*>)

@UseExperimental(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated2(val kClass: KClass<*>)

@UseExperimental(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated3(val kClass: KClass<*>)

@Associated1(Bar::class)
@Associated2(Baz::class)
class Foo

object Bar
object Baz
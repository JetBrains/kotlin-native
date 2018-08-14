/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin

/**
 * Represents a value which is either `true` or `false`. On the JVM, non-nullable values of this type are
 * represented as values of the primitive type `boolean`.
 */
public final class Boolean private constructor(private val value: kotlin.native.internal.BooleanValue) : Comparable<Boolean> {
    /**
     * Returns the inverse of this boolean.
     */
    @SymbolName("Kotlin_Boolean_not")
    external public operator fun not(): Boolean

    /**
     * Performs a logical `and` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_and_Boolean")
    external public infix fun and(other: Boolean): Boolean

    /**
     * Performs a logical `or` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_or_Boolean")
    external public infix fun or(other: Boolean): Boolean

    /**
     * Performs a logical `xor` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_xor_Boolean")
    external public infix fun xor(other: Boolean): Boolean

    @SymbolName("Kotlin_Boolean_compareTo_Boolean")
    external public override fun compareTo(other: Boolean): Int

    // Konan-specific.
    public fun equals(other: Boolean): Boolean = kotlin.native.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
        other is Boolean && kotlin.native.internal.areEqualByValue(this, other)

    public override fun toString() = if (this) "true" else "false"

    public override fun hashCode() = if (this) 1 else 0
}
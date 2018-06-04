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

package kotlin.collections


internal actual object EmptySet : Set<Nothing>, konan.internal.KonanSet<Nothing> {
    actual override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
    actual override fun hashCode(): Int = 0
    actual override fun toString(): String = "[]"

    actual override val size: Int get() = 0
    actual override fun isEmpty(): Boolean = true
    actual override fun contains(element: Nothing): Boolean = false
    actual override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()
    actual override fun iterator(): Iterator<Nothing> = EmptyIterator

    override fun getElement(element: Nothing): Nothing? = null
}

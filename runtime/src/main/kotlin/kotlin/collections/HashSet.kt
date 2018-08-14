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

actual class HashSet<E> internal constructor(
        val backing: HashMap<E, *>
) : MutableSet<E>, AbstractMutableCollection<E>(), kotlin.native.internal.KonanSet<E> {

    actual constructor() : this(HashMap<E, Nothing>())

    actual constructor(initialCapacity: Int) : this(HashMap<E, Nothing>(initialCapacity))

    actual constructor(elements: Collection<E>) : this(elements.size) {
        addAll(elements)
    }

    // This implementation doesn't use a loadFactor
    actual constructor(initialCapacity: Int, loadFactor: Float) : this(initialCapacity)

    override actual val size: Int get() = backing.size
    override actual fun isEmpty(): Boolean = backing.isEmpty()
    override actual fun contains(element: E): Boolean = backing.containsKey(element)
    override fun getElement(element: E): E? = backing.getKey(element)
    override actual fun clear() = backing.clear()
    override actual fun add(element: E): Boolean = backing.addKey(element) >= 0
    override actual fun remove(element: E): Boolean = backing.removeKey(element) >= 0
    override actual fun iterator(): MutableIterator<E> = backing.keysIterator()

    override actual fun containsAll(elements: Collection<E>): Boolean {
        val it = elements.iterator()
        while (it.hasNext()) {
            if (!contains(it.next()))
                return false
        }
        return true
    }

    override actual fun addAll(elements: Collection<E>): Boolean {
        val it = elements.iterator()
        var updated = false
        while (it.hasNext()) {
            if (add(it.next()))
                updated = true
        }
        return updated
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is Set<*>) &&
                        contentEquals(
                                @Suppress("UNCHECKED_CAST") (other as Set<E>))
    }

    override fun hashCode(): Int {
        var result = 0
        val it = iterator()
        while (it.hasNext()) {
            result += it.next().hashCode()
        }
        return result
    }

    override fun toString(): String = collectionToString()

    // ---------------------------- private ----------------------------

    private fun contentEquals(other: Set<E>): Boolean = size == other.size && containsAll(other)
}

// This hash set keeps insertion order.
actual typealias LinkedHashSet<V> = HashSet<V>
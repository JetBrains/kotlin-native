package kotlin.collections

class ArrayList<E> private constructor(
        private var array: Array<E>,
        private var offset: Int = 0,
        private var length: Int = 0
) : MutableList<E> {

    constructor(initialCapacity: Int = 10) : this(arrayOfLateInitElements(initialCapacity))

    constructor(c: Collection<E>) : this(c.size) {
        addAll(c)
    }

    override val size: Int
        get() = length

    override fun isEmpty(): Boolean = length == 0

    override fun get(index: Int): E {
        checkIndex(index)
        return array[offset + index]
    }

    override fun set(index: Int, element: E): E {
        checkIndex(index)
        val old = array[offset + index]
        array[offset + index] = element
        return old
    }

    override fun contains(element: E): Boolean {
        var i = 0
        while (i < length) {
            if (array[i] == element) return true
            i++
        }
        return false
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        val it = elements.iterator()
        while (it.hasNext()) {
            if (!contains(it.next())) return false
        }
        return true
    }

    override fun indexOf(element: E): Int {
        var i = 0
        while (i < length) {
            if (array[i] == element) return i
            i++
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        var i = length - 1
        while (i >= 0) {
            if (array[i] == element) return i
            i--
        }
        return -1
    }

    override fun iterator(): MutableIterator<E> = Iterator<E>(this)
    override fun listIterator(): MutableListIterator<E> = Iterator<E>(this)

    override fun listIterator(index: Int): MutableListIterator<E> {
        checkIteratorIndex(index)
        return Iterator(this, index)
    }

    override fun add(element: E): Boolean {
        ensureExtraCapacity()
        array[offset + length++] = element
        return true
    }

    override fun add(index: Int, element: E) {
        insertAt(index)
        array[offset + index] = element
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        val n = elements.size
        insertAt(index, n)
        var i = 0
        val it = elements.iterator()
        while (i < n) {
            array[offset + index + i] = it.next()
            i++
        }
        return n > 0
    }

    override fun addAll(elements: Collection<E>): Boolean = addAll(length, elements)

    override fun clear() {
        array.resetRange(offset, offset + length)
        length = 0
    }

    override fun removeAt(index: Int): E {
        checkIndex(index)
        val old = array[offset + index]
        array.copyRange(offset + index + 1, offset + length, offset + index)
        array.resetAt(offset + length - 1)
        length--
        return old
    }

    override fun remove(element: E): Boolean {
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var changed = false
        val it = elements.iterator()
        while (it.hasNext()) {
            if (remove(it.next())) changed = true
        }
        return changed
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        TODO("not implemented")
        return false
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        checkIteratorIndex(fromIndex)
        checkIteratorIndex(toIndex, fromIndex)
        return ArrayList(array, offset + fromIndex, toIndex - fromIndex) // todo: mark as sublist...
    }

    fun trimToSize() {
        if (length < array.size)
            array = array.copyOfLateInitElements(length)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity > array.size) {
            var newCapacity = array.size * 3 / 2
            if (newCapacity < capacity) newCapacity = capacity
            array = array.copyOfLateInitElements(newCapacity)
        }

    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is List<*>) &&
                        contentEquals(other)
    }

    override fun hashCode(): Int {
        var result = 1
        var i = 0
        while (i < length) {
            result = result * 31 + (array[offset + i]?.hashCode() ?: 0)
            i++
        }
        return result
    }

    override fun toString(): String {
        var result = "["
        var i = 0
        while (i < length) {
            if (i > 0) result += ", "
            result += array[offset + i]
            i++
        }
        result += "]"
        return result
    }

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(n: Int = 1) {
        ensureCapacity(length + n)
    }

    private fun insertAt(index: Int, n: Int = 1) {
        ensureExtraCapacity(n)
        array.copyRange(offset + index, offset + size, offset + index + n)
        length += n
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= length) throw IndexOutOfBoundsException()
    }

    private fun checkIteratorIndex(index: Int, fromIndex: Int = 0) {
        if (index < fromIndex || index > length) throw IndexOutOfBoundsException()
    }

    private fun contentEquals(other: List<*>): Boolean {
        if (length != other.size) return false
        var i = 0
        while (i < length) {
            if (array[offset + i] != other[i]) return false
            i++
        }
        return true
    }

    private class Iterator<E>(
            private val list: ArrayList<E>, private var index: Int = 0) : MutableListIterator<E> {
        private var lastIndex: Int = -1

        override fun hasPrevious(): Boolean = index > 0
        override fun hasNext(): Boolean = index < list.length

        override fun previousIndex(): Int = index - 1
        override fun nextIndex(): Int = index

        override fun previous(): E {
            if (index <= 0) throw IndexOutOfBoundsException()
            lastIndex = --index
            return list.array[list.offset + lastIndex]
        }

        override fun next(): E {
            if (index >= list.length) throw IndexOutOfBoundsException()
            lastIndex = index++
            return list.array[list.offset + lastIndex]
        }

        override fun set(element: E) {
            list.checkIndex(lastIndex)
            list.array[list.offset + lastIndex] = element
        }

        override fun add(element: E) {
            TODO("not implemented")
        }

        override fun remove() {
            TODO("not implemented")
        }
    }
}
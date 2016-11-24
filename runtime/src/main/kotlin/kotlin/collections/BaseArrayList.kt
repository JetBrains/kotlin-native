package kotlin.collections

class BaseArrayListIterator<E>(
        val view: BaseArrayListView<E>,
        val fromIndex: Int, val toIndex: Int,
        var index: Int) : ListIterator<E> {

    public override operator fun hasNext(): Boolean {
        return index < toIndex
    }

    public override fun hasPrevious(): Boolean {
        return index > fromIndex && (toIndex - fromIndex) > 0
    }

    public override operator fun next(): E {
        return view[index++]
    }

    public override fun nextIndex(): Int {
        return index
    }

    public override fun previous(): E {
        return view[--index]
    }

    public override fun previousIndex(): Int {
        return index - 1
    }
}

class BaseArrayListView<E>(
        val storage: Array<E>,
        val indexFrom: Int, val indexTo: Int) : List<E> {

    // Query Operations
    public override val size: Int
        get() = indexTo - indexFrom

    public override fun isEmpty(): Boolean {
        return indexTo == indexFrom
    }

    public override fun contains(element: E): Boolean {
        var index = indexFrom
        while (index < indexTo) {
            if (storage[index++] == element) {
                return true
            }
        }
        return false
    }

    public override fun iterator(): Iterator<E> {
        return BaseArrayListIterator<E>(this, indexFrom, indexTo, indexFrom)
    }

    // Bulk Operations
    public override fun containsAll(elements: Collection<E>): Boolean {
        for (e in elements) {
            if (!contains(e)) return false
        }
        return true
    }

    // Positional Access Operations
    /**
     * Returns the element at the specified index in the list.
     */
    public override operator fun get(index: Int): E {
        return storage[index]
    }

    public operator fun set(index: Int, value: E) {
        storage[index] = value
    }

    // Search Operations
    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public override fun indexOf(element: E): Int {
        var index = 0
        while (index < storage.size) {
            if (storage[index] == element) {
                return index
            }
            ++index
        }
        return -1
    }

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public override fun lastIndexOf(element: E): Int {
        var index = indexTo - 1
        while (index >= indexFrom) {
            if (storage[index] == element) {
                return index
            }
            --index
        }
        return -1
    }

    // List Iterators
    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     */
    public override fun listIterator(): ListIterator<E> {
        return BaseArrayListIterator<E>(this, indexFrom, indexTo, indexFrom)
    }

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
     */
    public override fun listIterator(index: Int): ListIterator<E> {
        return BaseArrayListIterator<E>(this, indexFrom, indexTo, index)
    }

    // View
    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
     */
    public override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        return BaseArrayListView<E>(storage, fromIndex, toIndex)
    }
}

public class BaseArrayList<E> : List<E> {

    private val view: BaseArrayListView<E>

    constructor(size: Int) {
        val storage = Array<Any>(size) as Array<E>
        view = BaseArrayListView<E>(storage, 0, size)
    }

    // Query Operations
    public override val size: Int
        get() = view.size

    public override fun isEmpty(): Boolean = view.isEmpty()

    public override fun contains(
            element: E): Boolean = view.contains(element)

    public override fun iterator(): Iterator<E> = view.iterator()

    // Bulk Operations
    public override fun containsAll(elements: Collection<E>): Boolean =
            view.containsAll(elements)

    // Positional Access Operations
    /**
     * Returns the element at the specified index in the list.
     */
    public override operator fun get(index: Int): E = view.get(index)

    public operator fun set(index: Int, value: E) =
            view.set(index, value)

    // Search Operations
    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public override fun indexOf(element: E): Int =
            view.indexOf(element)

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public override fun lastIndexOf(element: E): Int =
            view.lastIndexOf(element)

    // List Iterators
    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     */
    public override fun listIterator(): ListIterator<E> =
            view.listIterator()

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
     */
    public override fun listIterator(index: Int): ListIterator<E> =
            view.listIterator(index)

    // View
    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
     */
    public override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        return BaseArrayListView<E>(view.storage, fromIndex, toIndex)
    }
}

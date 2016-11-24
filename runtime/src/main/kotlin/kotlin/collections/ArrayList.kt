package kotlin.collections

public class BaseArrayList<out E> : List<E> {
    class BaseArrayListIterator<E>(
            val collection: BaseArrayList<E>,
            val fromIndex: Int, val toIndex: Int,
            var index: Int) : ListIterator<E> {

        public override operator fun hasNext(): kotlin.Boolean {
            return index < toIndex
        }

        public override fun hasPrevious(): kotlin.Boolean {
            return index > fromIndex && (toIndex - fromIndex) > 0
        }

        public override operator fun next(): E {
            return collection[index++]
        }

        public override fun nextIndex(): kotlin.Int {
            return index
        }

        public override fun previous(): E {
            return collection[--index]
        }

        public override fun previousIndex(): kotlin.Int {
            return index - 1
        }
    }

    /*
    class BaseArrayListView<E>(
            val collection: BaseArrayList<E>,
            val indexFrom: Int, val indexTo: Int) : List<E> {

    }

    class BaseArrayListIterator<E>(
            val collection: BaseArrayListView<E>) : Iterator<E> {
        public abstract operator fun hasNext(): Boolean {
        }

        public abstract operator fun next(): T
    }*/

    // Query Operations
    public override val size: Int
        get() = indexTo - indexFrom

    public override fun isEmpty(): Boolean {
        return indexTo == indexFrom
    }

    public override fun contains(element: @UnsafeVariance E): Boolean {
        var index = indexFrom
        while (index < indexTo) {
            if (storage[index++] == element) {
                return true
            }
        }
        return false
    }

    public override fun iterator(): Iterator<E> {
        return storage.iterator()
    }

    // Bulk Operations
    public override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean {
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

    public operator fun set(index: Int, value: @UnsafeVariance E) {
        storage[index] = value
    }

    // Search Operations
    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public override fun indexOf(element: @UnsafeVariance E): Int {
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
    public override fun lastIndexOf(element: @UnsafeVariance E): Int {
        var index = storage.size - 1
        while (index >= 0) {
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
        return BaseArrayListIterator<E>(this, 0, this.size, 0)
    }

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
     */
    public override fun listIterator(index: Int): ListIterator<E> {
        return BaseArrayListIterator<E>(this, 0, this.size, index)
    }

    // View
    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
     */
    public override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        return BaseArrayListView<E>(this, fromIndex, toIndex)
    }
}

private val storage: Array<E>

constructor(size:Int) {
    storage = Array<Any>(size) as Array<E>
}

// Query Operations
public override val size: Int
    get() = storage.size

public override fun isEmpty(): Boolean {
    return storage.size == 0
}

public override fun contains(element: @UnsafeVariance E): Boolean {
    var index = 0
    while (index < storage.size) {
        if (storage[index] == element) {
            return true
        }
    }
    return false
}

public override fun iterator(): Iterator<E> {
    return storage.iterator()
}

// Bulk Operations
public override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean {
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

public operator fun set(index: Int, value: @UnsafeVariance E) {
    storage[index] = value
}

// Search Operations
/**
 * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
 * element is not contained in the list.
 */
public override fun indexOf(element: @UnsafeVariance E): Int {
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
public override fun lastIndexOf(element: @UnsafeVariance E): Int {
    var index = storage.size - 1
    while (index >= 0) {
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
    return BaseArrayListIterator<E>(this, 0)
}

/**
 * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
 */
public override fun listIterator(index: Int): ListIterator<E> {
    return BaseArrayListIterator<E>(this, index)
}

// View
/**
 * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
 * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
 */
public override fun subList(fromIndex: Int, toIndex: Int): List<E> {
    // TODO: incorrect.
    return BaseArrayListView<E>(0)
}
}


/*
class BaseArrayListView<out E>(
        val collection: BaseArrayList<E>, val fromIndex: Int, val toIndex: Int) : List<E> {
} */
package kotlin.collections

/**
 * An iterator over a collection or another entity that can be represented as a sequence of elements.
 * Allows to sequentially access the elements.
 */
public interface Iterator<out T> {
    /**
     * Returns the next element in the iteration.
     */
    public operator fun next(): T

    /**
     * Returns `true` if the iteration has more elements.
     */
    public operator fun hasNext(): Boolean
}

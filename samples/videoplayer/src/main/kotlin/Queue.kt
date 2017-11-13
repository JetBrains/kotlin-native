class Queue<T>(val maxSize: Int, val none: T) {
    val array = Array<T>(maxSize, { _ -> none})
    var head = 0
    var tail = 0

    fun push(element: T) {
        if ((tail + 1) % maxSize == head)
            throw Error("queue overflow: $tail $head")
        array[tail] = element
        tail = (tail + 1) % maxSize
    }

    fun pop() : T {
        if (tail == head)
            throw Error("queue underflow")
        val result = array[head]
        array[head] = none
        head = (head + 1) % maxSize
        return result
    }

    fun peek() : T {
        if (tail == head)
            throw Error("queue underflow")
        return array[head]
    }

    fun size() = if (tail >= head) tail - head else maxSize - (head - tail)

    fun isEmpty() = head == tail
}

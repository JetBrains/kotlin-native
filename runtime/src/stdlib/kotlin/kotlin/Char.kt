package kotlin

    /** Creates a range from this value to the specified [other] value. */
    public operator fun Char.rangeTo(other: Char): CharRange {
        return CharRange(this, other)
    }



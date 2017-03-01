package kotlin

    /** Creates a range from this value to the specified [other] value. */
    public operator fun Byte.rangeTo(other: Byte): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Byte.rangeTo(other: Short): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Byte.rangeTo(other: Int): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Byte.rangeTo(other: Long): LongRange {
        return LongRange(this.toLong(), other.toLong())
    }

    /** Creates a range from this value to the specified [other] value. */
    public operator fun Short.rangeTo(other: Byte): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Short.rangeTo(other: Short): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Short.rangeTo(other: Int): IntRange {
        return IntRange(this.toInt(), other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Short.rangeTo(other: Long): LongRange  {
        return LongRange(this.toLong(), other.toLong())
    }

    /** Creates a range from this value to the specified [other] value. */
    public operator fun Int.rangeTo(other: Byte): IntRange {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Int.rangeTo(other: Short): IntRange {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Int.rangeTo(other: Int): IntRange  {
        return IntRange(this, other.toInt())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Int.rangeTo(other: Long): LongRange {
        return LongRange(this.toLong(), other.toLong())
    }

    /** Creates a range from this value to the specified [other] value. */
    external public operator fun Long.rangeTo(other: Byte): LongRange {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Long.rangeTo(other: Short): LongRange  {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Long.rangeTo(other: Int): LongRange {
        return LongRange(this, other.toLong())
    }
    /** Creates a range from this value to the specified [other] value. */
    public operator fun Long.rangeTo(other: Long): LongRange  {
        return LongRange(this, other.toLong())
    }



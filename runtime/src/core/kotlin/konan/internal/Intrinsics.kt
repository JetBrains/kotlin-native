package konan.internal

@Intrinsic external fun areEqualByValue(first: Boolean, second: Boolean): Boolean
@Intrinsic external fun areEqualByValue(first: Char, second: Char): Boolean
@Intrinsic external fun areEqualByValue(first: Byte, second: Byte): Boolean
@Intrinsic external fun areEqualByValue(first: Short, second: Short): Boolean
@Intrinsic external fun areEqualByValue(first: Int, second: Int): Boolean
@Intrinsic external fun areEqualByValue(first: Long, second: Long): Boolean
@Intrinsic external fun areEqualByValue(first: Float, second: Float): Boolean
@Intrinsic external fun areEqualByValue(first: Double, second: Double): Boolean

inline fun areEqual(first: Any?, second: Any?): Boolean {
    return if (first == null) second == null else first.equals(second)
}

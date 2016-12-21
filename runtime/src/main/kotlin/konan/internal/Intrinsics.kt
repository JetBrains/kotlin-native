package konan.internal

/**
 * This annotation denotes that the element is intrinsic and its usages require special handling in compiler.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Intrinsic

@Intrinsic external fun areEqualByValue(first: Boolean, second: Boolean): Boolean
@Intrinsic external fun areEqualByValue(first: Char, second: Char): Boolean
@Intrinsic external fun areEqualByValue(first: Byte, second: Byte): Boolean
@Intrinsic external fun areEqualByValue(first: Short, second: Short): Boolean
@Intrinsic external fun areEqualByValue(first: Int, second: Int): Boolean
@Intrinsic external fun areEqualByValue(first: Long, second: Long): Boolean
@Intrinsic external fun areEqualByValue(first: Float, second: Float): Boolean
@Intrinsic external fun areEqualByValue(first: Double, second: Double): Boolean

// For comparing with `null`:
@Intrinsic external fun areEqualByValue(first: Nothing?, second: Any?): Boolean
@Intrinsic external fun areEqualByValue(first: Any?, second: Nothing?): Boolean

inline fun areEqual(first: Any?, second: Any?): Boolean {
    return if (first == null) second == null else first.equals(second)
}
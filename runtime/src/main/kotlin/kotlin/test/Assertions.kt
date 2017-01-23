package kotlin.test

/** Asserts that the expression is `true` with an optional [message]. */
fun assertTrue(actual: Boolean, message: String? = null) {
    if (!actual) {
        throw AssertionError(message ?: "Expected value to be true.")
    }
}

/** Asserts that the given [block] returns `true`. */
fun assertTrue(message: String? = null, block: () -> Boolean): Unit
        = assertTrue(block(), message)

/** Asserts that the expression is `false` with an optional [message]. */
fun assertFalse(actual: Boolean, message: String? = null) : Unit
        = assertTrue(!actual, message ?: "Expected value to be false.")

/** Asserts that the given [block] returns `false`. */
fun assertFalse(message: String? = null, block: () -> Boolean): Unit
        = assertFalse(block(), message)

/** Asserts that the [expected] value is equal to the [actual] value, with an optional [message]. */
fun <@kotlin.internal.OnlyInputTypes T> assertEquals(expected: T, actual: T, message: String? = null) : Unit
        = assertTrue(message) { expected == actual }

/** Asserts that the [actual] value is not equal to the illegal value, with an optional [message]. */
fun <@kotlin.internal.OnlyInputTypes T> assertNotEquals(illegal: T, actual: T, message: String? = null) : Unit
        = assertTrue(message) { illegal != actual }

/** Asserts that the [actual] value is not `null`, with an optional [message]. */
fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    assertTrue(message) { actual != null }
    return actual!!
}

/** Asserts that the [actual] value is not `null`, with an optional [message] and a function [block] to process the not-null value. */
fun <T : Any, R> assertNotNull(actual: T?, message: String? = null, block: (T) -> R) {
    assertNotNull(actual, message)
    if (actual != null) {
        block(actual)
    }
}

/** Asserts that the [actual] value is `null`, with an optional [message]. */
fun assertNull(actual: Any?, message: String? = null) : Unit
        = assertTrue(message) { actual == null}
package kotlin

/**
 * Throws an [IllegalArgumentException] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failRequireWithLazyMessage
 */
@kotlin.internal.InlineOnly
public inline fun require(value: Boolean): Unit = require(value) { "Failed requirement." }

/**
 * Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failRequireWithLazyMessage
 */
@kotlin.internal.InlineOnly
public inline fun require(value: Boolean, lazyMessage: () -> Any): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

/**
 * Throws an [IllegalArgumentException] if the [value] is null. Otherwise returns the not null value.
 */
@kotlin.internal.InlineOnly
public inline fun <T:Any> requireNotNull(value: T?): T = requireNotNull(value) { "Required value was null." }

/**
 * Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample samples.misc.Preconditions.failRequireWithLazyMessage
 */
@kotlin.internal.InlineOnly
public inline fun <T:Any> requireNotNull(value: T?, lazyMessage: () -> Any): T {
    if (value == null) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    } else {
        return value
    }
}

/**
 * Throws an [IllegalStateException] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
public inline fun check(value: Boolean): Unit = check(value) { "Check failed." }

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
public inline fun check(value: Boolean, lazyMessage: () -> Any): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

/**
 * Throws an [IllegalStateException] if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
public inline fun <T:Any> checkNotNull(value: T?): T = checkNotNull(value) { "Required value was null." }

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage]  if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
public inline fun <T:Any> checkNotNull(value: T?, lazyMessage: () -> Any): T {
    if (value == null) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    } else {
        return value
    }
}

/**
 * Throws an [IllegalStateException] with the given [message].
 *
 * @sample samples.misc.Preconditions.failWithError
 */
@kotlin.internal.InlineOnly
public inline fun error(message: Any): Nothing = throw IllegalStateException(message.toString())

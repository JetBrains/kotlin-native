package kotlin.internal

/**
 * Specifies that the corresponding type parameter is not used for unsafe operations such as casts or 'is' checks
 * That means it's completely safe to use generic types as argument for such parameter.
 */
@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class PureReifiable

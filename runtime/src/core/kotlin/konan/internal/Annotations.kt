package konan.internal

// This one is used internally to mark the presence of a backing field
// in the absence of IR.
annotation class HasBackingField

/**
 * This annotation denotes that the element is intrinsic and its usages require special handling in compiler.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Intrinsic

/**
 * Exports symbol for compiler needs.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExportForCompiler



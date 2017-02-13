package kotlin

/**
 * Suppresses the given compilation warnings in the annotated element.
 * @property names names of the compiler diagnostics to suppress.
 */
//@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER,
//        CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE, TYPEALIAS)
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION,
        AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FUNCTION)
//@Retention(SOURCE)
public annotation class Suppress(vararg val names: String)


/**
 * Signifies that the annotated functional type represents an extension function.
 */
@Target(AnnotationTarget.TYPE)
@MustBeDocumented
public annotation class ExtensionFunctionType

/**
 * Annotates type arguments of functional type and holds corresponding parameter name specified by the user in type declaration (if any).
 */
@Target(AnnotationTarget.TYPE)
@MustBeDocumented
public annotation class ParameterName(val name: String)

/**
 * Suppresses errors about variance conflict
 */
@Target(AnnotationTarget.TYPE)
//@Retention(SOURCE)
@MustBeDocumented
public annotation class UnsafeVariance


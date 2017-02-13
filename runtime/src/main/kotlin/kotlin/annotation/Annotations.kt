package kotlin.annotation

/**
 * This meta-annotation indicates the kinds of code elements which are possible targets of an annotation.
 *
 * If the target meta-annotation is not present on an annotation declaration, the annotation
 * is applicable to any code element, except type parameters, type usages, expressions, and files.
 *
 * @property allowedTargets list of allowed annotation targets
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@MustBeDocumented
public annotation class Target(vararg val allowedTargets: AnnotationTarget)

/**
 * This meta-annotation determines whether an annotation is stored in binary output and visible for reflection. By default, both are true.
 *
 * @property value necessary annotation retention (RUNTIME, BINARY or SOURCE)
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class Retention(val value: AnnotationRetention = AnnotationRetention.RUNTIME)

/**
 * This meta-annotation determines that an annotation is applicable twice or more on a single code element
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class Repeatable

/**
 * This meta-annotation determines that an annotation is a part of public API and therefore should be included in the generated
 * documentation for the element to which the annotation is applied.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class MustBeDocumented

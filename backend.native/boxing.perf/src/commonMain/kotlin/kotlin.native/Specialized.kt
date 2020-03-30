package kotlin.native

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE_PARAMETER)
internal annotation class Specialized(
        val forTypes: Array<KClass<*>> = [
            Boolean::class,
            Byte::class,
            Char::class,
            Double::class,
            Float::class,
            Int::class,
            Long::class,
            Short::class
        ]
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
internal annotation class SpecializedClass(
        val forTypes: Array<KClass<*>> = [
            Boolean::class,
            Byte::class,
            Char::class,
            Double::class,
            Float::class,
            Int::class,
            Long::class,
            Short::class
        ]
)
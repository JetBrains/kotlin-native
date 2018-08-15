/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect

/**
 * Represents a type. Type is usually either a class with optional type arguments,
 * or a type parameter of some declaration, plus nullability.
 */
public interface KType {
    /**
     * The declaration of the classifier used in this type.
     * For example, in the type `List<String>` the classifier would be the [KClass] instance for [List].
     *
     * Returns `null` if this type is not denotable in Kotlin, for example if it is an intersection type.
     */
    @SinceKotlin("1.1")
    public val classifier: KClassifier?

    /**
     * Type arguments passed for the parameters of the classifier in this type.
     * For example, in the type `Array<out Number>` the only type argument is `out Number`.
     *
     * In case this type is based on an inner class, the returned list contains the type arguments provided for the innermost class first,
     * then its outer class, and so on.
     * For example, in the type `Outer<A, B>.Inner<C, D>` the returned list is `[C, D, A, B]`.
     */
//    @SinceKotlin("1.1")
//    public val arguments: List<KTypeProjection>

    /**
     * `true` if this type was marked nullable in the source code.
     *
     * For Kotlin types, it means that `null` value is allowed to be represented by this type.
     * In practice it means that the type was declared with a question mark at the end.
     * For non-Kotlin types, it means the type or the symbol which was declared with this type
     * is annotated with a runtime-retained nullability annotation such as [javax.annotation.Nullable].
     *
     * Note that even if [isMarkedNullable] is false, values of the type can still be `null`.
     * This may happen if it is a type of the type parameter with a nullable upper bound:
     *
     * ```
     * fun <T> foo(t: T) {
     *     // isMarkedNullable == false for t's type, but t can be null here when T = "Any?"
     * }
     * ```
     */
    public val isMarkedNullable: Boolean
}
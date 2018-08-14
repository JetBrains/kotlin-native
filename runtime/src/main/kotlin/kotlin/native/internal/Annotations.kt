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

package kotlin.native.internal

/**
 * Makes this function to be possible to call by given name from C++ part of runtime using C ABI.
 * The parameters are mapped in an implementation-dependent manner.
 *
 * The function to call from C++ can be a wrapper around the original function.
 *
 * If the name is not specified, the function to call will be available by its Kotlin unqualified name.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
annotation class ExportForCppRuntime(val name: String = "")

/**
 * Makes top level function available from C/C++ code with the given name.
 * `fullName` controls the name of top level function, `shortName` controls the short name.
 * If `fullName` is empty, no top level declaration is being created.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class CName(val fullName: String = "", val shortName: String = "")

/**
 * This annotation denotes that the element is intrinsic and its usages require special handling in compiler.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Intrinsic

/**
 * Exports symbol for compiler needs.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExportForCompiler

/**
 * Annotated constructor will be inlined.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
annotation class InlineConstructor

/**
 * Class is frozen by default. Also this annotation is (ab)used for marking objects
 * where mutability checks are not needed, and they are shared, such as atomics.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class Frozen

/**
 * Fields of annotated class won't be sorted.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class NoReorderFields
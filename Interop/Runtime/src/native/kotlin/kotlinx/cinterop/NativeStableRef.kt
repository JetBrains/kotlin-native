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

package kotlinx.cinterop
import kotlin.native.*

@SymbolName("Kotlin_Interop_createStablePointer")
internal external fun createStablePointer(any: Any): COpaquePointer

@SymbolName("Kotlin_Interop_disposeStablePointer")
internal external fun disposeStablePointer(pointer: COpaquePointer)

@SymbolName("Kotlin_Interop_derefStablePointer")
private external fun derefStablePointerOrNull(pointer: COpaquePointer): Any?

@SymbolName("Kotlin_Interop_describeStablePointer")
private external fun describeStablePointer(pointer: COpaquePointer): String

@PublishedApi
internal fun derefStablePointer(pointer: COpaquePointer): Any =
    derefStablePointerOrNull(pointer) ?: throw IncorrectDereferenceException("illegal attempt to access non-shared ${describeStablePointer(pointer)} from other thread")

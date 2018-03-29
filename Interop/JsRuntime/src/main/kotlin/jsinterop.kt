/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package kotlinx.wasm.jsinterop

import konan.internal.ExportForCppRuntime
import kotlinx.cinterop.*

typealias Arena = Int
typealias Object = Int
typealias Pointer = Int

/**
 * Used annotation is required to preserve functions
 * from internalization and DCE
 */

@Used
@SymbolName("Konan_js_allocateArena")
external public fun allocateArena(): Arena

@Used
@SymbolName("Konan_js_freeArena")
external public fun freeArena(arena: Arena)

@Used
@SymbolName("Konan_js_pushIntToArena")
external public fun pushIntToArena(arena: Arena, value: Int): Object

@Used
@SymbolName("Konan_js_pushNullToArena")
external public fun pushNullToArena(arena: Arena): Object

const val upperWord = 0xffffffff.toLong() shl 32

@ExportForCppRuntime
fun doubleUpper(value: Double): Int {
    return ((value.toBits() and upperWord) ushr 32) .toInt()
}

@ExportForCppRuntime
fun doubleLower(value: Double): Int =
    (value.toBits() and 0x00000000ffffffff) .toInt()

@Used
@SymbolName("ReturnSlot_getDouble")
external public fun ReturnSlot_getDouble(): Double

@Used
@SymbolName("Konan_js_fetchString")
external public fun Konan_js_fetchString(pointer: Int)

fun AllocateAndFetchString(size: Int): String {
    memScoped {
        val destBytes = allocArray<ByteVar>(size)
        val destPtr = alloc<CArrayPointerVar<ByteVar>>()
        destPtr.value = destBytes
        Konan_js_fetchString(destPtr.ptr.toLong().toInt()) // wasm32 is 32 bit.
        return destBytes.toKString()
    }
}

@Used
@SymbolName("Kotlin_String_utf16pointer")
external public fun stringPointer(message: String): Pointer

@Used
@SymbolName("Kotlin_String_utf16length")
external public fun stringLengthBytes(message: String): Int

@Used
@SymbolName("Konan_js_unboxPrimitive")
external public fun unboxPrimitive(arena: Int, index: Int): Int
fun unboxPrimitive(value: JsValue) = unboxPrimitive(value._arena, value._index)

@Used
@SymbolName("Konan_js_unboxDouble")
external public fun unboxDouble(arena: Int, index: Int)
fun unboxDouble(value: JsValue) = unboxDouble(value._arena, value._index)

@Used
@SymbolName("Konan_js_unboxString")
external public fun unboxString(arena: Int, index: Int): Int
fun unboxString(value: JsValue) = unboxString(value._arena, value._index)


@Used
@SymbolName("Konan_js_pushStringToArena")
external public fun pushStringToArena(arena: Arena, pointer: Int, length: Int): Int
fun String.boxString(arena: Arena): JsValue = JsValue(arena, pushStringToArena(arena, stringPointer(this), stringLengthBytes(this)))

fun Int.boxInt(arena: Arena): JsValue = JsValue(arena, pushIntToArena(arena, this))
fun Byte.boxByte(arena: Arena): JsValue = this.toInt().boxInt(arena)
fun Short.boxShort(arena: Arena): JsValue = this.toInt().boxInt(arena)
fun Float.boxFloat(arena: Arena): JsValue = this.toBits().boxInt(arena)
fun Boolean.boxBoolean(arena: Arena): JsValue = (if (this) 1 else 0).boxInt(arena)


@Used
@SymbolName("Konan_js_pushDoubleToArena")
external public fun pushDoubleToArena(arena: Arena, upper: Int, lower: Int ): Int
fun Double.boxDouble(arena: Arena): JsValue = JsValue(arena, pushDoubleToArena(arena, doubleUpper(this), doubleLower(this)))

typealias KtFunction <R> = ((ArrayList<JsValue>)->R)

fun <R> wrapFunction(func: KtFunction<R>): Int {
    val ptr: Long = StableRef.create(func).asCPointer().toLong() 
    return ptr.toInt() // TODO: LP64 unsafe.
}

@Used
@SymbolName("Konan_js_pushFunctionToArena")
external public fun pushFunctionToArena(arena: Arena, value: Int): Int

fun <R> KtFunction<R>.wrapFunction(arena: Arena): JsValue = JsValue(arena, pushFunctionToArena(arena, wrapFunction<R>(this)))

val <R> KtFunction<R>.wrapFunction: JsValue get() {
    val arena = ArenaManager.globalArena
    return JsValue(arena, pushFunctionToArena(arena, wrapFunction<R>(this)))
}

val Any.box: JsValue get() {
    val arena = ArenaManager.globalArena
    return when (this) {
        is Byte -> this.boxByte(arena)
        is Short -> this.boxShort(arena)
        is Int -> this.boxInt(arena)
        is Boolean -> this.boxBoolean(arena)
        is Float -> this.boxFloat(arena)
        is Double -> this.boxDouble(arena)
        is String -> this.boxString(arena)
        else -> error("Unsupported primitive box")
    }
}

@Used
@ExportForCppRuntime("Konan_js_runLambda")
fun runLambda(pointer: Int, argumentsArena: Arena, argumentsArenaSize: Int): Int {
    val arguments = arrayListOf<JsValue>()
    for (i in 0 until argumentsArenaSize) {
        arguments.add(JsValue(argumentsArena, i));
    }
    val args = arguments.map { it.toString() }.joinToString(", ")
    val previousArena = ArenaManager.currentArena
    ArenaManager.currentArena = argumentsArena
    // TODO: LP64 unsafe: wasm32 passes Int, not Long.
    val func = pointer.toLong().toCPointer<CPointed>()!!.asStableRef<KtFunction<Unit/*JsValue*/>>().get()
    val result = func(arguments)

    ArenaManager.currentArena = previousArena
    //return result._index
    return 0 // TODO: don't return anything for now.
}

interface JsValue {
    val _arena: Arena
    val _index: Object
    fun getInt(property: String): Int {
        return getInt(ArenaManager.currentArena, _index, stringPointer(property), stringLengthBytes(property))
    }
    fun getProperty(property: String): JsValue {
        return JsValue(ArenaManager.currentArena, Konan_js_getProperty(ArenaManager.currentArena, _index, stringPointer(property), stringLengthBytes(property)))
    }
}

open class JsValueImpl(override val _arena: Arena, override val _index: Object): JsValue

fun JsValue(_arena: Arena, _index: Object) = JsValueImpl(_arena, _index)

open class JsArray(override val _arena: Arena, override val _index: Object): JsValue {
    constructor(jsValue: JsValue): this(jsValue._arena, jsValue._index)        
    operator fun get(index: Int): JsValue {
        // TODO: we could pass an integer index to index arrays.
        return getProperty(index.toString())
    }
    val _size: Int
        get() = this.getInt("length")
}

@Used
@SymbolName("Konan_js_isNull")
external public fun isNull(arena: Arena, obj: Object): Int;
val JsValue.isNull: Boolean get() = (isNull(this._arena, this._index) == 1)

@Used
@SymbolName("Konan_js_jsPrint")
external public fun jsPrint(arena: Arena, obj: Object);
fun JsValue.jsPrint() = jsPrint(this._arena, this._index) 

@Used
@SymbolName("Konan_js_getInt")
external public fun getInt(arena: Arena, obj: Object, propertyPtr: Pointer, propertyLen: Int): Int;

@Used
@SymbolName("Konan_js_getProperty")
external public fun Konan_js_getProperty(arena: Arena, obj: Object, propertyPtr: Pointer, propertyLen: Int): Int;

@Used
@SymbolName("Konan_js_setFunction")
external public fun setFunction(arena: Arena, obj: Object, propertyName: Pointer, propertyLength: Int , function: Int)

@Used
@SymbolName("Konan_js_setString")
external public fun setString(arena: Arena, obj: Object, propertyName: Pointer, propertyLength: Int, stringPtr: Pointer, stringLength: Int )

fun setter(obj: JsValue, property: String, string: String) {
    setString(obj._arena, obj._index, stringPointer(property), stringLengthBytes(property), stringPointer(string), stringLengthBytes(string))
}

fun setter(obj: JsValue, property: String, lambda: KtFunction<Unit>) {
    val pointer = wrapFunction(lambda);
    setFunction(obj._arena, obj._index, stringPointer(property), stringLengthBytes(property), pointer)
}

fun JsValue.setter(property: String, lambda: KtFunction<Unit>) {
    setter(this, property, lambda)
}

fun JsValue.setter(property: String, string: String) {
    setter(this, property, string)
}

object ArenaManager {
    val globalArena: Arena = allocateArena()
    var currentArena = globalArena
}

val JsNullValue: JsValue by lazy {
    JsValue(ArenaManager.globalArena, pushNullToArena(ArenaManager.globalArena))
}

// TODO: These ones do not belong here.
@Used
@SymbolName("Konan_js_pushWindowToArena")
external public fun pushWindowToArena(arena: Arena): Object

@Used
@SymbolName("Konan_js_pushDocumentToArena")
external public fun pushDocumentToArena(arena: Arena): Object


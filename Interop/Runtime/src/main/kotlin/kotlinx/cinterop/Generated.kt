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

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Byte> CPointer<ByteVarOf<T>>.get(index: Int): T =
        interpretPointed<ByteVarOf<T>>(this.rawValue + index * 1L).value

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Byte> CPointer<ByteVarOf<T>>.set(index: Int, value: T) {
    interpretPointed<ByteVarOf<T>>(this.rawValue + index * 1L).value = value
}

@Suppress("FINAL_UPPER_BOUND")
@JvmName("plus\$Byte")
operator fun <T : ByteVar> CPointer<T>?.plus(index: Int): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 1L)

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Short> CPointer<ShortVarOf<T>>.get(index: Int): T =
        interpretPointed<ShortVarOf<T>>(this.rawValue + index * 2L).value

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Short> CPointer<ShortVarOf<T>>.set(index: Int, value: T) {
    interpretPointed<ShortVarOf<T>>(this.rawValue + index * 2L).value = value
}

@Suppress("FINAL_UPPER_BOUND")
@JvmName("plus\$Short")
operator fun <T : ShortVar> CPointer<T>?.plus(index: Int): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 2L)

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Int> CPointer<IntVarOf<T>>.get(index: Int): T =
        interpretPointed<IntVarOf<T>>(this.rawValue + index * 4L).value

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Int> CPointer<IntVarOf<T>>.set(index: Int, value: T) {
    interpretPointed<IntVarOf<T>>(this.rawValue + index * 4L).value = value
}

@Suppress("FINAL_UPPER_BOUND")
@JvmName("plus\$Int")
operator fun <T : IntVar> CPointer<T>?.plus(index: Int): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4L)

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Long> CPointer<LongVarOf<T>>.get(index: Int): T =
        interpretPointed<LongVarOf<T>>(this.rawValue + index * 8L).value

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Long> CPointer<LongVarOf<T>>.set(index: Int, value: T) {
    interpretPointed<LongVarOf<T>>(this.rawValue + index * 8L).value = value
}

@Suppress("FINAL_UPPER_BOUND")
@JvmName("plus\$Long")
operator fun <T : LongVar> CPointer<T>?.plus(index: Int): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8L)

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Float> CPointer<FloatVarOf<T>>.get(index: Int): T =
        interpretPointed<FloatVarOf<T>>(this.rawValue + index * 4L).value

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Float> CPointer<FloatVarOf<T>>.set(index: Int, value: T) {
    interpretPointed<FloatVarOf<T>>(this.rawValue + index * 4L).value = value
}

@Suppress("FINAL_UPPER_BOUND")
@JvmName("plus\$Float")
operator fun <T : FloatVar> CPointer<T>?.plus(index: Int): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4L)

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Double> CPointer<DoubleVarOf<T>>.get(index: Int): T =
        interpretPointed<DoubleVarOf<T>>(this.rawValue + index * 8L).value

@Suppress("FINAL_UPPER_BOUND")
operator fun <T : Double> CPointer<DoubleVarOf<T>>.set(index: Int, value: T) {
    interpretPointed<DoubleVarOf<T>>(this.rawValue + index * 8L).value = value
}

@Suppress("FINAL_UPPER_BOUND")
@JvmName("plus\$Double")
operator fun <T : DoubleVar> CPointer<T>?.plus(index: Int): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8L)

/* Generated by:

#!/bin/bash

function gen {
echo '@Suppress("FINAL_UPPER_BOUND")'
echo "operator fun <T : $1> CPointer<${1}VarOf<T>>.get(index: Int): T ="
echo "        interpretPointed<${1}VarOf<T>>(this.rawValue + index * ${2}L).value"
echo
echo '@Suppress("FINAL_UPPER_BOUND")'
echo "operator fun <T : $1> CPointer<${1}VarOf<T>>.set(index: Int, value: T) {"
echo "    interpretPointed<${1}VarOf<T>>(this.rawValue + index * ${2}L).value = value"
echo '}'
echo
echo '@Suppress("FINAL_UPPER_BOUND")'
echo "@JvmName(\"plus\\\$$1\")"
echo "operator fun <T : ${1}Var> CPointer<T>?.plus(index: Int): CPointer<T>? ="
echo "        interpretCPointer(this.rawValue + index * ${2}L)"
echo
}

gen Byte 1
gen Short 2
gen Int 4
gen Long 8
gen Float 4
gen Double 8

 */
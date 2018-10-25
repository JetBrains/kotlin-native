/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.androidnative

import kotlinx.cinterop.*

internal class BMPHeader(val rawPtr: NativePtr) {
    inline fun <reified T : CPointed> memberAt(offset: Long): T {
        return interpretPointed<T>(this.rawPtr + offset)
    }

    val magic get() = memberAt<ShortVar>(0).value.toInt()
    val size get() = memberAt<IntVar>(2).value
    val zero get() = memberAt<IntVar>(6).value
    val width get() = memberAt<IntVar>(18).value
    val height get() = memberAt<IntVar>(22).value
    val bits get() = memberAt<ShortVar>(28).value.toInt()
    val data get() = interpretCPointer<ByteVar>(rawPtr + 54) as CArrayPointer<ByteVar>
}
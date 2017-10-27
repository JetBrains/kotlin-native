/*
 * This file is part of the w32api package.
 *
 * Contributors:
 *   Created by Markus Koenig <markus@stber-koenig.de>
 *   Kotlin/Native port by Mike Sinkovsky <msink@permonline.ru>
 *
 * THIS SOFTWARE IS NOT COPYRIGHTED
 *
 * This source code is offered for use in the public domain. You may
 * use, modify or distribute it freely.
 *
 * This code is distributed in the hope that it will be useful but
 * WITHOUT ANY WARRANTY. ALL WARRANTIES, EXPRESS OR IMPLIED ARE HEREBY
 * DISCLAIMED. This includes but is not limited to warranties of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package platform.gdiplus

import kotlinx.cinterop.*

abstract class GdipObject(ptr: COpaquePointer? = null, status: GpStatus = Ok) {
    internal var ptr: COpaquePointer? = ptr
    internal var lastStatus: GpStatus = status

    fun GetLastStatus(): GpStatus {
        val result = lastStatus
        lastStatus = Ok
        return result
    }

    internal fun updateStatus(newStatus: GpStatus): GpStatus {
        if (newStatus != Ok) lastStatus = newStatus
        return newStatus
    }

    abstract fun Clone(): GdipObject?

    abstract fun Dispose()

    fun IsAvailable() = (ptr != null)

/*
    static void* operator new(size_t in_size) {
        return GdipAlloc(in_size)
    }
    static void* operator new[](size_t in_size) {
        return GdipAlloc(in_size)
    }
    static void operator delete(void *in_pVoid) {
        GdipFree(in_pVoid)
    }
    static void operator delete[](void *in_pVoid) {
        GdipFree(in_pVoid)
    }
*/
}

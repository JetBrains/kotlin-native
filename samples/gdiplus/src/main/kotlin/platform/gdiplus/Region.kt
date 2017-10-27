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
import platform.windows.*

/**
 * The Region class describes an area of the display surface. The area can be any shape. In other words,
 * the boundary of the area can be a combination of curved and straight lines. Regions can also be created
 * from the interiors of rectangles, paths, or a combination of these. Regions are used in clipping
 * and hit-testing operations.
 */
class Region : GdipObject {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    companion object {
        /**
         * Creates a GDI+Region object from a GDI  region.
         */
         fun FromHRGN(hrgn: HRGN) = Region(hrgn)
    }

    /**
     * Creates a region that is infinite. This is the default constructor.
     */
    constructor() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateRegion(result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a region that is defined by a rectangle.
     */
    constructor(rect: RectF) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateRegionRect(rect.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a region that is defined by a rectangle.
     */
    constructor(rect: Rect) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateRegionRectI(rect.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a region that is defined by a path (a GraphicsPath object) and has a fill mode that is
     * contained in the GraphicsPath object.
     */
    constructor(path: GraphicsPath?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateRegionPath(path?.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a region that is defined by data obtained from another region.
     */
    constructor(regionData: CValuesRef<BYTEVar>, size: INT) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateRegionRgnData(regionData, size, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a region that is identical to the region that is specified by a handle to a GDI region.
     */
    constructor(hrgn: HRGN) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateRegionHrgn(hrgn, result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDeleteRegion(ptr)
    }

    /**
     * Makes a copy of this Region object and returns the address of the new Region object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneRegion(ptr, result.ptr))
        if (status == Ok) Region(result.value!!, status) else null
    }

    /**
     * Updates this region to the portion of the specified rectangle's interior that does not intersect this region.
     */
    fun Complement(rect: RectF)
        = updateStatus(GdipCombineRegionRect(ptr, rect.ptr, CombineModeComplement))

    /**
     * Updates this region to the portion of the specified rectangle's interior that does not intersect this region.
     */
    fun Complement(rect: Rect)
        = updateStatus(GdipCombineRegionRectI(ptr, rect.ptr, CombineModeComplement))

    /**
     * Updates this region to the portion of another region that does not intersect this region.
     */
    fun Complement(region: Region?)
        = updateStatus(GdipCombineRegionRegion(ptr, region?.ptr, CombineModeComplement))

    /**
     * Updates this region to the portion of the specified path's interior that does not intersect this region.
     */
    fun Complement(path: GraphicsPath?)
        = updateStatus(GdipCombineRegionPath(ptr, path?.ptr, CombineModeComplement))

    /**
     * Determines whether this region is equal to a specified region.
     */
    fun Equals(region: Region?, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsEqualRegion(ptr, region?.ptr, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Updates this region to the portion of itself that does not intersect the specified rectangle's interior.
     */
    fun Exclude(rect: RectF)
        = updateStatus(GdipCombineRegionRect(ptr, rect.ptr, CombineModeExclude))

    /**
     * Updates this region to the portion of itself that does not intersect the specified rectangle's interior.
     */
    fun Exclude(rect: Rect)
        = updateStatus(GdipCombineRegionRectI(ptr, rect.ptr, CombineModeExclude))

    /**
     * Updates this region to the portion of itself that does not intersect another region.
     */
    fun Exclude(region: Region?)
        = updateStatus(GdipCombineRegionRegion(ptr, region?.ptr, CombineModeExclude))

    /**
     * Updates this region to the portion of itself that does not intersect the specified path's interior.
     */
    fun Exclude(path: GraphicsPath?)
        = updateStatus(GdipCombineRegionPath(ptr, path?.ptr, CombineModeExclude))

    /**
     * Gets a rectangle that encloses this region.
     */
    fun GetBounds(rect: RectF, graphics: Graphics?)
        = updateStatus(GdipGetRegionBounds(ptr, graphics?.ptr, rect.ptr))

    /**
     * Gets a rectangle that encloses this region.
     */
    fun GetBounds(rect: Rect, graphics: Graphics?)
        = updateStatus(GdipGetRegionBoundsI(ptr, graphics?.ptr, rect.ptr))

    /**
     * Gets data that describes this region.
     */
    fun GetData(buffer: CValuesRef<BYTEVar>, bufferSize: UINT, sizeFilled: UINTVar)
        = updateStatus(GdipGetRegionData(ptr, buffer, bufferSize, sizeFilled.ptr))

    /**
     * Gets the number of bytes of data that describes this region.
     */
    fun GetDataSize() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetRegionDataSize(ptr, result.ptr))
        result.value
    }

    /**
     * Creates a GDI region from this region.
     */
    fun GetHRGN(graphics: Graphics?): HRGN? = memScoped {
        val result = alloc<HRGNVar>().apply { value = null }
        updateStatus(GdipGetRegionHRgn(ptr, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Gets an array of rectangles that approximate this region. The region is transformed by a specified
     * matrix before the rectangles are calculated.
     */
    fun GetRegionScans(matrix: Matrix?, rects: RectF, count: INTVar)
        = updateStatus(GdipGetRegionScans(ptr, rects.ptr, count.ptr, matrix?.ptr))

    /**
     * Gets an array of rectangles that approximate this region. The region is transformed by a specified
     * matrix before the rectangles are calculated.
     */
    fun GetRegionScans(matrix: Matrix?, rects: Rect, count: INTVar)
        = updateStatus(GdipGetRegionScansI(ptr, rects.ptr, count.ptr, matrix?.ptr))

    /**
     * Gets the number of rectangles that approximate this region. The region is transformed by a
     * specified matrix before the rectangles are calculated.
     */
    fun GetRegionScansCount(matrix: Matrix?) = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetRegionScansCount(ptr, result.ptr, matrix?.ptr))
        result.value
    }

    /**
     * Updates this region to the portion of itself that intersects the specified rectangle's interior.
     */
    fun Intersect(rect: RectF)
        = updateStatus(GdipCombineRegionRect(ptr, rect.ptr, CombineModeIntersect))

    /**
     * Updates this region to the portion of itself that intersects the specified rectangle's interior.
     */
    fun Intersect(rect: Rect)
        = updateStatus(GdipCombineRegionRectI(ptr, rect.ptr, CombineModeIntersect))

    /**
     * Updates this region to the portion of itself that intersects another region.
     */
    fun Intersect(region: Region?)
        = updateStatus(GdipCombineRegionRegion(ptr, region?.ptr, CombineModeIntersect))

    /**
     * Updates this region to the portion of itself that intersects the specified path's interior.
     */
    fun Intersect(path: GraphicsPath?)
        = updateStatus(GdipCombineRegionPath(ptr, path?.ptr, CombineModeIntersect))

    /**
     * Determines whether this region is empty.
     */
    fun IsEmpty(graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsEmptyRegion(ptr, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether this region is infinite.
     */
    fun IsInfinite(graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsInfiniteRegion(ptr, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a point is inside this region.
     */
    fun IsVisible(x: REAL, y: REAL, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionPoint(ptr, x, y, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a point is inside this region.
     */
    fun IsVisible(x: INT, y: INT, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionPointI(ptr, x, y, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a point is inside this region.
     */
    fun IsVisible(point: PointF, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionPoint(ptr, point.X, point.Y, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a point is inside this region.
     */
    fun IsVisible(point: Point, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionPointI(ptr, point.X, point.Y, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a rectangle intersects this region.
     */
    fun IsVisible(x: REAL, y: REAL, width: REAL, height: REAL, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionRect(ptr, x, y, width, height, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a rectangle intersects this region.
     */
    fun IsVisible(x: INT, y: INT, width: INT, height: INT, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionRectI(ptr, x, y, width, height, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a rectangle intersects this region.
     */
    fun IsVisible(rect: RectF, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionRect(ptr, rect.X, rect.Y, rect.Width, rect.Height, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a rectangle intersects this region.
     */
    fun IsVisible(rect: Rect, graphics: Graphics?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRegionRectI(ptr, rect.X, rect.Y, rect.Width, rect.Height, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Updates this region to an empty region. In other words, the region occupies no space on the display device.
     */
    fun MakeEmpty()
        = updateStatus(GdipSetEmpty(ptr))

    /**
     * Updates this region to an infinite region.
     */
    fun MakeInfinite()
        = updateStatus(GdipSetInfinite(ptr))

    /**
     * Transforms this region by multiplying each of its data points by a specified matrix.
     */
    fun Transform(matrix: Matrix?)
        = updateStatus(GdipTransformRegion(ptr, matrix?.ptr))

    /**
     * Offsets this region by specified amounts in the horizontal and vertical directions.
     */
    fun Translate(dx: REAL, dy: REAL)
        = updateStatus(GdipTranslateRegion(ptr, dx, dy))

    /**
     * Offsets this region by specified amounts in the horizontal and vertical directions.
     */
    fun Translate(dx: INT, dy: INT)
        = updateStatus(GdipTranslateRegionI(ptr, dx, dy))

    /**
     * Updates this region to all portions (intersecting and nonintersecting) of itself and all portions
     * of the specified rectangle's interior.
     */
    fun Union(rect: RectF)
        = updateStatus(GdipCombineRegionRect(ptr, rect.ptr, CombineModeUnion))

    /**
     * Updates this region to all portions (intersecting and nonintersecting) of itself and all portions
     * of the specified rectangle's interior.
     */
    fun Union(rect: Rect)
        = updateStatus(GdipCombineRegionRectI(ptr, rect.ptr, CombineModeUnion))

    /**
     * Updates this region to all portions (intersecting and nonintersecting) of itself and all portions
     * of another region.
     */
    fun Union(region: Region?)
        = updateStatus(GdipCombineRegionRegion(ptr, region?.ptr, CombineModeUnion))

    /**
     * Updates this region to all portions (intersecting and nonintersecting) of itself and all portions
     * of the specified path's interior.
     */
    fun Union(path: GraphicsPath?)
        = updateStatus(GdipCombineRegionPath(ptr, path?.ptr, CombineModeUnion))

    /**
     * Updates this region to the nonintersecting portions of itself and the specified rectangle's interior.
     */
    fun Xor(rect: RectF)
        = updateStatus(GdipCombineRegionRect(ptr, rect.ptr, CombineModeXor))

    /**
     * Updates this region to the nonintersecting portions of itself and the specified rectangle's interior.
     */
    fun Xor(rect: Rect)
        = updateStatus(GdipCombineRegionRectI(ptr, rect.ptr, CombineModeXor))

    /**
     * Updates this region to the nonintersecting portions of itself and another region.
     */
    fun Xor(region: Region?)
        = updateStatus(GdipCombineRegionRegion(ptr, region?.ptr, CombineModeXor))

    /**
     * Updates this region to the nonintersecting portions of itself and the specified path's interior.
     */
    fun Xor(path: GraphicsPath?)
       = updateStatus(GdipCombineRegionPath(ptr, path?.ptr, CombineModeXor))
}

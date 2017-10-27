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
 * A Pen object is a Windows GDI+ object used to draw lines and curves.
 */
class Pen : GdipObject {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a Pen object that uses a specified color and width.
     */
    constructor(color: Color, width: REAL = 1.0f) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePen1(color.Value, width, UnitWorld, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Pen object that uses the attributes of a brush and a real number to set the width
     * of this Pen object.
     */
    constructor(brush: Brush?, width: REAL = 1.0f) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePen2(brush?.ptr, width, UnitWorld, result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDeletePen(ptr)
    }

    /**
     * Copies a Pen object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipClonePen(ptr, result.ptr))
        if (status == Ok) Pen(result.value!!, status) else null
    }

    /**
     * Gets the alignment currently set for this Pen object.
     */
    fun GetAlignment() = memScoped {
        val result = alloc<PenAlignmentVar>().apply { value = PenAlignmentCenter }
        updateStatus(GdipGetPenMode(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the Brush object that is currently set for this Pen object.
     */
    fun GetBrush(): Brush? {
        //TODO where is the pen brush allocated (static,member,new,other)?
        // GdipGetPenBrushFill just returns a GpBrush*
        updateStatus(NotImplemented)
        return null
    }

    /**
     * Gets the color currently set for this Pen object.
     */
    fun GetColor(color: Color)
        = updateStatus(GdipGetPenColor(ptr, color.memberAt<ARGBVar>(0).ptr))

    /**
     * Gets the compound array currently set for this Pen object.
     */
    fun GetCompoundArray(compoundArray: REALVar, count: INT)
        = updateStatus(GdipGetPenCompoundArray(ptr, compoundArray.ptr, count))

    /**
     * Gets the number of elements in a compound array.
     */
    fun GetCompoundArrayCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetPenCompoundCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the custom end cap currently set for this Pen object.
     */
    fun GetCustomEndCap(customCap: CustomLineCap)
        = updateStatus(GdipGetPenCustomEndCap(ptr, customCap.ptr?.reinterpret()))

    /**
     * Gets the custom start cap currently set for this Pen object.
     */
    fun GetCustomStartCap(customCap: CustomLineCap)
        = updateStatus(GdipGetPenCustomStartCap(ptr, customCap.ptr?.reinterpret()))

    /**
     * Gets the dash cap style currently set for this Pen object.
     */
    fun GetDashCap() = memScoped {
        val result = alloc<DashCapVar>().apply { value = DashCapFlat }
        updateStatus(GdipGetPenDashCap197819(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the distance from the start of the line to the start of the first space in a dashed line.
     */
    fun GetDashOffset() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetPenDashOffset(ptr, result.ptr))
        result.value
    }

    /**
     * Gets an array of custom dashes and spaces currently set for this Pen object.
     */
    fun GetDashPattern(dashArray: REALVar, count: INT)
        = updateStatus(GdipGetPenDashArray(ptr, dashArray.ptr, count))

    /**
     * Gets the number of elements in a dash pattern array.
     */
    fun GetDashPatternCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetPenDashCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the dash style currently set for this Pen object.
     */
    fun GetDashStyle() = memScoped {
        val result = alloc<DashStyleVar>().apply { value = DashStyleSolid }
        updateStatus(GdipGetPenDashStyle(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the end cap currently set for this Pen object.
     */
    fun GetEndCap() = memScoped {
        val result = alloc<LineCapVar>().apply { value = LineCapFlat }
        updateStatus(GdipGetPenEndCap(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the line join style currently set for this Pen object.
     */
    fun GetLineJoin() = memScoped {
        val result = alloc<LineJoinVar>().apply { value = LineJoinMiter }
        updateStatus(GdipGetPenLineJoin(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the miter length currently set for this Pen object.
     */
    fun GetMiterLimit() = memScoped {
        val result = alloc<REALVar>().apply { value = 10.0f }
        updateStatus(GdipGetPenMiterLimit(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the type currently set for this Pen object.
     */
    fun GetPenType() = memScoped {
        val result = alloc<PenTypeVar>().apply { value = PenTypeUnknown }
        updateStatus(GdipGetPenFillType(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the start cap currently set for this Pen object.
     */
    fun GetStartCap() = memScoped {
        val result = alloc<LineCapVar>().apply { value = LineCapFlat }
        updateStatus(GdipGetPenStartCap(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the world transformation matrix currently set for this Pen object.
     */
    fun GetTransform(matrix: Matrix?)
        = updateStatus(GdipGetPenTransform(ptr, matrix?.ptr))

    /**
     * Gets the width currently set for this Pen object.
     */
    fun GetWidth() = memScoped {
        val result = alloc<REALVar>().apply { value = 1.0f }
        updateStatus(GdipGetPenWidth(ptr, result.ptr))
        result.value
    }

    /**
     * Updates the world transformation matrix of this Pen object with the product of itself and another matrix.
     */
    fun MultiplyTransform(matrix: Matrix?, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipMultiplyPenTransform(ptr, matrix?.ptr, order))

    /**
     * Sets the world transformation matrix of this Pen object to the identity matrix.
     */
    fun ResetTransform()
        = updateStatus(GdipResetPenTransform(ptr))

    /**
     * Updates the world transformation matrix of this Pen object with the product of itself and a rotation matrix.
     */
    fun RotateTransform(angle: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipRotatePenTransform(ptr, angle, order))

    /**
     * Sets the Pen object's world transformation matrix equal to the product of itself and a scaling matrix.
     */
    fun ScaleTransform(sx: REAL, sy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipScalePenTransform(ptr, sx, sy, order))

    /**
     * Sets the alignment for this Pen object relative to the line.
     */
    fun SetAlignment(penAlignment: PenAlignment)
        = updateStatus(GdipSetPenMode(ptr, penAlignment))

    /**
     * Sets the Brush object that a pen uses to fill a line.
     */
    fun SetBrush(brush: Brush?)
        = updateStatus(GdipSetPenBrushFill(ptr, brush?.ptr))

    /**
     * Sets the color for this Pen object.
     */
    fun SetColor(color: Color)
        = updateStatus(GdipSetPenColor(ptr, color.Value))

    /**
     * Sets the compound array for this Pen object.
     */
    fun SetCompoundArray(compoundArray: REALVar, count: INT)
        = updateStatus(GdipSetPenCompoundArray(ptr, compoundArray.ptr, count))

    /**
     * Sets the custom end cap for this Pen object.
     */
    fun SetCustomEndCap(customCap: CustomLineCap?)
        = updateStatus(GdipSetPenCustomEndCap(ptr, customCap?.ptr))

    /**
     * Sets the custom start cap for this Pen object.
     */
    fun SetCustomStartCap(customCap: CustomLineCap?)
        = updateStatus(GdipSetPenCustomStartCap(ptr, customCap?.ptr))

    /**
     * Sets the dash cap style for this Pen object.
     */
    fun SetDashCap(dashCap: DashCap)
        = updateStatus(GdipSetPenDashCap197819(ptr, dashCap))

    /**
     * Sets the distance from the start of the line to the start of the first dash in a dashed line.
     */
    fun SetDashOffset(dashOffset: REAL)
        = updateStatus(GdipSetPenDashOffset(ptr, dashOffset))

    /**
     * Sets an array of custom dashes and spaces for this Pen object.
     */
    fun SetDashPattern(dashArray: REALVar, count: INT)
        = updateStatus(GdipSetPenDashArray(ptr, dashArray.ptr, count))

    /**
     * Sets the dash style for this Pen object.
     */
    fun SetDashStyle(dashStyle: DashStyle)
        = updateStatus(GdipSetPenDashStyle(ptr, dashStyle))

    /**
     * Sets the end cap for this Pen object.
     */
    fun SetEndCap(endCap: LineCap)
        = updateStatus(GdipSetPenEndCap(ptr, endCap))

    /**
     * Sets the cap styles for the start, end, and dashes in a line drawn with this pen.
     */
    fun SetLineCap(startCap: LineCap, endCap: LineCap, dashCap: DashCap)
        = updateStatus(GdipSetPenLineCap197819(ptr, startCap, endCap, dashCap))

    /**
     * Sets the line join for this Pen object.
     */
    fun SetLineJoin(lineJoin: LineJoin)
        = updateStatus(GdipSetPenLineJoin(ptr, lineJoin))

    /**
     * Sets the miter limit of this Pen object.
     */
    fun SetMiterLimit(miterLimit: REAL)
        = updateStatus(GdipSetPenMiterLimit(ptr, miterLimit))

    /**
     * Sets the start cap for this Pen object.
     */
    fun SetStartCap(startCap: LineCap)
        = updateStatus(GdipSetPenStartCap(ptr, startCap))

    /**
     * Sets the world transformation of this Pen object.
     */
    fun SetTransform(matrix: Matrix?)
        = updateStatus(GdipSetPenTransform(ptr, matrix?.ptr))

    /**
     * Sets the width for this Pen object.
     */
    fun SetWidth(width: REAL)
        = updateStatus(GdipSetPenWidth(ptr, width))
}

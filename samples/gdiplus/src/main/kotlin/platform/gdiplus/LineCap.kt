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
 * The CustomLineCap class encapsulates a custom line cap. A line cap defines the style of graphic used
 * to draw the ends of a line. It can be various shapes, such as a square, circle, or diamond.
 * A custom line cap is defined by the path that draws it. The path is drawn by using a Pen object to draw
 * the outline of a shape or by using a Brush object to fill the interior. The cap can be used on either
 * or both ends of the line. Spacing can be adjusted between the end caps and the line.
 */
open class CustomLineCap : GdipObject {
    protected constructor(ptr: COpaquePointer? = null, status: GpStatus = Ok) : super(ptr, status) {}

    /**
     * Creates a CustomLineCap object.
     */
    constructor(fillPath: GraphicsPath?, strokePath: GraphicsPath?,
                baseCap: LineCap = LineCapFlat, baseInset: REAL = 0.0f) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateCustomLineCap(fillPath?.ptr, strokePath?.ptr, baseCap, baseInset, result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDeleteCustomLineCap(ptr)
    }

    /**
     * Copies the contents of the existing object into a new CustomLineCap object.
     */
    override open fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneCustomLineCap(ptr, result.ptr))
        if (status == Ok) CustomLineCap(result.value!!, status) else null
    }

    /**
     * Gets the style of the base cap. The base cap is a LineCap object used as a cap at the end of a line along with this CustomLineCap object.
     */
    fun GetBaseCap() = memScoped {
        val result = alloc<LineCapVar>().apply { value = LineCapFlat }
        updateStatus(GdipGetCustomLineCapBaseCap(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the distance between the base cap to the start of the line.
     */
    fun GetBaseInset() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetCustomLineCapBaseInset(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the end cap styles for both the start line cap and the end line cap.
     * Line caps are LineCap objects that end the individual lines within a path.
     */
    fun GetStrokeCaps(startCap: LineCapVar, endCap: LineCapVar)
        = updateStatus(GdipGetCustomLineCapStrokeCaps(ptr, startCap.ptr, endCap.ptr))

    /**
     * Returns the style of LineJoin used to join multiple lines in the same GraphicsPath object.
     */
    fun GetStrokeJoin() = memScoped {
        val result = alloc<LineJoinVar>().apply { value = LineJoinMiter }
        updateStatus(GdipGetCustomLineCapStrokeJoin(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the value of the scale width. This is the amount to scale the custom line cap relative to
     * the width of the Pen object used to draw a line. The default value of 1.0 does not scale the line cap.
     */
    fun GetWidthScale() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetCustomLineCapWidthScale(ptr, result.ptr))
        result.value
    }

    /**
     * Sets the LineCap that appears as part of this CustomLineCap at the end of a line.
     */
    fun SetBaseCap(baseCap: LineCap)
        = updateStatus(GdipSetCustomLineCapBaseCap(ptr, baseCap))

    /**
     * Sets the base inset value of this custom line cap. This is the distance between the end of a line
     * and the base cap.
     */
    fun SetBaseInset(inset: REAL)
        = updateStatus(GdipSetCustomLineCapBaseInset(ptr, inset))

    /**
     * Sets the LineCap object used to start and end lines within the GraphicsPath object that defines
     * this CustomLineCap object.
     */
    fun SetStrokeCap(strokeCap: LineCap)
        = updateStatus(GdipSetCustomLineCapStrokeCaps(ptr, strokeCap, strokeCap))

    /**
     * Sets the LineCap objects used to start and end lines within the GraphicsPath object that defines
     * this CustomLineCap object.
     */
    fun SetStrokeCaps(startCap: LineCap, endCap: LineCap)
        = updateStatus(GdipSetCustomLineCapStrokeCaps(ptr, startCap, endCap))

    /**
     * Sets the style of line join for the stroke. The line join specifies how two lines that intersect
     * within the GraphicsPath object that makes up the custom line cap are joined.
     */
    fun SetStrokeJoin(lineJoin: LineJoin)
        = updateStatus(GdipSetCustomLineCapStrokeJoin(ptr, lineJoin))

    /**
     * Sets the value of the scale width. This is the amount to scale the custom line cap relative to
     * the width of the Pen used to draw lines. The default value of 1.0 does not scale the line cap.
     */
    fun SetWidthScale(widthScale: REAL)
        = updateStatus(GdipSetCustomLineCapWidthScale(ptr, widthScale))
}

/**
 * The AdjustableArrowCap class is a subclass of the CustomLineCap.
 * This class builds a line cap that looks like an arrow.
 */
class AdjustableArrowCap : CustomLineCap {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates an adjustable arrow line cap with the specified height and width.
     * The arrow line cap can be filled or nonfilled. The middle inset defaults to zero.
     */
    constructor(height: REAL, width: REAL, isFilled: BOOL) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateAdjustableArrowCap(height, width, isFilled, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Copies the contents of the existing object into a new CustomLineCap object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneCustomLineCap(ptr, result.ptr))
        if (status == Ok) AdjustableArrowCap(result.value!!, status) else null
    }

    /**
     * Gets the height of the arrow cap. The height is the distance from the base of the arrow to its vertex.
     */
    fun GetHeight() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetAdjustableArrowCapHeight(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the value of the inset. The middle inset is the number of units that the midpoint of
     * the base shifts towards the vertex.
     */
    fun GetMiddleInset() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetAdjustableArrowCapMiddleInset(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the width of the arrow cap. The width is the distance between the endpoints of the base of the arrow.
     */
    fun GetWidth() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetAdjustableArrowCapWidth(ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether the arrow cap is filled.
     */
    fun IsFilled() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipGetAdjustableArrowCapFillState(ptr, result.ptr))
        result.value
    }

    /**
     * Sets the fill state of the arrow cap. If the arrow cap is not filled, only the outline is drawn.
     */
    fun SetFillState(isFilled: BOOL)
        = updateStatus(GdipSetAdjustableArrowCapFillState(ptr, isFilled))

    /**
     * Sets the height of the arrow cap. This is the distance from the base of the arrow to its vertex.
     */
    fun SetHeight(height: REAL)
        = updateStatus(GdipSetAdjustableArrowCapHeight(ptr, height))

    /**
     * Sets the number of units that the midpoint of the base shifts towards the vertex.
     */
    fun SetMiddleInset(middleInset: REAL)
        = updateStatus(GdipSetAdjustableArrowCapMiddleInset(ptr, middleInset))

    /**
     * Sets the width of the arrow cap. The width is the distance between the endpoints of the base of the arrow.
     */
    fun SetWidth(width: REAL)
        = updateStatus(GdipSetAdjustableArrowCapWidth(ptr, width))
}

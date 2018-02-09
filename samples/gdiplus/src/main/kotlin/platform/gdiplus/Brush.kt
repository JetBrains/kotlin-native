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
 * The Brush class is an abstract base class that defines a Brush object. A Brush object is used to paint
 * the interior of graphics shapes, such as rectangles, ellipses, pies, polygons, and paths.
 */
abstract class Brush : GdipObject {
    protected constructor(ptr: COpaquePointer? = null, status: GpStatus = Ok) : super(ptr, status) {}

    /**
     *
     */
    override fun Dispose() {
        GdipDeleteBrush(ptr)
    }

    /**
     * Gets the type of this brush.
     */
    fun GetType() = memScoped {
        val result = alloc<BrushTypeVar>().apply { value = BrushTypeSolidColor }
        updateStatus(GdipGetBrushType(ptr, result.ptr))
        result.value
    }
}

/**
 * The SolidBrush class defines a solid color Brush object. A Brush object is used to fill in shapes similar to
 * the way a paint brush can paint the inside of a shape. This class inherits from the Brush abstract base class.
 */
class SolidBrush : Brush {
    protected constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a SolidBrush object based on a color.
     */
    constructor(color: Color) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateSolidFill(color.Value, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a new Brush object based on this brush
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneBrush(ptr, result.ptr))
        if (status == Ok) SolidBrush(result.value!!, status) else null
    }

    /**
     * Gets the color of this solid brush.
     */
    fun GetColor(color: Color)
        = updateStatus(GdipGetSolidFillColor(ptr, color.memberAt<ARGBVar>(0).ptr))

    /**
     * Sets the color of this solid brush.
     */
    fun SetColor(color: Color)
        = updateStatus(GdipSetSolidFillColor(ptr, color.Value))
}

/**
 * This HatchBrush class defines a rectangular brush with a hatch style, a foreground color, and a
 * background color. There are six hatch styles. The foreground color defines the color of the hatch lines;
 * the background color defines the color over which the hatch lines are drawn.
 */
class HatchBrush : Brush {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a HatchBrush object based on a hatch style, a foreground color, and a background color.
     */
    constructor(hatchStyle: HatchStyle, foreColor: Color, backColor: Color? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateHatchBrush(hatchStyle, foreColor.Value, backColor?.Value ?: 0, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a new Brush object based on this brush
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneBrush(ptr, result.ptr))
        if (status == Ok) HatchBrush(result.value!!, status) else null
    }

    /**
     * Gets the background color of this hatch brush.
     */
    fun GetBackgroundColor(color: Color)
        = updateStatus(GdipGetHatchBackgroundColor(ptr, color.memberAt<ARGBVar>(0).ptr))

    /**
     * Gets the foreground color of this hatch brush.
     */
    fun GetForegroundColor(color: Color)
        = updateStatus(GdipGetHatchForegroundColor(ptr, color.memberAt<ARGBVar>(0).ptr))

    /**
     * Gets the hatch style of this hatch brush.
     */
    fun GetHatchStyle() = memScoped {
        val result = alloc<HatchStyleVar>()
        updateStatus(GdipGetHatchStyle(ptr, result.ptr))
        result.value
    }
}

/**
 * The TextureBrush class defines a Brush object that contains an Image object that is used for the fill.
 * The fill image can be transformed by using the local Matrix object contained in the Brush object.
 */
class TextureBrush : Brush {
    protected constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a TextureBrush object based on an image and a wrap mode. The size of the brush defaults
     * to the size of the image, so the entire image is used by the brush.
     */
    constructor(image: Image?, wrapMode: WrapMode = WrapModeTile) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateTexture(image?.ptr, wrapMode, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a TextureBrush object based on an image, a wrap mode, and a defining set of coordinates.
     */
    constructor(image: Image?, wrapMode: WrapMode, dstX: REAL, dstY: REAL, dstWidth: REAL, dstHeight: REAL) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateTexture2(image?.ptr, wrapMode, dstX, dstY, dstWidth, dstHeight, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a TextureBrush object based on an image, a wrap mode, and a defining set of coordinates.
     */
    constructor(image: Image?, wrapMode: WrapMode, dstX: INT, dstY: INT, dstWidth: INT, dstHeight: INT) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateTexture2I(image?.ptr, wrapMode, dstX, dstY, dstWidth, dstHeight, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a TextureBrush object based on an image, a wrap mode, and a defining rectangle.
     */
    constructor(image: Image?, wrapMode: WrapMode, dstRect: RectF) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateTexture2(image?.ptr, wrapMode, dstRect.X, dstRect.Y,
                                            dstRect.Width, dstRect.Height, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a TextureBrush object based on an image, a wrap mode, and a defining rectangle.
     */
    constructor(image: Image?, wrapMode: WrapMode, dstRect: Rect) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateTexture2I(image?.ptr, wrapMode, dstRect.X, dstRect.Y,
                                             dstRect.Width, dstRect.Height, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a TextureBrush object based on an image, a defining rectangle, and a set of image properties.
     */
    constructor(image: Image?, dstRect: RectF, imageAttributes: ImageAttributes? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateTextureIA(image?.ptr, imageAttributes?.ptr, dstRect.X, dstRect.Y,
                                             dstRect.Width, dstRect.Height, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a TextureBrush object based on an image, a defining rectangle, and a set of image properties.
     */
    constructor(image: Image?, dstRect: Rect, imageAttributes: ImageAttributes? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateTextureIAI(image?.ptr, imageAttributes?.ptr, dstRect.X,
                                              dstRect.Y, dstRect.Width, dstRect.Height, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a new Brush object based on this brush
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneBrush(ptr, result.ptr))
        if (status == Ok) TextureBrush(result.value!!, status) else null
    }

    /**
     * Gets a pointer to the Image object that is defined by this texture brush.
     */
    fun GetImage(): Image? {
      //TODO where is the Image allocated (static,member,new,other)?
      // GdipGetTextureImage just returns a GpImage*
      updateStatus(NotImplemented)
      return null
    }

    /**
     * Gets the transformation matrix of this texture brush.
     */
    fun GetTransfrom(matrix: Matrix?)
        = updateStatus(GdipGetTextureTransform(ptr, matrix?.ptr))

    /**
     * Gets the wrap mode currently set for this texture brush.
     */
    fun GetWrapMode() = memScoped {
        val result = alloc<WrapModeVar>().apply { value = WrapModeTile }
        updateStatus(GdipGetTextureWrapMode( ptr, result.ptr))
        result.value
    }

    /**
     * Updates this brush's transformation matrix with the product of itself and another matrix.
     */
    fun MultiplyTransform(matrix: Matrix?, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipMultiplyTextureTransform(ptr, matrix?.ptr, order))

    /**
     * Resets the transformation matrix of this texture brush to the identity matrix. This means that no transformation takes place.
     */
    fun ResetTransform()
        = updateStatus(GdipResetTextureTransform(ptr))

    /**
     * Updates this texture brush's current transformation matrix with the product of itself and a rotation matrix.
     */
    fun RotateTransform(angle: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipRotateTextureTransform(ptr, angle, order))

    /**
     * Updates this texture brush's current transformation matrix with the product of itself and a scaling matrix.
     */
    fun ScaleTransform(sx: REAL, sy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipScaleTextureTransform(ptr, sx, sy, order))

    /**
     * Sets the transformation matrix of this texture brush.
     */
    fun SetTransform(matrix: Matrix?)
        = updateStatus(GdipSetTextureTransform(ptr, matrix?.ptr))

    /**
     * Sets the wrap mode of this texture brush.
     */
    fun SetWrapMode(wrapMode: WrapMode)
        = updateStatus(GdipSetTextureWrapMode(ptr, wrapMode))

    /**
     * Updates this brush's current transformation matrix with the product of itself and a translation matrix.
     */
    fun TranslateTransform(dx: REAL, dy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipTranslateTextureTransform(ptr, dx, dy, order))
}

/**
 * The LinearGradientBrush class defines a brush that paints a color gradient in which the color changes
 * evenly from the starting boundary line of the linear gradient brush to the ending boundary line of the
 * linear gradient brush. The boundary lines of a linear gradient brush are two parallel straight lines.
 * The color gradient is perpendicular to the boundary lines of the linear gradient brush, changing
 * gradually across the stroke from the starting boundary line to the ending boundary line.
 * The color gradient has one color at the starting boundary line and another color at the ending boundary
 * line.
 */
class LinearGradientBrush : Brush {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a LinearGradientBrush object from a set of boundary points and boundary colors.
     */
    constructor(point1: PointF, point2: PointF, color1: Color, color2: Color) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateLineBrush(point1.ptr, point2.ptr, color1.Value, color2.Value, WrapModeTile, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a LinearGradientBrush object from a set of boundary points and boundary colors.
     */
    constructor(point1: Point, point2: Point, color1: Color, color2: Color) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateLineBrushI(point1.ptr, point2.ptr, color1.Value, color2.Value, WrapModeTile, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a LinearGradientBrush object based on a rectangle and mode of direction.
     */
    constructor(rect: RectF, color1: Color, color2: Color, mode: LinearGradientMode) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateLineBrushFromRect(rect.ptr, color1.Value, color2.Value, mode, WrapModeTile, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a LinearGradientBrush object based on a rectangle and mode of direction.
     */
    constructor(rect: Rect, color1: Color, color2: Color, mode: LinearGradientMode) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateLineBrushFromRectI(rect.ptr, color1.Value, color2.Value, mode, WrapModeTile, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a LinearGradientBrush object from a rectangle and angle of direction.
     */
    constructor(rect: RectF, color1: Color, color2: Color, angle: REAL, isAngleScalable: Boolean = false) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateLineBrushFromRectWithAngle(rect.ptr, color1.Value, color2.Value, angle,
                            if (isAngleScalable) TRUE else FALSE, WrapModeTile, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a LinearGradientBrush object from a rectangle and angle of direction.
     */
    constructor(rect: Rect, color1: Color, color2: Color, angle: REAL, isAngleScalable: Boolean = false) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateLineBrushFromRectWithAngleI(rect.ptr, color1.Value, color2.Value, angle,
                            if (isAngleScalable) TRUE else FALSE, WrapModeTile, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a new Brush object based on this brush
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneBrush(ptr, result.ptr))
        if (status == Ok) LinearGradientBrush(result.value!!, status) else null
    }

    /**
     * Gets the blend factors and their corresponding blend positions from a LinearGradientBrush object.
     */
    fun GetBlend(blendFactors: CValuesRef<REALVar>, blendPositions: CValuesRef<REALVar>, count: INT)
        = updateStatus(GdipGetLineBlend(ptr, blendFactors, blendPositions, count))

    /**
     * Gets the number of blend factors currently set for this LinearGradientBrush object.
     */
    fun GetBlendCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetLineBlendCount(ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether gamma correction is enabled for this LinearGradientBrush object.
     */
    fun GetGammaCorrection() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipGetLineGammaCorrection(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the number of colors currently set to be interpolated for this linear gradient brush.
     */
    fun GetInterpolationColorCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetLinePresetBlendCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the colors currently set to be interpolated for this linear gradient brush and their corresponding
     * blend positions.
     */
    fun GetInterpolationColors(presetColors: CPointer<Color>, blendPositions: CValuesRef<REALVar>, count: INT): GpStatus {
        if (count <= 0)
            return updateStatus(InvalidParameter)
        val presetArgb: CPointer<ARGBVar>?
            = GdipAlloc((count * sizeOf<ARGBVar>()).signExtend())?.reinterpret()
        if (presetArgb == null)
            return updateStatus(OutOfMemory)
        val status = updateStatus(GdipGetLinePresetBlend(ptr, presetArgb, blendPositions, count))
        for (i in 0 until count)
            presetColors[i].Value  = presetArgb[i]
        GdipFree(presetArgb)
        return status
    }

    /**
     * Gets the starting color and ending color of this linear gradient brush.
     */
    fun GetLinearColors(colors: CPointer<Color>): GpStatus = memScoped {
        val colorsArgb = allocArray<ARGBVar>(2)
        val status = updateStatus(GdipGetLineColors(ptr, colorsArgb))
        colors[0].Value = colorsArgb[0]
        colors[1].Value = colorsArgb[1]
        return status
    }

    /**
     * Gets the rectangle that defines the boundaries of the gradient.
     */
    fun GetRectangle(rect: RectF)
        = updateStatus(GdipGetLineRect(ptr, rect.ptr))

    /**
     * Gets the rectangle that defines the boundaries of the gradient.
     */
    fun GetRectangle(rect: Rect)
        = updateStatus(GdipGetLineRectI(ptr, rect.ptr))

    /**
     * Gets the transformation matrix of this linear gradient brush.
     */
    fun GetTransform(matrix: Matrix?)
        = updateStatus(GdipGetLineTransform(ptr, matrix?.ptr))

    /**
     * Gets the wrap mode for this brush. The wrap mode determines how an area is tiled when it is painted
     * with a brush.
     */
    fun GetWrapMode(): WrapMode = memScoped {
        val result = alloc<WrapModeVar>().apply { value = WrapModeTile }
        updateStatus(GdipGetLineWrapMode(ptr, result.ptr))
        return result.value
    }

    /**
     * Updates this brush's transformation matrix with the product of itself and another matrix.
     */
    fun MultiplyTransform(matrix: Matrix?, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipMultiplyLineTransform(ptr, matrix?.ptr, order))

    /**
     * Resets the transformation matrix of this linear gradient brush to the identity matrix.
     * This means that no transformation takes place.
     */
    fun ResetTransform()
        = updateStatus(GdipResetLineTransform(ptr))

    /**
     * Updates this brush's current transformation matrix with the product of itself and a rotation matrix.
     */
    fun RotateTransform(angle: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipRotateLineTransform(ptr, angle, order))

    /**
     * Updates this brush's current transformation matrix with the product of itself and a scaling matrix.
     */
    fun ScaleTransform(sx: REAL, sy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipScaleLineTransform(ptr, sx, sy, order))

    /**
     * Sets the blend factors and the blend positions of this linear gradient brush to create a custom blend.
     */
    fun SetBlend(blendFactors: REALVar, blendPositions: REALVar, count: INT)
        = updateStatus(GdipSetLineBlend(ptr, blendFactors.ptr, blendPositions.ptr, count))

    /**
     * Sets the blend shape of this linear gradient brush to create a custom blend based on a bell-shaped curve.
     */
    fun SetBlendBellShape(focus: REAL, scale: REAL = 1.0f)
        = updateStatus(GdipSetLineSigmaBlend(ptr, focus, scale))

    /**
     * Sets the blend shape of this linear gradient brush to create a custom blend based on a triangular shape.
     */
    fun SetBlendTriangularShape(focus: REAL, scale: REAL = 1.0f)
        = updateStatus(GdipSetLineLinearBlend(ptr,focus, scale))

    /**
     * Specifies whether gamma correction is enabled for this linear gradient brush.
     */
    fun SetGammaCorrection(useGammaCorrection: BOOL)
        = updateStatus(GdipSetLineGammaCorrection(ptr, useGammaCorrection))

    /**
     * Sets the colors to be interpolated for this linear gradient brush and their corresponding blend positions.
     */ 
    fun SetInterpolationColors(presetColors: CPointer<Color>, blendPositions: CValuesRef<REALVar>, count: INT): GpStatus {
        if (count < 0)
            return updateStatus(InvalidParameter)
        val presetArgb: CPointer<ARGBVar>?
            = GdipAlloc((count * sizeOf<ARGBVar>()).signExtend())?.reinterpret()
        if (presetArgb == null)
            return updateStatus(OutOfMemory)
        for (i in 0 until count)
            presetArgb[i] = presetColors[i].Value
        val status = updateStatus(GdipSetLinePresetBlend(ptr, presetArgb, blendPositions, count))
        GdipFree(presetArgb)
        return status
    }

    /**
     * Sets the starting color and ending color of this linear gradient brush.
     */
    fun SetLinearColors(color1: Color, color2: Color)
        = updateStatus(GdipSetLineColors(ptr, color1.Value, color2.Value))

    /**
     * Sets the transformation matrix of this linear gradient brush.
     */
    fun SetTransform(matrix: Matrix?)
        = updateStatus(GdipSetLineTransform(ptr, matrix?.ptr))

    /**
     * Sets the wrap mode of this linear gradient brush.
     */
    fun SetWrapMode(wrapMode: WrapMode)
        = updateStatus(GdipSetLineWrapMode(ptr, wrapMode))

    /**
     * Updates this brush's current transformation matrix with the product of itself and a translation matrix.
     */
    fun TranslateTransform(dx: REAL, dy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipTranslateLineTransform(ptr, dx, dy, order))
}

/**
 * A PathGradientBrush object stores the attributes of a color gradient that you can use to fill the interior
 * of a path with a gradually changing color. A path gradient brush has a boundary path, a boundary color,
 * a center point, and a center color. When you paint an area with a path gradient brush, the color changes
 * gradually from the boundary color to the center color as you move from the boundary path to the center point.
 */
class PathGradientBrush : Brush {
    protected constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a PathGradientBrush object based on an array of points. Initializes the wrap mode
     * of the path gradient brush.
     */
    constructor(points: PointF, count: INT, wrapMode: WrapMode = WrapModeClamp) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePathGradient(points.ptr, count, wrapMode, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a PathGradientBrush object based on an array of points. Initializes the wrap mode
     * of the path gradient brush.
     */
    constructor(points: Point, count: INT, wrapMode: WrapMode = WrapModeClamp) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePathGradientI(points.ptr, count, wrapMode, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a PathGradientBrush::PathGradientBrush object based on a GraphicsPath object.
     */
    constructor(path: GraphicsPath?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePathGradientFromPath(path?.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a new Brush object based on this brush
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneBrush(ptr, result.ptr))
        if (status == Ok) PathGradientBrush(result.value!!, status) else null
    }

    /**
     * Gets the blend factors and the corresponding blend positions currently set for this path gradient brush.
     */
    fun GetBlend(blendFactors: CValuesRef<REALVar>, blendPositions: CValuesRef<REALVar>, count: INT)
        = updateStatus(GdipGetPathGradientBlend(ptr, blendFactors, blendPositions, count))

    /**
     * Gets the number of blend factors currently set for this path gradient brush.
     */
    fun GetBlendCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetPathGradientBlendCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the color of the center point of this path gradient brush.
     */
    fun GetCenterColor(color: Color)
        = updateStatus(GdipGetPathGradientCenterColor(ptr, color.memberAt<ARGBVar>(0).ptr))

    /**
     * Gets the center point of this path gradient brush.
     */
    fun GetCenterPoint(point: PointF)
        = updateStatus(GdipGetPathGradientCenterPoint(ptr, point.ptr))

    /**
     * Gets the center point of this path gradient brush.
     */
    fun GetCenterPoint(point: Point)
        = updateStatus(GdipGetPathGradientCenterPointI(ptr, point.ptr))

    /**
     * Gets the focus scales of this path gradient brush.
     */
    fun GetFocusScales(xScale: REALVar, yScale: REALVar)
        = updateStatus(GdipGetPathGradientFocusScales(ptr, xScale.ptr, yScale.ptr))

    /**
     * Determines whether gamma correction is enabled for this path gradient brush.
     */
    fun GetGammaCorrection() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipGetPathGradientGammaCorrection(ptr, result.ptr))
        result.value
    }

    /**
     * Is not implemented in GDI+ version 1.0.
     */
//TODO fun GetGraphicsPath(path: GraphicsPath)
//         = updateStatus(NotImplemented)

    /**
     * Gets the number of preset colors currently specified for this path gradient brush.
     */
    fun GetInterpolationColorCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetPathGradientPresetBlendCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the preset colors and blend positions currently specified for this path gradient brush.
     */
    fun GetInterpolationColors(presetColors: CPointer<Color>, blendPositions: CValuesRef<REALVar>, count: INT): GpStatus {
        if (count <= 0)
            return updateStatus(InvalidParameter)
        val presetArgb: CPointer<ARGBVar>?
            = GdipAlloc((count * sizeOf<ARGBVar>()).signExtend())?.reinterpret()
        if (presetArgb == null)
            return updateStatus(OutOfMemory)
        val status = updateStatus(GdipGetPathGradientPresetBlend(ptr, presetArgb, blendPositions, count))
        for (i in 0 until count)
            presetColors[i].Value = presetArgb[i]
        GdipFree(presetArgb)
        return status
    }

    /**
     * Gets the number of points in the array of points that defines this brush's boundary path.
     */    
    fun GetPointCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetPathGradientPointCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the smallest rectangle that encloses the boundary path of this path gradient brush.
     */
    fun GetRectangle(rect: RectF)
        = updateStatus(GdipGetPathGradientRect(ptr, rect.ptr))

    /**
     * Gets the smallest rectangle that encloses the boundary path of this path gradient brush.
     */
    fun GetRectangle(rect: Rect)
        = updateStatus(GdipGetPathGradientRectI(ptr, rect.ptr))

    /**
     * Gets the number of colors that have been specified for the boundary path of this path gradient brush.
     */
    fun GetSurroundColorCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetPathGradientSurroundColorCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the surround colors currently specified for this path gradient brush.
     */
    fun GetSurroundColors(colors: CPointer<Color>, count: INTVar): GpStatus {
        if (count.value <= 0)
            return updateStatus(InvalidParameter)
        val colorsArgb: CPointer<ARGBVar>?
            = GdipAlloc((count.value * sizeOf<ARGBVar>()).signExtend())?.reinterpret()
        if (colorsArgb == null)
            return updateStatus(OutOfMemory)
        val status = updateStatus(GdipGetPathGradientSurroundColorsWithCount(ptr, colorsArgb, count.ptr))
        for (i in 0 until count.value)
            colors[i].Value = colorsArgb[i]
        GdipFree(colorsArgb)
        return status
    }

    /**
     * Gets transformation matrix of this path gradient brush.
     */
    fun GetTransform(matrix: Matrix?)
        = updateStatus(GdipGetPathGradientTransform(ptr, matrix?.ptr))

    /**
     * Gets the wrap mode currently set for this path gradient brush.
     */
    fun GetWrapMode() = memScoped {
        val result = alloc<WrapModeVar>().apply { value = WrapModeTile }
        updateStatus(GdipGetPathGradientWrapMode(ptr, result.ptr))
        result.value
    }

    /**
     * Updates the brush's transformation matrix with the product of itself and another matrix.
     */
    fun MultiplyTransform(matrix: Matrix?, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipMultiplyPathGradientTransform(ptr, matrix?.ptr, order))

    /**
     * Resets the transformation matrix of this path gradient brush to the identity matrix.
     * This means that no transformation will take place.
     */
    fun ResetTransform()
        = updateStatus(GdipResetPathGradientTransform(ptr))

    /**
     * Updates this brush's current transformation matrix with the product of itself and a rotation matrix.
     */
    fun RotateTransform(angle: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipRotatePathGradientTransform(ptr, angle, order))

    /**
     * Updates this brush's current transformation matrix with the product of itself and a scaling matrix.
     */
    fun ScaleTransform(sx: REAL, sy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipScalePathGradientTransform(ptr, sx, sy, order))

    /**
     * Sets the blend factors and the blend positions of this path gradient brush.
     */
    fun SetBlend(blendFactors: CValuesRef<REALVar>, blendPositions: CValuesRef<REALVar>, count: INT)
        = updateStatus(GdipSetPathGradientBlend(ptr, blendFactors, blendPositions, count))

    /**
     * Sets the blend shape of this path gradient brush.
     */
    fun SetBlendBellShape(focus: REAL, scale: REAL = 1.0f)
        = updateStatus(GdipSetPathGradientSigmaBlend(ptr, focus, scale))

    /**
     * Sets the blend shape of this path gradient brush.
     */
    fun SetBlendTriangularShape(focus: REAL, scale: REAL = 1.0f)
        = updateStatus(GdipSetPathGradientLinearBlend(ptr, focus, scale))

    /**
     * Sets the center color of this path gradient brush. The center color is the color that appears
     * at the brush's center point.
     */
    fun SetCenterColor(color: Color)
        = updateStatus(GdipSetPathGradientCenterColor(ptr, color.Value))

    /**
     * Sets the center point of this path gradient brush. By default, the center point is at the centroid of
     * the brush's boundary path, but you can set the center point to any location inside or outside the path.
     */
    fun SetCenterPoint(point: PointF)
        = updateStatus(GdipSetPathGradientCenterPoint(ptr, point.ptr))

    /**
     * Sets the center point of this path gradient brush. By default, the center point is at the centroid of
     * the brush's boundary path, but you can set the center point to any location inside or outside the path.
     */
    fun SetCenterPoint(point: Point)
        = updateStatus(GdipSetPathGradientCenterPointI(ptr, point.ptr))

    /**
     * Sets the focus scales of this path gradient brush.
     */
    fun SetFocusScales(xScale: REAL, yScale: REAL)
        = updateStatus(GdipSetPathGradientFocusScales(ptr, xScale, yScale))

    /**
     * Specifies whether gamma correction is enabled for this path gradient brush.
     */
    fun SetGammaCorrection(useGammaCorrection: BOOL)
        = updateStatus(GdipSetPathGradientGammaCorrection(ptr, useGammaCorrection))

    /**
     * is not implemented in GDI+ version 1.0.
     */
//TODO fun SetGraphicsPath(path: GraphicsPath)
//         = updateStatus(NotImplemented)

    /**
     * Sets the preset colors and the blend positions of this path gradient brush.
     */
    fun SetInterpolationColors(presetColors: CPointer<Color>, blendPositions: CValuesRef<REALVar>, count: INT): GpStatus {
        if (count <= 0)
            return updateStatus(InvalidParameter)
        val presetArgb: CPointer<ARGBVar>?
            = GdipAlloc((count * sizeOf<ARGBVar>()).signExtend())?.reinterpret()
        if (presetArgb == null)
            return updateStatus(OutOfMemory)
        for (i in 0 until count)
            presetArgb[i] = presetColors[i].Value
        val status = updateStatus(GdipSetPathGradientPresetBlend(ptr, presetArgb, blendPositions, count))
        GdipFree(presetArgb)
        return status
    }

    /**
     * Sets the surround colors of this path gradient brush. The surround colors are colors specified
     * for discrete points on the brush's boundary path.
     */
    fun SetSurroundColors(colors: CPointer<Color>, count: INTVar): GpStatus {
        if (count.value <= 0)
            return updateStatus(InvalidParameter)
        val colorsArgb: CPointer<ARGBVar>?
            = GdipAlloc((count.value * sizeOf<ARGBVar>()).signExtend())?.reinterpret()
        if (colorsArgb == null)
            return updateStatus(OutOfMemory)
        for (i in 0 until count.value)
            colorsArgb[i] = colors[i].Value
        val status = updateStatus(GdipSetPathGradientSurroundColorsWithCount(ptr, colorsArgb, count.ptr))
        GdipFree(colorsArgb)
        return status
    }

    /**
     * Sets the transformation matrix of this path gradient brush.
     */
    fun SetTransform(matrix: Matrix?)
        = updateStatus(GdipSetPathGradientTransform(ptr, matrix?.ptr))

    /**
     * Sets the wrap mode of this path gradient brush.
     */
    fun SetWrapMode(wrapMode: WrapMode)
        = updateStatus(GdipSetPathGradientWrapMode(ptr, wrapMode))

    /**
     * Updates this brush's current transformation matrix with the product of itself and a translation matrix.
     */
    fun TranslateTransform(dx: REAL, dy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipTranslatePathGradientTransform(ptr, dx, dy, order))
}

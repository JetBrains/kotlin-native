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
 * The Graphics class provides methods for drawing lines, curves, figures, images, and text.
 * A Graphics object stores attributes of the display device and attributes of the items to be drawn.
 */
class Graphics : GdipObject {

    companion object {

        /**
         * Creates a Graphics object that is associated with a specified device context.
         */
        fun FromHDC(hdc: HDC) = Graphics(hdc)

        /**
         * Creates a Graphics object that is associated with a specified device context and a specified device.
         */
        fun FromHDC(hdc: HDC, hdevice: HANDLE) = Graphics(hdc, hdevice)

        /**
         * Creates a Graphics object that is associated with a specified window.
         */
//TODO  fun FromHWND(hwnd: HWND, icm: Boolean = false) = Graphics(hwnd, icm)

        /**
         * Creates a Graphics object that is associated with a specified Image object.
         */
        fun FromImage(image: Image) = Graphics(image)

        /**
         * Gets a Windows halftone palette.
         */
        fun GetHalftonePalette() = GdipCreateHalftonePalette()
    }

    /**
     * Creates a Graphics object that is associated with an Image object.
     */
    constructor(image: Image) : super() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipGetImageGraphicsContext(image.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Graphics object that is associated with a specified device context.
     */
    constructor(hdc: HDC) : super() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateFromHDC(hdc, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Graphics object that is associated with a specified device context and a specified device.
     */
    constructor(hdc: HDC, hdevice: HANDLE) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateFromHDC2(hdc, hdevice, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Graphics object that is associated with a specified window.
     */
/*TODO
    constructor(hwnd: HWND, icm: Boolean = false) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = if (icm) GdipCreateFromHWNDICM(hwnd, result.ptr)
                             else GdipCreateFromHWND(hwnd, result.ptr)
            ptr = result.value
        }
    }
*/

    override fun Clone() = TODO()

    override fun Dispose() {
        GdipDeleteGraphics(ptr)
    }

    /**
     * Adds a text comment to an existing metafile.
     */
    fun AddMetafileComment(data: CPointer<BYTEVar>, sizeData: UINT)
        = updateStatus(GdipComment(ptr, sizeData, data))

    /**
     * Begins a new graphics container.
     */
    fun BeginContainer(): GraphicsContainer = memScoped {
        val result = alloc<GraphicsContainerVar>().apply { value = 0 }
        updateStatus(GdipBeginContainer2(ptr, result.ptr))
        result.value
    }

    /**
     * Begins a new graphics container.
     */
    fun BeginContainer(dstrect: RectF, srcrect: RectF, unit: GpUnit): GraphicsContainer = memScoped {
        val result = alloc<GraphicsContainerVar>().apply { value = 0 }
        updateStatus(GdipBeginContainer(ptr, dstrect.ptr, srcrect.ptr, unit, result.ptr))
        result.value
    }

    /**
     * Begins a new graphics container.
     */
    fun BeginContainer(dstrect: Rect, srcrect: Rect, unit: GpUnit): GraphicsContainer = memScoped {
        val result = alloc<GraphicsContainerVar>().apply { value = 0 }
        updateStatus(GdipBeginContainerI(ptr, dstrect.ptr, srcrect.ptr, unit, result.ptr))
        result.value
    }

    /**
     * Clears a Graphics object to a specified color.
     */
    fun Clear(color: Color)
        = updateStatus(GdipGraphicsClear(ptr, color.Value))

    /**
     * Draws an arc. The arc is part of an ellipse.
     */
    fun DrawArc(pen: Pen?, x: REAL, y: REAL, width: REAL, height: REAL, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawArc(ptr, pen?.ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Draws an arc. The arc is part of an ellipse.
     */
    fun DrawArc(pen: Pen?, x: INT, y: INT, width: INT, height: INT, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawArcI(ptr, pen?.ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Draws an arc. The arc is part of an ellipse.
     */
    fun DrawArc(pen: Pen?, rect: RectF, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawArc(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Draws an arc. The arc is part of an ellipse.
     */
    fun DrawArc(pen: Pen?, rect: Rect, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawArcI(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Draws a Bézier spline.
     */
    fun DrawBezier(pen: Pen?, x1: REAL, y1: REAL, x2: REAL, y2: REAL, x3: REAL, y3: REAL, x4: REAL, y4: REAL)
        = updateStatus(GdipDrawBezier(ptr, pen?.ptr, x1, y1, x2, y2, x3, y3, x4, y4))

    /**
     * Draws a Bézier spline.
     */
    fun DrawBezier(pen: Pen?, x1: INT, y1: INT, x2: INT, y2: INT, x3: INT, y3: INT, x4: INT, y4: INT)
        = updateStatus(GdipDrawBezierI(ptr, pen?.ptr, x1, y1, x2, y2, x3, y3, x4, y4))

    /**
     * Draws a Bézier spline.
     */
    fun DrawBezier(pen: Pen?, pt1: PointF, pt2: PointF, pt3: PointF, pt4: PointF)
        = updateStatus(GdipDrawBezier(ptr, pen?.ptr, pt1.X, pt1.Y, pt2.X, pt2.Y, pt3.X, pt3.Y, pt4.X, pt4.Y))

    /**
     * Draws a Bézier spline.
     */
    fun DrawBezier(pen: Pen?, pt1: Point, pt2: Point, pt3: Point, pt4: Point)
        = updateStatus(GdipDrawBezierI(ptr, pen?.ptr, pt1.X, pt1.Y, pt2.X, pt2.Y, pt3.X, pt3.Y, pt4.X, pt4.Y))

    /**
     * Draws a sequence of connected Bézier splines.
     */
    fun DrawBeziers(pen: Pen?, points: PointF?, count: INT)
        = updateStatus(GdipDrawBeziers(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a sequence of connected Bézier splines.
     */
    fun DrawBeziers(pen: Pen?, points: Point?, count: INT)
        = updateStatus(GdipDrawBeziersI(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws the image stored in a CachedBitmap object.
     */
    fun DrawCachedBitmap(cb: CachedBitmap?, x: INT, y: INT)
        = updateStatus(GdipDrawCachedBitmap(ptr, cb?.ptr, x, y))

    /**
     * Draws a closed cardinal spline.
     */
    fun DrawClosedCurve(pen: Pen?, points: PointF?, count: INT)
        = updateStatus(GdipDrawClosedCurve(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a closed cardinal spline.
     */
    fun DrawClosedCurve(pen: Pen?, points: Point?, count: INT)
        = updateStatus(GdipDrawClosedCurveI(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a closed cardinal spline.
     */
    fun DrawClosedCurve(pen: Pen?, points: PointF?, count: INT, tension: REAL)
        = updateStatus(GdipDrawClosedCurve2(ptr, pen?.ptr, points?.ptr, count, tension))

    /**
     * Draws a closed cardinal spline.
     */
    fun DrawClosedCurve(pen: Pen?, points: Point?, count: INT, tension: REAL)
        = updateStatus(GdipDrawClosedCurve2I(ptr, pen?.ptr, points?.ptr, count, tension))

    /**
     * Draws a cardinal spline.
     */
    fun DrawCurve(pen: Pen?, points: PointF?, count: INT)
        = updateStatus(GdipDrawCurve(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a cardinal spline.
     */
    fun DrawCurve(pen: Pen?, points: Point?, count: INT)
        = updateStatus(GdipDrawCurveI(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a cardinal spline.
     */
    fun DrawCurve(pen: Pen?, points: PointF?, count: INT, tension: REAL)
        = updateStatus(GdipDrawCurve2(ptr, pen?.ptr, points?.ptr, count, tension))

    /**
     * Draws a cardinal spline.
     */
    fun DrawCurve(pen: Pen?, points: Point?, count: INT, tension: REAL)
        = updateStatus(GdipDrawCurve2I(ptr, pen?.ptr, points?.ptr, count, tension))

    /**
     * Draws a cardinal spline.
     */
    fun DrawCurve(pen: Pen?, points: PointF?, count: INT, offset: INT, numberOfSegments: INT, tension: REAL)
        = updateStatus(GdipDrawCurve3(ptr, pen?.ptr, points?.ptr, count, offset, numberOfSegments, tension))

    /**
     * Draws a cardinal spline.
     */
    fun DrawCurve(pen: Pen?, points: Point?, count: INT, offset: INT, numberOfSegments: INT, tension: REAL)
        = updateStatus(GdipDrawCurve3I(ptr, pen?.ptr, points?.ptr, count, offset, numberOfSegments, tension))

    /**
     * Draws characters at the specified positions. The method gives the client complete control over the 
     * appearance of text. The method assumes that the client has already set up the format and layout to be applied.
     */
    fun DrawDriverString(text: String, length: INT, font: Font?, brush: Brush?, positions: PointF?, 
                         flags: INT, matrix: Matrix?)
        = updateStatus(GdipDrawDriverString(ptr, text.wcstr, length, font?.ptr, brush?.ptr, 
                                            positions?.ptr, flags, matrix?.ptr))

    /**
     * Draws an ellipse.
     */
    fun DrawEllipse(pen: Pen?, x: REAL, y: REAL, width: REAL, height: REAL)
        = updateStatus(GdipDrawEllipse(ptr, pen?.ptr, x, y, width, height))

    /**
     * Draws an ellipse.
     */
    fun DrawEllipse(pen: Pen?, x: INT, y: INT, width: INT, height: INT)
        = updateStatus(GdipDrawEllipseI(ptr, pen?.ptr, x, y, width, height))

    /**
     * Draws an ellipse.
     */
    fun DrawEllipse(pen: Pen?, rect: RectF)
        = updateStatus(GdipDrawEllipse(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Draws an ellipse.
     */
    fun DrawEllipse(pen: Pen?, rect: Rect)
        = updateStatus(GdipDrawEllipseI(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, x: REAL, y: REAL)
        = updateStatus(GdipDrawImage(ptr, image?.ptr, x, y))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, x: INT, y: INT)
        = updateStatus(GdipDrawImageI(ptr, image?.ptr, x, y))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, point: PointF)
        = updateStatus(GdipDrawImage(ptr, image?.ptr, point.X, point.Y))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, point: Point)
        = updateStatus(GdipDrawImageI(ptr, image?.ptr, point.X, point.Y))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, x: REAL, y: REAL, width: REAL, height: REAL)
        = updateStatus(GdipDrawImageRect(ptr, image?.ptr, x, y, width, height))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, x: INT, y: INT, width: INT, height: INT)
        = updateStatus(GdipDrawImageRectI(ptr, image?.ptr, x, y, width, height))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, rect: RectF)
        = updateStatus(GdipDrawImageRect(ptr, image?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, rect: Rect)
        = updateStatus(GdipDrawImageRectI(ptr, image?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, destPoints: PointF?, count: INT)
        = updateStatus(GdipDrawImagePoints(ptr, image?.ptr, destPoints?.ptr, count))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, destPoints: Point?, count: INT)
        = updateStatus(GdipDrawImagePointsI(ptr, image?.ptr, destPoints?.ptr, count))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, x: REAL, y: REAL, srcx: REAL, srcy: REAL, srcwidth: REAL, srcheight: REAL, srcUnit: GpUnit)
        = updateStatus(GdipDrawImagePointRect(ptr, image?.ptr, x, y, srcx, srcy, srcwidth, srcheight, srcUnit))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, x: INT, y: INT, srcx: INT, srcy: INT, srcwidth: INT, srcheight: INT, srcUnit: GpUnit)
        = updateStatus(GdipDrawImagePointRectI(ptr, image?.ptr, x, y, srcx, srcy, srcwidth, srcheight, srcUnit))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, destRect: RectF, srcx: REAL, srcy: REAL, srcwidth: REAL, srcheight: REAL,
                  srcUnit: GpUnit, imageAttributes: ImageAttributes? = null, callback: DrawImageAbort? = null,
                  callbackData: COpaquePointer? = null)
        = updateStatus(GdipDrawImageRectRect(ptr, image?.ptr, destRect.X, destRect.Y, destRect.Width, destRect.Height,
                  srcx, srcy, srcwidth, srcheight, srcUnit, imageAttributes?.ptr, callback, callbackData))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, destRect: Rect, srcx: INT, srcy: INT, srcwidth: INT, srcheight: INT,
                  srcUnit: GpUnit, imageAttributes: ImageAttributes? = null, callback: DrawImageAbort? = null,
                  callbackData: COpaquePointer? = null)
        = updateStatus(GdipDrawImageRectRectI(ptr, image?.ptr, destRect.X, destRect.Y, destRect.Width, destRect.Height,
                  srcx, srcy, srcwidth, srcheight, srcUnit, imageAttributes?.ptr, callback, callbackData))

    /**
     * Draws an image.
     */
    fun DrawImage(image: Image?, destRect: RectF, sourceRect: RectF, srcUnit: GpUnit, imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipDrawImageRectRect(ptr, image?.ptr, destRect.X, destRect.Y, destRect.Width, destRect.Height,
                  sourceRect.X, sourceRect.Y, sourceRect.Width, sourceRect.Height, srcUnit, imageAttributes?.ptr,
                  null, null))

    /**
     * Draws a specified portion of an image at a specified location.
     */
    fun DrawImage(image: Image?, destPoints: PointF?, count: INT, srcx: REAL, srcy: REAL, srcwidth: REAL, srcheight: REAL,
                  srcUnit: GpUnit, imageAttributes: ImageAttributes? = null, callback: DrawImageAbort? = null,
                  callbackData: COpaquePointer? = null)
        = updateStatus(GdipDrawImagePointsRect(ptr, image?.ptr, destPoints?.ptr, count, srcx, srcy, srcwidth, srcheight, srcUnit,
                  imageAttributes?.ptr, callback, callbackData))

    /**
     * Draws a specified portion of an image at a specified location.
     */
    fun DrawImage(image: Image?, destPoints: Point?, count: INT, srcx: INT, srcy: INT, srcwidth: INT, srcheight: INT,
                  srcUnit: GpUnit, imageAttributes: ImageAttributes? = null, callback: DrawImageAbort? = null,
                  callbackData: COpaquePointer? = null)
        = updateStatus(GdipDrawImagePointsRectI(ptr, image?.ptr, destPoints?.ptr, count, srcx, srcy, srcwidth, srcheight, srcUnit,
                  imageAttributes?.ptr, callback, callbackData))

    // TODO: [GDI+ 1.1] Graphics::DrawImage(..Effect..)
    //Draws a portion of an image after applying a specified effect.
    //Status DrawImage(image: Image?, RectF *sourceRect, Matrix *matrix,
    //      Effect *effect, ImageAttributes *imageAttributes,
    //      GpUnit srcUnit)
    //{
    //  return updateStatus(GdipDrawImageFX(
    //          ptr,
    //          image?.ptr,
    //          sourceRect,
    //          matrix?.ptr,
    //          effect ? effect->ptrEffect : null,
    //          imageAttributes?.ptr,
    //          srcUnit))
    //}

    /**
     * Draws a line that connects two points.
     */
    fun DrawLine(pen: Pen?, x1: REAL, y1: REAL, x2: REAL, y2: REAL)
        = updateStatus(GdipDrawLine(ptr, pen?.ptr, x1, y1, x2, y2))

    /**
     * Draws a line that connects two points.
     */
    fun DrawLine(pen: Pen?, x1: INT, y1: INT, x2: INT, y2: INT)
        = updateStatus(GdipDrawLineI(ptr, pen?.ptr, x1, y1, x2, y2))

    /**
     * Draws a line that connects two points.
     */
    fun DrawLine(pen: Pen?, pt1: PointF, pt2: PointF)
        = updateStatus(GdipDrawLine(ptr, pen?.ptr, pt1.X, pt1.Y, pt2.X, pt2.Y))

    /**
     * Draws a line that connects two points.
     */
    fun DrawLine(pen: Pen?, pt1: Point, pt2: Point)
        = updateStatus(GdipDrawLineI(ptr, pen?.ptr, pt1.X, pt1.Y, pt2.X, pt2.Y))

    /**
     * Draws a sequence of connected lines.
     */
    fun DrawLines(pen: Pen?, points: PointF?, count: INT)
        = updateStatus(GdipDrawLines(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a sequence of connected lines.
     */
    fun DrawLines(pen: Pen?, points: Point?, count: INT)
        = updateStatus(GdipDrawLinesI(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a sequence of lines and curves defined by a GraphicsPath object.
     */
    fun DrawPath(pen: Pen?, path: GraphicsPath?)
        = updateStatus(GdipDrawPath(ptr, pen?.ptr, path?.ptr))

    /**
     * Draws a pie.
     */
    fun DrawPie(pen: Pen?, x: REAL, y: REAL, width: REAL, height: REAL, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawPie(ptr, pen?.ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Draws a pie.
     */
    fun DrawPie(pen: Pen?, x: INT, y: INT, width: INT, height: INT, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawPieI(ptr, pen?.ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Draws a pie.
     */
    fun DrawPie(pen: Pen?, rect: RectF, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawPie(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Draws a pie.
     */
    fun DrawPie(pen: Pen?, rect: Rect, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipDrawPieI(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Draws a polygon.
     */
    fun DrawPolygon(pen: Pen?, points: PointF?, count: INT)
        = updateStatus(GdipDrawPolygon(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a polygon.
     */
    fun DrawPolygon(pen: Pen?, points: Point?, count: INT)
        = updateStatus(GdipDrawPolygonI(ptr, pen?.ptr, points?.ptr, count))

    /**
     * Draws a rectangle.
     */
    fun DrawRectangle(pen: Pen?, x: REAL, y: REAL, width: REAL, height: REAL)
        = updateStatus(GdipDrawRectangle(ptr, pen?.ptr, x, y, width, height))

    /**
     * Draws a rectangle.
     */
    fun DrawRectangle(pen: Pen?, x: INT, y: INT, width: INT, height: INT)
        = updateStatus(GdipDrawRectangleI(ptr, pen?.ptr, x, y, width, height))

    /**
     * Draws a rectangle.
     */
    fun DrawRectangle(pen: Pen?, rect: RectF)
        = updateStatus(GdipDrawRectangle(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Draws a rectangle.
     */
    fun DrawRectangle(pen: Pen?, rect: Rect)
        = updateStatus(GdipDrawRectangleI(ptr, pen?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Draws a sequence of rectangles.
     */
    fun DrawRectangles(pen: Pen?, rects: RectF?, count: INT)
        = updateStatus(GdipDrawRectangles(ptr, pen?.ptr, rects?.ptr, count))

    /**
     * Draws a sequence of rectangles.
     */
    fun DrawRectangles(pen: Pen?, rects: Rect?, count: INT)
        = updateStatus(GdipDrawRectanglesI(ptr, pen?.ptr, rects?.ptr, count))

    /**
     * Draws a string based on a font and an origin for the string.
     */
    fun DrawString(string: String, length: INT, font: Font?, origin: PointF, brush: Brush?) = memScoped {
        val layoutRect = alloc<RectF>().apply { X = origin.X; Y = origin.Y; Width = 0.0f; Height = 0.0f }
        updateStatus(GdipDrawString(ptr, string.wcstr, length, font?.ptr, layoutRect.ptr, null, brush?.ptr ))
    }

    /**
     * Draws a string based on a font, a string origin, and a format.
     */
    fun DrawString(string: String, length: INT, font: Font?, origin: PointF, stringFormat: StringFormat?, brush: Brush?) = memScoped {
        val layoutRect = alloc<RectF>().apply { X = origin.X; Y = origin.Y; Width = 0.0f; Height = 0.0f }
        updateStatus(GdipDrawString(ptr, string.wcstr, length, font?.ptr, layoutRect.ptr, stringFormat?.ptr, brush?.ptr))
    }

    /**
     * Draws a string based on a font, a layout rectangle, and a format.
     */
    fun DrawString(string: String, length: INT, font: Font?, layoutRect: RectF, stringFormat: StringFormat?, brush: Brush?)
        = updateStatus(GdipDrawString(ptr, string.wcstr, length, font?.ptr, layoutRect.ptr, stringFormat?.ptr, brush?.ptr))

    /**
     * Closes a graphics container that was previously opened by the Graphics::BeginContainer method.
     */
    fun EndContainer(state: GraphicsContainer)
        = updateStatus(GdipEndContainer(ptr, state))
/*TODO
    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destPoint: PointF, callback: EnumerateMetafileProc,
                          callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipEnumerateMetafileDestPoint(ptr, metafile?.ptr, destPoint, callback, callbackData,
                          imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destPoint: Point, callback: EnumerateMetafileProc,
                          callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes = null)
        = updateStatus(GdipEnumerateMetafileDestPointI(ptr, metafile?.ptr, destPoint, callback, callbackData,
                          imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destRect: RectF, callback: EnumerateMetafileProc,
                          callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipEnumerateMetafileDestRect(ptr, metafile?.ptr,  destRect, callback, callbackData,
                          imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destRect: Rect, callback: EnumerateMetafileProc,
                          callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipEnumerateMetafileDestRectI(ptr, metafile?.ptr, destRect, callback, callbackData,
                          imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile?, destPoints: PointF?, count: INT, callback: EnumerateMetafileProc,
                          callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipEnumerateMetafileDestPoints(
                ptr, metafile?.ptr, destPoints?.ptr, count, callback, callbackData, imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile?, destPoints: Point?, count: INT, callback: EnumerateMetafileProc,
                          callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipEnumerateMetafileDestPointsI(
                ptr, metafile?.ptr, destPoints?.ptr, count, callback, callbackData, imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destPoint: PointF, srcRect: RectF, GpUnit srcUnit,
                          callback: EnumerateMetafileProc, callbackData: COpaquePointer? = null,
                          imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipEnumerateMetafileSrcRectDestPoint(
                ptr, metafile?.ptr, destPoint, srcRect, srcUnit, callback, callbackData, imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destPoint: Point, srcRect: Rect, GpUnit srcUnit,
            callback: EnumerateMetafileProc, callbackData: COpaquePointer? = null,
            imageAttributes: ImageAttributes? = null)
        = updateStatus(GdipEnumerateMetafileSrcRectDestPointI(
                ptr, metafile?.ptr, destPoint, srcRect, srcUnit, callback, callbackData, imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destRect: RectF, srcRect: RectF, GpUnit srcUnit,
            callback: EnumerateMetafileProc, callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        return updateStatus(GdipEnumerateMetafileSrcRectDestRect(
                ptr, metafile?.ptr, destRect, srcRect, srcUnit, callback, callbackData, imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destRect: Rect, srcRect: Rect, GpUnit srcUnit,
            callback: EnumerateMetafileProc, callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        return updateStatus(GdipEnumerateMetafileSrcRectDestRectI(
                ptr, metafile?.ptr, destRect, srcRect, srcUnit, callback, callbackData, imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, const PointF* destPoints, count: INT, srcRect: RectF, GpUnit srcUnit,
            callback: EnumerateMetafileProc, callbackData: VOIDVar = null, imageAttributes: ImageAttributes? = null)
        return updateStatus(GdipEnumerateMetafileSrcRectDestPoints(
                ptr, metafile?.ptr, destPoints, count, srcRect, srcUnit, callback, callbackData, imageAttributes?.ptr))

    //Calls an application-defined callback function for each record in a specified metafile. You can use this method to display a metafile by calling Metafile::PlayRecord in the callback function.
    fun EnumerateMetafile(metafile: Metafile, destPoints: Point?, count: INT, srcRect: Rect, GpUnit srcUnit,
            callback: EnumerateMetafileProc, callbackData: COpaquePointer? = null, imageAttributes: ImageAttributes? = null)
        return updateStatus(GdipEnumerateMetafileSrcRectDestPointsI(
                ptr, metafile?.ptr, destPoints?.ptr, count, srcRect, srcUnit, callback, callbackData, imageAttributes?.ptr))
*/
    /**
     * Updates the clipping region to the portion of itself that does not intersect the specified rectangle.
     */
    fun ExcludeClip(rect: RectF)
        = updateStatus(GdipSetClipRect(ptr, rect.X, rect.Y, rect.Width, rect.Height, CombineModeExclude))

    /**
     * Updates the clipping region to the portion of itself that does not intersect the specified rectangle.
     */
    fun ExcludeClip(rect: Rect)
        = updateStatus(GdipSetClipRectI(ptr, rect.X, rect.Y, rect.Width, rect.Height, CombineModeExclude))

    /**
     * Updates the clipping region with the portion of itself that does not overlap the specified region.
     */
    fun ExcludeClip(region: Region?)
        = updateStatus(GdipSetClipRegion(ptr, region?.ptr, CombineModeExclude))

    /**
     * Creates a closed cardinal spline from an array of points and uses a brush to fill the interior of the spline.
     */
    fun FillClosedCurve(brush: Brush?, points: PointF?, count: INT)
        = updateStatus(GdipFillClosedCurve(ptr, brush?.ptr, points?.ptr, count))

    /**
     * Creates a closed cardinal spline from an array of points and uses a brush to fill the interior of the spline.
     */
    fun FillClosedCurve(brush: Brush?, points: Point?, count: INT)
        = updateStatus(GdipFillClosedCurveI(ptr, brush?.ptr, points?.ptr, count))

    /**
     * Creates a closed cardinal spline from an array of points and uses a brush to fill, according to a specified mode, the interior of the spline.
     */
    fun FillClosedCurve(brush: Brush?, points: PointF?, count: INT, fillMode: FillMode, tension: REAL = 0.5f)
        = updateStatus(GdipFillClosedCurve2(ptr, brush?.ptr, points?.ptr, count, tension, fillMode))

    /**
     * Creates a closed cardinal spline from an array of points and uses a brush to fill, according to a specified mode, the interior of the spline.
     */
    fun FillClosedCurve(brush: Brush?, points: Point?, count: INT, fillMode: FillMode, tension: REAL = 0.5f)
        = updateStatus(GdipFillClosedCurve2I(ptr, brush?.ptr, points?.ptr, count, tension, fillMode))

    /**
     * Uses a brush to fill the interior of an ellipse that is specified by coordinates and dimensions.
     */
    fun FillEllipse(brush: Brush?, x: REAL, y: REAL, width: REAL, height: REAL)
        = updateStatus(GdipFillEllipse(ptr, brush?.ptr, x, y, width, height))

    /**
     * Uses a brush to fill the interior of an ellipse that is specified by coordinates and dimensions.
     */
    fun FillEllipse(brush: Brush?, x: INT, y: INT, width: INT, height: INT)
        = updateStatus(GdipFillEllipseI(ptr, brush?.ptr, x, y, width, height))

    /**
     * Uses a brush to fill the interior of an ellipse that is specified by a rectangle.
     */
    fun FillEllipse(brush: Brush?, rect: RectF)
        = updateStatus(GdipFillEllipse(ptr, brush?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Uses a brush to fill the interior of an ellipse that is specified by a rectangle.
     */
    fun FillEllipse(brush: Brush?, rect: Rect)
        = updateStatus(GdipFillEllipseI(ptr, brush?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Uses a brush to fill the interior of a path. If a figure in the path is not closed, this method treats 
     * the nonclosed figure as if it were closed by a straight line that connects the figure's starting 
     * and ending points.
     */
    fun FillPath(brush: Brush?, path: GraphicsPath?)
        = updateStatus(GdipFillPath(ptr, brush?.ptr, path?.ptr))

    /**
     * Uses a brush to fill the interior of a pie.
     */
    fun FillPie(brush: Brush?, x: REAL, y: REAL, width: REAL, height: REAL, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipFillPie(ptr, brush?.ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Uses a brush to fill the interior of a pie.
     */
    fun FillPie(brush: Brush?, x: INT, y: INT, width: INT, height: INT, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipFillPieI(ptr, brush?.ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Uses a brush to fill the interior of a pie.
     */
    fun FillPie(brush: Brush?, rect: RectF, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipFillPie(ptr, brush?.ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Uses a brush to fill the interior of a pie.
     */
    fun FillPie(brush: Brush?, rect: Rect, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipFillPieI(ptr, brush?.ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Uses a brush to fill the interior of a polygon.
     */
    fun FillPolygon(brush: Brush?, points: PointF?, count: INT)
        = updateStatus(GdipFillPolygon(ptr, brush?.ptr, points?.ptr, count, FillModeAlternate))

    /**
     * Uses a brush to fill the interior of a polygon.
     */
    fun FillPolygon(brush: Brush?, points: Point?, count: INT)
        = updateStatus(GdipFillPolygonI(ptr, brush?.ptr, points?.ptr, count, FillModeAlternate))

    /**
     * Uses a brush to fill the interior of a polygon.
     */
    fun FillPolygon(brush: Brush?, points: PointF?, count: INT, fillMode: FillMode)
        = updateStatus(GdipFillPolygon(ptr, brush?.ptr, points?.ptr, count, fillMode))

    /**
     * Uses a brush to fill the interior of a polygon.
     */
    fun FillPolygon(brush: Brush?, points: Point?, count: INT, fillMode: FillMode)
        = updateStatus(GdipFillPolygonI(ptr, brush?.ptr, points?.ptr, count, fillMode))

    /**
     * Uses a brush to fill the interior of a rectangle.
     */
    fun FillRectangle(brush: Brush?, x: REAL, y: REAL, width: REAL, height: REAL)
        = updateStatus(GdipFillRectangle(ptr, brush?.ptr, x, y, width, height))

    /**
     * Uses a brush to fill the interior of a rectangle.
     */
    fun FillRectangle(brush: Brush?, x: INT, y: INT, width: INT, height: INT)
        = updateStatus(GdipFillRectangleI(ptr, brush?.ptr, x, y, width, height))

    /**
     * Uses a brush to fill the interior of a rectangle.
     */
    fun FillRectangle(brush: Brush?, rect: RectF)
        = updateStatus(GdipFillRectangle(ptr, brush?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Uses a brush to fill the interior of a rectangle.
     */
    fun FillRectangle(brush: Brush?, rect: Rect)
        = updateStatus(GdipFillRectangleI(ptr, brush?.ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Uses a brush to fill the interior of a sequence of rectangles.
     */
    fun FillRectangles(brush: Brush?, rects: RectF?, count: INT)
        = updateStatus(GdipFillRectangles(ptr, brush?.ptr, rects?.ptr, count))

    /**
     * Uses a brush to fill the interior of a sequence of rectangles.
     */
    fun FillRectangles(brush: Brush?, rects: Rect?, count: INT)
        = updateStatus(GdipFillRectanglesI(ptr, brush?.ptr, rects?.ptr, count))

    /**
     * Uses a brush to fill a specified region.
     */
    fun FillRegion(brush: Brush?, region: Region?)
        = updateStatus(GdipFillRegion(ptr, brush?.ptr, region?.ptr))

    /**
     * Flushes all pending graphics operations.
     */
    fun Flush(intention: FlushIntention = FlushIntentionFlush)
        = updateStatus(GdipFlush(ptr, intention))

    /**
     * Gets the clipping region of this Graphics object.
     */
    fun GetClip(region: Region?)
        = updateStatus(GdipGetClip(ptr, region?.ptr))

    /**
     * Gets a rectangle that encloses the clipping region of this Graphics object.
     */
    fun GetClipBounds(rect: RectF?)
        = updateStatus(GdipGetClipBounds(ptr, rect?.ptr))

    /**
     * Gets a rectangle that encloses the clipping region of this Graphics object.
     */
    fun GetClipBounds(rect: Rect?)
        = updateStatus(GdipGetClipBoundsI(ptr, rect?.ptr))

    /**
     * Gets the compositing mode currently set for this Graphics object.
     */
    fun GetCompositingMode() = memScoped {
        val result = alloc<CompositingModeVar>().apply { value = CompositingModeSourceOver }
        updateStatus(GdipGetCompositingMode(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the compositing quality currently set for this Graphics object.
     */
    fun GetCompositingQuality() = memScoped {
        val result = alloc<CompositingQualityVar>().apply { value = CompositingQualityDefault }
        updateStatus(GdipGetCompositingQuality(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the horizontal resolution, in dots per inch, of the display device associated with this Graphics object.
     */
    fun GetDpiX() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetDpiX(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the vertical resolution, in dots per inch, of the display device associated with this Graphics object.
     */
    fun GetDpiY() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetDpiY(ptr, result.ptr))
        result.value
    }

    /**
     * Gets a handle to the device context associated with this Graphics object.
     */
    fun GetHDC() = memScoped {
        val result = alloc<HDCVar>().apply { value = null }
        updateStatus(GdipGetDC(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the interpolation mode currently set for this Graphics object. The interpolation mode determines the algorithm that is used when images are scaled or rotated.
     */
    fun GetInterpolationMode() = memScoped {
        val result = alloc<InterpolationModeVar>().apply { value = InterpolationModeDefault }
        updateStatus(GdipGetInterpolationMode(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the nearest color to the color that is passed in. This method works on 8-bits per pixel or lower 
     * display devices for which there is an 8-bit color palette.
     */
    fun GetNearestColor(color: Color?)
        = updateStatus(GdipGetNearestColor(ptr, color?.memberAt<ARGBVar>(0)?.ptr))

    /**
     * Gets the scaling factor currently set for the page transformation of this Graphics object.
     * The page transformation converts page coordinates to device coordinates.
     */
    fun GetPageScale() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetPageScale(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the unit of measure currently set for this Graphics object.
     */
    fun GetPageUnit() = memScoped {
        val result = alloc<GpUnitVar>().apply { value = UnitWorld }
        updateStatus(GdipGetPageUnit(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the pixel offset mode currently set for this Graphics object.
     */
    fun GetPixelOffsetMode() = memScoped {
        val result = alloc<PixelOffsetModeVar>().apply { value = PixelOffsetModeDefault }
        updateStatus(GdipGetPixelOffsetMode(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the rendering origin currently set for this Graphics object. The rendering origin is used to set 
     * the dither origin for 8-bits per pixel and 16-bits per pixel dithering and is also used to set the 
     * origin for hatch brushes.
     */
    fun GetRenderingOrigin(x: INTVar, y: INTVar)
        = updateStatus(GdipGetRenderingOrigin(ptr, x.ptr, y.ptr))

    /**
     * Determines whether smoothing (antialiasing) is applied to the Graphics object.
     */
    fun GetSmoothingMode() = memScoped {
        val result = alloc<SmoothingModeVar>().apply { value = SmoothingModeDefault }
        updateStatus(GdipGetSmoothingMode(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the contrast value currently set for this Graphics object. The contrast value is used for
     * antialiasing text.
     */
    fun GetTextContrast() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetTextContrast(ptr, result.ptr))
        result.value
    }

    /**
     * Returns the text rendering mode currently set for this Graphics object.
     */
    fun GetTextRenderingHint() = memScoped {
        val result = alloc<TextRenderingHintVar>().apply { value = TextRenderingHintSystemDefault }
        updateStatus(GdipGetTextRenderingHint(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the world transformation matrix of this Graphics object.
     */
    fun GetTransform(matrix: Matrix?)
        = updateStatus(GdipGetWorldTransform(ptr, matrix?.ptr))

    /**
     * Gets a rectangle that encloses the visible clipping region of this Graphics object. 
     * The visible clipping region is the intersection of the clipping region of this Graphics object
     * and the clipping region of the window.
     */
    fun GetVisibleClipBounds(rect: RectF)
        = updateStatus(GdipGetVisibleClipBounds(ptr, rect.ptr))

    /**
     * Gets a rectangle that encloses the visible clipping region of this Graphics object. 
     * The visible clipping region is the intersection of the clipping region of this Graphics object
     * and the clipping region of the window.
     */
    fun GetVisibleClipBounds(rect: Rect)
        = updateStatus(GdipGetVisibleClipBoundsI(ptr, rect.ptr))

    /**
     * Updates the clipping region of this Graphics object to the portion of the specified rectangle
     * that intersects with the current clipping region of this Graphics object.
     */
    fun IntersectClip(rect: RectF)
        = updateStatus(GdipSetClipRect(ptr, rect.X, rect.Y, rect.Width, rect.Height, CombineModeIntersect))

    /**
     * Updates the clipping region of this Graphics object to the portion of the specified rectangle
     * that intersects with the current clipping region of this Graphics object.
     */
    fun IntersectClip(rect: Rect)
        = updateStatus(GdipSetClipRectI(ptr, rect.X, rect.Y, rect.Width, rect.Height, CombineModeIntersect))

    /**
     * Updates the clipping region of this Graphics object to the portion of the specified rectangle
     * that intersects with the current clipping region of this Graphics object.
     */
    fun IntersectClip(region: Region?)
        = updateStatus(GdipSetClipRegion(ptr, region?.ptr, CombineModeIntersect))

    /**
     * Determines whether the clipping region of this Graphics object is empty.
     */
    fun IsClipEmpty() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsClipEmpty(ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified point is inside the visible clipping region of this Graphics object. 
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the 
     * clipping region of the window.
     */
    fun IsVisible(x: REAL, y: REAL) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePoint(ptr, x, y, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified point is inside the visible clipping region of this Graphics object.
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the
     * clipping region of the window.
     */
    fun IsVisible(x: INT, y: INT) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePointI(ptr, x, y, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified point is inside the visible clipping region of this Graphics object.
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the
     * clipping region of the window.
     */
    fun IsVisible(point: PointF) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePoint(ptr, point.X, point.Y, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified point is inside the visible clipping region of this Graphics object.
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the
     * clipping region of the window.
     */
    fun IsVisible(point: Point) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePointI(ptr, point.X, point.Y, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified rectangle intersects the visible clipping region of this Graphics object.
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the
     * clipping region of the window.
     */
    fun IsVisible(x: REAL, y: REAL, width: REAL, height: REAL) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRect(ptr, x, y, width, height, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified rectangle intersects the visible clipping region of this Graphics object. 
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the 
     * clipping region of the window.
     */
    fun IsVisible(x: INT, y: INT, width: INT, height: INT) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRectI(ptr, x, y, width, height, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified rectangle intersects the visible clipping region of this Graphics object.
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the
     * clipping region of the window.
     */
    fun IsVisible(rect: RectF) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRect(ptr, rect.X, rect.Y, rect.Width, rect.Height, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified rectangle intersects the visible clipping region of this Graphics object.
     * The visible clipping region is the intersection of the clipping region of this Graphics object and the
     * clipping region of the window.
     */
    fun IsVisible(rect: Rect) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleRectI(ptr, rect.X, rect.Y, rect.Width, rect.Height, result.ptr))
        result.value
    }

    /**
     * Determines whether the visible clipping region of this Graphics object is empty. The visible clipping region is the intersection of the clipping region of this Graphics object and the clipping region of the window.
     */
    fun IsVisibleClipEmpty() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisibleClipEmpty(ptr, result.ptr))
        result.value
    }
    
    /*TODO
    //Gets a set of regions each of which bounds a range of character positions within a string.
    fun MeasureCharacterRanges(string: String, length: INT, font: Font?, layoutRect: RectF,
            stringFormat: StringFormat, regionCount: INT, regions: Region) {
        if (regionCount <= 0 || !regions)
            return lastStatus = InvalidParameter

        GpRegion **ptrArray = (GpRegion**)
            GdipAlloc(regionCount * sizeof(GpRegion*))
        if (!ptrArray)
            return lastStatus = OutOfMemory
        for (i: INT = 0 i < regionCount ++i) {
            ptrArray[i] = regions[i].ptr
        }
        Status status = updateStatus(GdipMeasureCharacterRanges(
                ptr, string, length,
                font?.ptr,
                layoutRect,
                stringFormat?.ptr,
                regionCount, ptrArray))
        GdipFree(ptrArray)
        return status
    }
*/
    /**
     * Measures the bounding box for the specified characters and their corresponding positions.
     */
    fun MeasureDriverString(text: String, length: INT, font: Font?, positions: PointF?, flags: INT,
                            matrix: Matrix?, boundingBox: RectF)
        = updateStatus(GdipMeasureDriverString(ptr, text.wcstr, length, font?.ptr, positions?.ptr, flags,
                       matrix?.ptr, boundingBox.ptr))

    /**
     * Measures the extent of the string in the specified font and layout rectangle.
     */
    fun MeasureString(string: String, length: INT, font: Font?, layoutRect: RectF, boundingBox: RectF)
        = updateStatus(GdipMeasureString(ptr, string.wcstr, length, font?.ptr, layoutRect.ptr, null, boundingBox.ptr, null, null))

    /**
     * Measures the extent of the string in the specified font and layout rectangle.
     */
    fun MeasureString(string: String, length: INT, font: Font?, layoutRect: RectF, stringFormat: StringFormat?, boundingBox: RectF,
                      codepointsFitted: INTVar? = null, linesFitted: INTVar? = null)
        = updateStatus(GdipMeasureString(ptr, string.wcstr, length, font?.ptr, layoutRect.ptr, stringFormat?.ptr,
                       boundingBox.ptr, codepointsFitted?.ptr, linesFitted?.ptr))
/*TODO
    fun MeasureString(string: String, length: INT, font: Font?, layoutRectSize: SizeF, stringFormat: StringFormat,
                      size: SizeF, codepointsFitted: INTVar = null, linesFitted: INTVar = null) {
        if (!size) return lastStatus = InvalidParameter
        RectF layoutRect(PointF(0.0f, 0.0f), layoutRectSize)
        RectF boundingBox
        Status status = updateStatus(GdipMeasureString(
                ptr, string, length,
                font?.ptr,
                &layoutRect,
                stringFormat?.ptr,
                &boundingBox, codepointsFitted, linesFitted))
        boundingBox.GetSize(size)
        return status
    }
*/
    /**
     * Measures the extent of the string in the specified font and layout rectangle.
     */
    fun MeasureString(string: String, length: INT, font: Font?, origin: PointF, boundingBox: RectF): GpStatus = memScoped {
        val layoutRect = alloc<RectF>().apply { X = origin.X; Y = origin.Y; Width = 0.0f; Height = 0.0f }
        return updateStatus(GdipMeasureString(ptr, string.wcstr, length, font?.ptr, layoutRect.ptr, null, boundingBox.ptr, null, null))
    }

    /**
     * Measures the extent of the string in the specified font, format, and layout rectangle.
     */
    fun MeasureString(string: String, length: INT, font: Font?, origin: PointF, stringFormat: StringFormat?, boundingBox: RectF): GpStatus = memScoped {
        val layoutRect = alloc<RectF>().apply { X = origin.X; Y = origin.Y; Width = 0.0f; Height = 0.0f }
        return updateStatus(GdipMeasureString(ptr, string.wcstr, length, font?.ptr, layoutRect.ptr, stringFormat?.ptr, boundingBox.ptr, null, null))
    }

    /**
     * Updates this Graphics object's world transformation matrix with the product of itself and another matrix.
     */
    fun MultiplyTransform(matrix: Matrix?, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipMultiplyWorldTransform(ptr, matrix?.ptr, order))

    /**
     * Releases a device context handle obtained by a previous call to the Graphics::GetHDC method of this Graphics object.
     */
    fun ReleaseHDC(hdc: HDC)
        = updateStatus(GdipReleaseDC(ptr, hdc))

    /**
     * Sets the clipping region of this Graphics object to an infinite region.
     */
    fun ResetClip()
        = updateStatus(GdipResetClip(ptr))

    /**
     * Sets the world transformation matrix of this Graphics object to the identity matrix.
     */
    fun ResetTransform()
        = updateStatus(GdipResetWorldTransform(ptr))

    /**
     * Sets the state of this Graphics object to the state stored by a previous call to the Graphics::Save method of this Graphics object.
     */
    fun Restore(state: GraphicsState)
        = updateStatus(GdipRestoreGraphics(ptr, state))

    /**
     * Updates the world transformation matrix of this Graphics object with the product of itself and a rotation matrix.
     */
    fun RotateTransform(angle: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipRotateWorldTransform(ptr, angle, order))

    /**
     * Saves the current state (transformations, clipping region, and quality settings) of this Graphics object. 
     * You can restore the state later by calling the Restore method.
     */
    fun Save(): GraphicsState = memScoped {
        val result = alloc<GraphicsStateVar>().apply { value = 0 }
        updateStatus(GdipSaveGraphics(ptr, result.ptr))
        result.value
    }

    /**
     * Updates this Graphics object's world transformation matrix with the product of itself and a scaling matrix.
     */
    fun ScaleTransform(sx: REAL, sy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipScaleWorldTransform(ptr, sx, sy, order))

    /**
     * Not used in GDI+ versions 1.0 and 1.1.
     */
    fun SetAbort()
        = updateStatus(NotImplemented)

    /**
     * Updates the clipping region of this Graphics object to a region that is the combination of itself and 
     * the clipping region of another Graphics object.
     */
    fun SetClip(g: Graphics?, combineMode: CombineMode = CombineModeReplace)
        = updateStatus(GdipSetClipGraphics(ptr, g?.ptr, combineMode))

    /**
     * Updates the clipping region of this Graphics object to a region that is the combination of itself 
     * and a rectangle.
     */
    fun SetClip(rect: RectF, combineMode: CombineMode = CombineModeReplace)
        = updateStatus(GdipSetClipRect(ptr, rect.X, rect.Y, rect.Width, rect.Height, combineMode))

    /**
     * Updates the clipping region of this Graphics object to a region that is the combination of itself
     * and a rectangle.
     */
    fun SetClip(rect: Rect, combineMode: CombineMode = CombineModeReplace)
        = updateStatus(GdipSetClipRectI(ptr, rect.X, rect.Y, rect.Width, rect.Height, combineMode))

    /**
     * Updates the clipping region of this Graphics object to a region that is the combination of itself and 
     * the region specified by a graphics path. If a figure in the path is not closed, this method treats 
     * the nonclosed figure as if it were closed by a straight line that connects the figure's starting and 
     * ending points.
     */
    fun SetClip(path: GraphicsPath?, combineMode: CombineMode = CombineModeReplace)
        = updateStatus(GdipSetClipPath(ptr, path?.ptr, combineMode))

    /**
     * Updates the clipping region of this Graphics object to a region that is the combination of itself
     * and the region specified by a Region object.
     */
    fun SetClip(region: Region?, combineMode: CombineMode = CombineModeReplace)
        = updateStatus(GdipSetClipRegion(ptr, region?.ptr, combineMode))

    /**
     * Updates the clipping region of this Graphics object to a region that is the combination of itself 
     * and a GDI region.
     */
    fun SetClip(hRgn: HRGN, combineMode: CombineMode = CombineModeReplace)
        = updateStatus(GdipSetClipHrgn(ptr, hRgn, combineMode))

    /**
     *  Sets the compositing mode of this Graphics object.
     */
    fun SetCompositingMode(compositingMode: CompositingMode)
        = updateStatus(GdipSetCompositingMode(ptr, compositingMode))

    /**
     * Sets the compositing quality of this Graphics object.
     */
    fun SetCompositingQuality(compositingQuality: CompositingQuality)
        = updateStatus(GdipSetCompositingQuality(ptr, compositingQuality))

    /**
     * Sets the interpolation mode of this Graphics object. The interpolation mode determines the algorithm
     * that is used when images are scaled or rotated.
     */
    fun SetInterpolationMode(interpolationMode: InterpolationMode)
        = updateStatus(GdipSetInterpolationMode(ptr, interpolationMode))

    /**
     * Sets the scaling factor for the page transformation of this Graphics object.
     * The page transformation converts page coordinates to device coordinates.
     */
    fun SetPageScale(scale: REAL)
        = updateStatus(GdipSetPageScale(ptr, scale))

    /**
     * Sets the unit of measure for this Graphics object. The page unit belongs to the page transformation,
     * which converts page coordinates to device coordinates.
     */
    fun SetPageUnit(unit: GpUnit)
        = updateStatus(GdipSetPageUnit(ptr, unit))

    /**
     * Sets the pixel offset mode of this Graphics object.
     */
    fun SetPixelOffsetMode(pixelOffsetMode: PixelOffsetMode)
        = updateStatus(GdipSetPixelOffsetMode(ptr, pixelOffsetMode))

    /**
     * Sets the rendering origin of this Graphics object. The rendering origin is used to set the dither
     * origin for 8-bits-per-pixel and 16-bits-per-pixel dithering and is also used to set the origin
     * for hatch brushes.
     */
    fun SetRenderingOrigin(x: INT, y: INT)
        = updateStatus(GdipSetRenderingOrigin(ptr, x, y))

    /**
     * Sets the rendering quality of the Graphics object.
     */
    fun SetSmoothingMode(smoothingMode: SmoothingMode)
        = updateStatus(GdipSetSmoothingMode(ptr, smoothingMode))

    /**
     * Sets the contrast value of this Graphics object. The contrast value is used for antialiasing text.
     */
    fun SetTextContrast(contrast: UINT)
        = updateStatus(GdipSetTextContrast(ptr, contrast))

    /**
     * Sets the text rendering mode of this Graphics object.
     */
    fun SetTextRenderingHint(textRenderingHint: TextRenderingHint)
        = updateStatus(GdipSetTextRenderingHint(ptr, textRenderingHint))

    /**
     * Sets the world transformation of this Graphics object.
     */
    fun SetTransform(matrix: Matrix?)
        = updateStatus(GdipSetWorldTransform(ptr, matrix?.ptr))

    /**
     * Converts an array of points from one coordinate space to another. The conversion is based on the
     * current world and page transformations of this Graphics object.
     */
    fun TransformPoints(destSpace: CoordinateSpace, srcSpace: CoordinateSpace, pts: PointF?, count: INT)
        = updateStatus(GdipTransformPoints(ptr, destSpace, srcSpace, pts?.ptr, count))

    /**
     * Converts an array of points from one coordinate space to another. The conversion is based on the
     * current world and page transformations of this Graphics object.
     */
    fun TransformPoints(destSpace: CoordinateSpace, srcSpace: CoordinateSpace, pts: Point?, count: INT)
        = updateStatus(GdipTransformPointsI(ptr, destSpace, srcSpace, pts?.ptr, count))

    /**
     * Translates the clipping region of this Graphics object.
     */
    fun TranslateClip(dx: REAL, dy: REAL)
        = updateStatus(GdipTranslateClip(ptr, dx, dy))

    /**
     * Translates the clipping region of this Graphics object.
     */
    fun TranslateClip(dx: INT, dy: INT)
        = updateStatus(GdipTranslateClipI(ptr, dx, dy))

    /**
     * Updates this Graphics object's world transformation matrix with the product of itself and a
     * translation matrix.
     */
    fun TranslateTransform(dx: REAL, dy: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipTranslateWorldTransform(ptr, dx, dy, order))
}

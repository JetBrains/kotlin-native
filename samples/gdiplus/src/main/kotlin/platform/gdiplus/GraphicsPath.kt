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
 * A GraphicsPath object stores a sequence of lines, curves, and shapes. You can draw the entire sequence
 * by calling the DrawPath method of a Graphics object. You can partition the sequence of lines, curves,
 * and shapes into figures, and with the help of a GraphicsPathIterator object, you can draw selected figures.
 * You can also place markers in the sequence, so that you can draw selected portions of the path.
 */
class GraphicsPath : GdipObject {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a GraphicsPath object and initializes the fill mode. This is the default constructor.
     */
    constructor(fillMode: FillMode = FillModeAlternate) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePath(fillMode, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a GraphicsPath object based on an array of points, an array of types, and a fill mode.
     */
    constructor(points: PointF, types: BYTEVar, count: INT, fillMode: FillMode = FillModeAlternate) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePath2(points.ptr, types.ptr, count, fillMode, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a GraphicsPath object based on an array of points, an array of types, and a fill mode.
     */
    constructor(points: Point, types: BYTEVar, count: INT, fillMode: FillMode = FillModeAlternate) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePath2I(points.ptr, types.ptr, count, fillMode, result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDeletePath(ptr)
    }

    /**
     * Creates a new GraphicsPath object, and initializes it with the contents of this GraphicsPath object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipClonePath(ptr, result.ptr))
        if (status == Ok) GraphicsPath(result.value!!, status) else null
    }

    /**
     * Adds an elliptical arc to the current figure of this path.
     */
    fun AddArc(x: REAL, y: REAL, width: REAL, height: REAL, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathArc(ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Adds an elliptical arc to the current figure of this path.
     */
    fun AddArc(x: INT, y: INT, width: INT, height: INT, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathArcI(ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Adds an elliptical arc to the current figure of this path.
     */
    fun AddArc(rect: RectF, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathArc(ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Adds an elliptical arc to the current figure of this path.
     */
    fun AddArc(rect: Rect, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathArcI(ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Adds a Bézier spline to the current figure of this path.
     */
    fun AddBezier(x1: REAL, y1: REAL, x2: REAL, y2: REAL, x3: REAL, y3: REAL, x4: REAL, y4: REAL)
        = updateStatus(GdipAddPathBezier(ptr, x1, y1, x2, y2, x3, y3, x4, y4))

    /**
     * Adds a Bézier spline to the current figure of this path.
     */
    fun AddBezier(x1: INT, y1: INT, x2: INT, y2: INT, x3: INT, y3: INT, x4: INT, y4: INT)
         = updateStatus(GdipAddPathBezierI(ptr, x1, y1, x2, y2, x3, y3, x4, y4))

    /**
     * Adds a Bézier spline to the current figure of this path.
     */
    fun AddBezier(pt1: PointF, pt2: PointF, pt3: PointF, pt4: PointF)
        = updateStatus(GdipAddPathBezier(ptr, pt1.X, pt1.Y, pt2.X, pt2.Y, pt3.X, pt3.Y, pt4.X, pt4.Y))

    /**
     * Adds a Bézier spline to the current figure of this path.
     */
    fun AddBezier(pt1: Point, pt2: Point, pt3: Point, pt4: Point)
        = updateStatus(GdipAddPathBezierI(ptr, pt1.X, pt1.Y, pt2.X, pt2.Y, pt3.X, pt3.Y, pt4.X, pt4.Y))

    /**
     * Adds a sequence of connected Bézier splines to the current figure of this path.
     */
    fun AddBeziers(points: PointF, count: INT)
        = updateStatus(GdipAddPathBeziers(ptr, points.ptr, count))

    /**
     * Adds a sequence of connected Bézier splines to the current figure of this path.
     */
    fun AddBeziers(points: Point, count: INT)
        = updateStatus(GdipAddPathBeziersI(ptr, points.ptr, count))

    /**
     * Adds a closed cardinal spline to this path.
     */
    fun AddClosedCurve(points: PointF, count: INT)
        = updateStatus(GdipAddPathClosedCurve(ptr, points.ptr, count))

    /**
     * Adds a closed cardinal spline to this path.
     */
    fun AddClosedCurve(points: Point, count: INT)
        = updateStatus(GdipAddPathClosedCurveI(ptr, points.ptr, count))

    /**
     * Adds a closed cardinal spline to this path.
     */
    fun AddClosedCurve(points: PointF, count: INT, tension: REAL)
        = updateStatus(GdipAddPathClosedCurve2(ptr, points.ptr, count, tension))

    /**
     * Adds a closed cardinal spline to this path.
     */
    fun AddClosedCurve(points: Point, count: INT, tension: REAL)
        = updateStatus(GdipAddPathClosedCurve2I(ptr, points.ptr, count, tension))

    /**
     * Adds a cardinal spline to the current figure of this path.
     */
    fun AddCurve(points: PointF, count: INT)
        = updateStatus(GdipAddPathCurve(ptr, points.ptr, count))

    /**
     * Adds a cardinal spline to the current figure of this path.
     */
    fun AddCurve(points: Point, count: INT)
        = updateStatus(GdipAddPathCurveI(ptr, points.ptr, count))

    /**
     * Adds a cardinal spline to the current figure of this path.
     */
    fun AddCurve(points: PointF, count: INT, tension: REAL)
        = updateStatus(GdipAddPathCurve2(ptr, points.ptr, count, tension))

    /**
     * Adds a cardinal spline to the current figure of this path.
     */
    fun AddCurve(points: Point, count: INT, tension: REAL)
        = updateStatus(GdipAddPathCurve2I(ptr, points.ptr, count, tension))

    /**
     * Adds a cardinal spline to the current figure of this path.
     */
    fun AddCurve(points: PointF, count: INT, offset: INT, numberOfSegments: INT, tension: REAL)
        = updateStatus(GdipAddPathCurve3(ptr, points.ptr, count, offset, numberOfSegments, tension))

    /**
     * Adds a cardinal spline to the current figure of this path.
     */
    fun AddCurve(points: Point, count: INT, offset: INT, numberOfSegments: INT, tension: REAL)
        = updateStatus(GdipAddPathCurve3I(ptr, points.ptr, count, offset, numberOfSegments, tension))

    /**
     * Adds an ellipse to this path.
     */
    fun AddEllipse(x: REAL, y: REAL, width: REAL, height: REAL)
        = updateStatus(GdipAddPathEllipse(ptr, x, y, width, height))

    /**
     * Adds an ellipse to this path.
     */
    fun AddEllipse(x: INT, y: INT, width: INT, height: INT)
        = updateStatus(GdipAddPathEllipseI(ptr, x, y, width, height))

    /**
     * Adds an ellipse to this path.
     */
    fun AddEllipse(rect: RectF)
        = updateStatus(GdipAddPathEllipse(ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Adds an ellipse to this path.
     */
    fun AddEllipse(rect: Rect)
        = updateStatus(GdipAddPathEllipseI(ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Adds a line to the current figure of this path.
     */
    fun AddLine(x1: REAL, y1: REAL, x2: REAL, y2: REAL)
        = updateStatus(GdipAddPathLine(ptr, x1, y1, x2, y2))

    /**
     * Adds a line to the current figure of this path.
     */
    fun AddLine(x1: INT, y1: INT, x2: INT, y2: INT)
        = updateStatus(GdipAddPathLineI(ptr, x1, y1, x2, y2))

    /**
     * Adds a line to the current figure of this path.
     */
    fun AddLine(pt1: PointF, pt2: PointF)
        = updateStatus(GdipAddPathLine(ptr, pt1.X, pt1.Y, pt2.X, pt2.Y))

    /**
     * Adds a line to the current figure of this path.
     */
    fun AddLine(pt1: Point, pt2: Point)
        = updateStatus(GdipAddPathLineI(ptr, pt1.X, pt1.Y, pt2.X, pt2.Y))

    /**
     * Adds a sequence of connected lines to the current figure of this path.
     */
    fun AddLines(points: PointF, count: INT)
        = updateStatus(GdipAddPathLine2(ptr, points.ptr, count))

    /**
     * Adds a sequence of connected lines to the current figure of this path.
     */
    fun AddLines(points: Point, count: INT)
        = updateStatus(GdipAddPathLine2I(ptr, points.ptr, count))

    /**
     * Adds a path to this path.
     */
    fun AddPath(addingPath: GraphicsPath?, connect: BOOL)
        = updateStatus(GdipAddPathPath(ptr, addingPath?.ptr, connect))

    /**
     * Adds a pie to this path. An arc is a portion of an ellipse, and a pie is a portion of the area
     * enclosed by an ellipse. A pie is bounded by an arc and two lines (edges) that go from the center
     * of the ellipse to the endpoints of the arc.
     */
    fun AddPie(x: REAL, y: REAL, width: REAL, height: REAL, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathPie(ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Adds a pie to this path. An arc is a portion of an ellipse, and a pie is a portion of the area
     * enclosed by an ellipse. A pie is bounded by an arc and two lines (edges) that go from the center
     * of the ellipse to the endpoints of the arc.
     */
    fun AddPie(x: INT, y: INT, width: INT, height: INT, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathPieI(ptr, x, y, width, height, startAngle, sweepAngle))

    /**
     * Adds a pie to this path. An arc is a portion of an ellipse, and a pie is a portion of the area
     * enclosed by an ellipse. A pie is bounded by an arc and two lines (edges) that go from the center
     * of the ellipse to the endpoints of the arc.
     */
    fun AddPie(rect: RectF, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathPie(ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Adds a pie to this path. An arc is a portion of an ellipse, and a pie is a portion of the area
     * enclosed by an ellipse. A pie is bounded by an arc and two lines (edges) that go from the center
     * of the ellipse to the endpoints of the arc.
     */
    fun AddPie(rect: Rect, startAngle: REAL, sweepAngle: REAL)
        = updateStatus(GdipAddPathPieI(ptr, rect.X, rect.Y, rect.Width, rect.Height, startAngle, sweepAngle))

    /**
     * Adds a polygon to this path.
     */
    fun AddPolygon(points: PointF, count: INT)
        = updateStatus(GdipAddPathPolygon(ptr, points.ptr, count))

    /**
     * Adds a polygon to this path.
     */
    fun AddPolygon(points: Point, count: INT)
        = updateStatus(GdipAddPathPolygonI(ptr, points.ptr, count))

    /**
     * Adds a rectangle to this path.
     */
    fun AddRectangle(rect: RectF)
        = updateStatus(GdipAddPathRectangle(ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Adds a rectangle to this path.
     */
    fun AddRectangle(rect: Rect)
        = updateStatus(GdipAddPathRectangleI(ptr, rect.X, rect.Y, rect.Width, rect.Height))

    /**
     * Adds a sequence of rectangles to this path
     */
    fun AddRectangles(rects: RectF, count: INT)
        = updateStatus(GdipAddPathRectangles(ptr, rects.ptr, count))

    /**
     * Adds a sequence of rectangles to this path
     */
    fun AddRectangles(rects: Rect, count: INT)
        = updateStatus(GdipAddPathRectanglesI(ptr, rects.ptr, count))

    /**
     * Adds the outlines of a string to this path.
     */
    fun AddString(string: String, length: INT, family: FontFamily?, style: INT, emSize: REAL, origin: PointF, format: StringFormat?): GpStatus = memScoped {
        val layoutRect = alloc<RectF>().apply { X = origin.X; Y = origin.Y; Width = 0.0f; Height = 0.0f }
        return updateStatus(GdipAddPathString(ptr, string.wcstr, length, family?.ptr, style, emSize, layoutRect.ptr, format?.ptr))
    }

    /**
     * Adds the outlines of a string to this path.
     */
    fun AddString(string: String, length: INT, family: FontFamily?, style: INT, emSize: REAL, origin: Point, format: StringFormat?): GpStatus = memScoped {
        val layoutRect = alloc<Rect>().apply { X = origin.X; Y = origin.Y; Width = 0; Height = 0 }
        return updateStatus(GdipAddPathStringI(ptr, string.wcstr, length, family?.ptr, style, emSize, layoutRect.ptr, format?.ptr))
    }

    /**
     * Adds the outlines of a string to this path.
     */
    fun AddString(string: String, length: INT, family: FontFamily?, style: INT, emSize: REAL, layoutRect: RectF, format: StringFormat?)
        = updateStatus(GdipAddPathString(ptr, string.wcstr, length, family?.ptr, style, emSize, layoutRect.ptr, format?.ptr))

    /**
     * Adds the outlines of a string to this path.
     */
    fun AddString(string: String, length: INT, family: FontFamily?, style: INT, emSize: REAL, layoutRect: Rect, format: StringFormat?)
        = updateStatus(GdipAddPathStringI(ptr, string.wcstr, length, family?.ptr, style, emSize, layoutRect.ptr, format?.ptr))

    /**
     * Clears the markers from this path.
     */
    fun ClearMarkers()
        = updateStatus(GdipClearPathMarkers(ptr))

    /**
     * Closes all open figures in this path.
     */
    fun CloseAllFigures()
        = updateStatus(GdipClosePathFigures(ptr))

    /**
     * Closes the current figure of this path.
     */
    fun CloseFigure()
        = updateStatus(GdipClosePathFigure(ptr))

    /**
     * Applies a transformation to this path and converts each curve in the path to a sequence of connected lines.
     */
    fun Flatten(matrix: Matrix? = null, flatness: REAL = FlatnessDefault)
        = updateStatus(GdipFlattenPath(ptr, matrix?.ptr, flatness))

    /**
     * Gets a bounding rectangle for this path.
     */
    fun GetBounds(bounds: RectF, matrix: Matrix? = null, pen: Pen? = null)
        = updateStatus(GdipGetPathWorldBounds(ptr, bounds.ptr, matrix?.ptr, pen?.ptr))

    /**
     * Gets a bounding rectangle for this path.
     */
    fun GetBounds(bounds: Rect, matrix: Matrix? = null, pen: Pen? = null)
        = updateStatus(GdipGetPathWorldBoundsI(ptr, bounds.ptr, matrix?.ptr, pen?.ptr))

    /**
     * Gets the fill mode of this path.
     */
    fun GetFillMode() = memScoped {
        val result = alloc<FillModeVar>().apply { value = FillModeAlternate }
        updateStatus(GdipGetPathFillMode(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the ending point of the last figure in this path.
     */
    fun GetLastPoint(lastPoint: PointF)
        = updateStatus(GdipGetPathLastPoint(ptr, lastPoint.ptr))

    /**
     * Gets an array of points and an array of point types from this path. Together, these two arrays define
     * the lines, curves, figures, and markers of this path.
     */
    fun GetPathData(pathData: PathData): GpStatus = memScoped {
        val count = alloc<INTVar>()
        val status1 = updateStatus(GdipGetPointCount(ptr, count.ptr))
        if (status1 != Ok) return status1
        val status2 = updateStatus(pathData.AllocateArrays(count.value))
        if (status2 != Ok) return status2
        return updateStatus(GdipGetPathData(ptr, pathData.ptr))
    }

    /**
     * Gets this path's array of points. The array contains the endpoints and control points of the lines
     * and Bézier splines that are used to draw the path.
     */
    fun GetPathPoints(points: PointF, count: INT)
        = updateStatus(GdipGetPathPoints(ptr, points.ptr, count))

    /**
     * Gets this path's array of points. The array contains the endpoints and control points of the lines
     * and Bézier splines that are used to draw the path.
     */
    fun GetPathPoints(points: Point, count: INT)
        = updateStatus(GdipGetPathPointsI(ptr, points.ptr, count))

    /**
     * Gets this path's array of point types.
     */
    fun GetPathTypes(types: BYTEVar, count: INT)
        = updateStatus(GdipGetPathTypes(ptr, types.ptr, count))

    /**
     * Gets the number of points in this path's array of data points.
     * This is the same as the number of types in the path's array of point types.
     */
    fun GetPointCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetPointCount(ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point touches the outline of this path when the path is drawn by a
     * specified Graphics object and a specified pen.
     */
    fun IsOutlineVisible(x: REAL, y: REAL, pen: Pen?, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsOutlineVisiblePathPoint(ptr, x, y, pen?.ptr, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point touches the outline of this path when the path is drawn by a
     * specified Graphics object and a specified pen.
     */
    fun IsOutlineVisible(x: INT, y: INT, pen: Pen?, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsOutlineVisiblePathPointI(ptr, x, y, pen?.ptr, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point touches the outline of this path when the path is drawn by a
     * specified Graphics object and a specified pen.
     */
    fun IsOutlineVisible(point: PointF, pen: Pen?, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsOutlineVisiblePathPoint(ptr, point.X, point.Y, pen?.ptr, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point touches the outline of this path when the path is drawn by a
     * specified Graphics object and a specified pen.
     */
    fun IsOutlineVisible(point: Point, pen: Pen?, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsOutlineVisiblePathPointI(ptr, point.X, point.Y, pen?.ptr, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point lies in the area that is filled when this path is filled
     * by a specified Graphics object.
     */
    fun IsVisible(x: REAL, y: REAL, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePathPoint(ptr, x, y, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point lies in the area that is filled when this path is filled
     * by a specified Graphics object.
     */
    fun IsVisible(x: INT, y: INT, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePathPointI(ptr, x, y, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point lies in the area that is filled when this path is filled
     * by a specified Graphics object.
     */
    fun IsVisible(point: PointF, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePathPoint(ptr, point.X, point.Y, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether a specified point lies in the area that is filled when this path is filled
     * by a specified Graphics object.
     */
    fun IsVisible(point: Point, g: Graphics? = null) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsVisiblePathPointI(ptr, point.X, point.Y, g?.ptr, result.ptr))
        result.value
    }

    /**
     * Transforms and flattens this path, and then converts this path's data points so that they represent
     * only the outline of the path.
     */
    fun Outline(matrix: Matrix? = null, flatness: REAL = FlatnessDefault)
        = updateStatus(GdipWindingModeOutline(ptr, matrix?.ptr, flatness))

    /**
     * Empties the path and sets the fill mode to FillModeAlternate.
     */
    fun Reset()
        = updateStatus(GdipResetPath(ptr))

    /**
     * Reverses the order of the points that define this path's lines and curves.
     */
    fun Reverse()
        = updateStatus(GdipReversePath(ptr))

    /**
     * Sets the fill mode of this path.
     */
    fun SetFillMode(fillMode: FillMode)
        = updateStatus(GdipSetPathFillMode(ptr, fillMode))

    /**
     * Designates the last point in this path as a marker point.
     */
    fun SetMarker()
        = updateStatus(GdipSetPathMarker(ptr))

    /**
     * Starts a new figure without closing the current figure. Subsequent points added to this path
     * are added to the new figure.
     */
    fun StartFigure()
        = updateStatus(GdipStartPathFigure(ptr))

    /**
     * Multiplies each of this path's data points by a specified matrix.
     */
    fun Transform(matrix: Matrix?)
        = updateStatus(GdipTransformPath(ptr, matrix?.ptr))

    /**
     * Applies a warp transformation to this path. Also flattens (converts to a sequence of straight lines) the path.
     */
    fun Warp(destPoints: PointF, count: INT, srcRect: RectF, matrix: Matrix? = null,
             warpMode: WarpMode = WarpModePerspective, flatness: REAL = FlatnessDefault)
        = updateStatus(GdipWarpPath(ptr, matrix?.ptr, destPoints.ptr, count,
                       srcRect.X, srcRect.Y, srcRect.Width, srcRect.Height, warpMode, flatness))

    /**
     * Replaces this path with curves that enclose the area that is filled when this path is drawn by
     * a specified pen. Also flattens the path.
     */
    fun Widen(pen: Pen?, matrix: Matrix? = null, flatness: REAL = FlatnessDefault)
        = updateStatus(GdipWidenPath(ptr, pen?.ptr, matrix?.ptr, flatness))
}

fun PathData.AllocateArrays(capacity: INT): GpStatus {
    if (capacity < 0) {
        return InvalidParameter
    } else if (Count < capacity) {
        FreeArrays()
        val pointArray: CPointer<PointF>?
            = GdipAlloc((capacity * sizeOf<PointF>()).signExtend())?.reinterpret()
        if (pointArray == null)
            return OutOfMemory
        val typeArray: CPointer<BYTEVar>?
            = GdipAlloc((capacity * sizeOf<BYTEVar>()).signExtend())?.reinterpret()
        if (typeArray == null) {
            GdipFree(pointArray)
            return OutOfMemory
        }
        Count = capacity
        Points = pointArray
        Types = typeArray
    }
    return Ok
}

fun PathData.FreeArrays() {
    if (Points != null) GdipFree(Points)
    if (Types != null) GdipFree(Types)
    Count = 0
    Points = null
    Types = null
}

/**
 * This GraphicsPathIterator class provides methods for isolating selected subsets of the path stored in
 * a GraphicsPath object. A path consists of one or more figures. You can use a GraphicsPathIterator to isolate
 * one or more of those figures. A path can also have markers that divide the path into sections.
 * You can use a GraphicsPathIterator object to isolate one or more of those sections.
 */
class GraphicsPathIterator : GdipObject {

    /**
     * Creates a new GraphicsPathIterator object and associates it with a GraphicsPath object.
     */
    constructor(path: GraphicsPath?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreatePathIter(result.ptr, path?.ptr)
            ptr = result.value
        }
    }

    override fun Clone() = TODO()

    override fun Dispose() {
        GdipDeletePathIter(ptr)
    }

    /**
     * Copies a subset of the path's data points to a PointF array and copies a subset of the
     * path's point types to a BYTE array.
     */
    fun CopyData(points: PointF, types: BYTEVar, startIndex: INT, endIndex: INT) = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterCopyData(ptr, result.ptr, points.ptr, types.ptr, startIndex, endIndex))
        result.value
    }

    /**
     * Copies the path's data points to a PointF array and copies the path's point types to a BYTE array.
     */
    fun Enumerate(points: PointF, types: BYTEVar, count: INT) = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterEnumerate(ptr, result.ptr, points.ptr, types.ptr, count))
        result.value
    }

    /**
     * Returns the number of data points in the path.
     */
    fun GetCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterGetCount(ptr, result.ptr))
        result.value
    }

    /**
     * Returns the number of subpaths (also called figures) in the path.
     */
    fun GetSubpathCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterGetSubpathCount(ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether the path has any curves.
     */
    fun HasCurve() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipPathIterHasCurve(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the starting index and the ending index of the next marker-delimited section in this
     * iterator's associated path.
     */
    fun NextMarker(startIndex: INTVar, endIndex: INTVar) = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterNextMarker(ptr, result.ptr, startIndex.ptr, endIndex.ptr))
        result.value
    }

    /**
     * Gets the next marker-delimited section of this iterator's associated path.
     */
    fun NextMarker(path: GraphicsPath?) = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterNextMarkerPath(ptr, result.ptr, path?.ptr))
        result.value
    }

    /**
     * Gets the starting index and the ending index of the next group of data points that all have the same type.
     */
    fun NextPathType(pathType: BYTEVar, startIndex: INTVar, endIndex: INTVar) = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterNextPathType(ptr, result.ptr, pathType.ptr, startIndex.ptr, endIndex.ptr))
        result.value
    }

    /**
     * Gets the starting index and the ending index of the next subpath (figure) in this iterator's associated path.
     */
    fun NextSubpath(startIndex: INTVar, endIndex: INTVar, isClosed: BOOLVar) = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterNextSubpath(ptr, result.ptr, startIndex.ptr, endIndex.ptr, isClosed.ptr))
        result.value
    }

    /**
     * Gets the next figure (subpath) from this iterator's associated path.
     */
    fun NextSubpath(path: GraphicsPath?, isClosed: BOOLVar) = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipPathIterNextSubpathPath(ptr, result.ptr, path?.ptr, isClosed.ptr))
        result.value
    }

    /**
     * Rewinds this iterator to the beginning of its associated path.
     */
    fun Rewind()
        = updateStatus(GdipPathIterRewind(ptr))
}

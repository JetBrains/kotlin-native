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
import platform.posix.sin
import platform.posix.cos

private const val PI = 3.1415926535897932384626433832795028841971693993751058209749445923078164

/**
 * A Matrix object represents a 3 ×3 matrix that, in turn, represents an affine transformation.
 * A Matrix object stores only six of the 9 numbers in a 3 × 3 matrix because all 3 × 3 matrices
 * that represent affine transformations have the same third column (0, 0, 1).
 */
class Matrix : GdipObject {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates and initializes a Matrix object that represents the identity matrix.
     */
    constructor() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMatrix(result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates and initializes a Matrix object based on six numbers that define an affine transformation.
     */
    constructor(m11: REAL, m12: REAL, m21: REAL, m22: REAL, dx: REAL, dy: REAL) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMatrix2(m11, m12, m21, m22, dx, dy, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Matrix object based on a rectangle and a point.
     */
    constructor(rect: RectF, dstplg: PointF) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMatrix3(rect.ptr, dstplg.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Matrix object based on a rectangle and a point.
     */
    constructor(rect: Rect, dstplg: Point) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMatrix3I(rect.ptr, dstplg.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDeleteMatrix(ptr)
    }

    /**
     * Creates a new Matrix object that is a copy of this Matrix object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneMatrix(ptr, result.ptr))
        if (status == Ok) Matrix(result.value!!, status) else null
    }

    /**
     * Determines whether the elements of this matrix are equal to the elements of another matrix.
     */
    fun Equals(matrix: Matrix?) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsMatrixEqual(ptr, matrix?.ptr, result.ptr))
        result.value
    }

    /**
     * Gets the elements of this matrix. The elements are placed in an array in the order
     * m11, m12, m21, m22, m31, m32, where mij denotes the element in row i, column j.
     */
    fun GetElements(m: REALVar)
        = updateStatus(GdipGetMatrixElements(ptr, m.ptr))

    /**
     * If this matrix is invertible, replaces the elements of this matrix with the elements of its inverse.
     */
    fun Invert()
        = updateStatus(GdipInvertMatrix(ptr))

    /**
     * Determines whether this matrix is the identity matrix.
     */
    fun IsIdentity() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsMatrixIdentity(ptr, result.ptr))
        result.value
    }

    /**
     * Determines whether this matrix is invertible.
     */
    fun IsInvertible() = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsMatrixInvertible(ptr, result.ptr))
        result.value
    }

    /**
     * Updates this matrix with the product of itself and another matrix.
     */
    fun Multiply(matrix: Matrix?, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipMultiplyMatrix(ptr, matrix?.ptr, order))

    /**
     * Gets the horizontal translation value of this matrix, which is the element in row 3, column 1.
     */
    fun OffsetX(): REAL = memScoped {
        val m = allocArray<REALVar>(6)
        updateStatus(GdipGetMatrixElements(ptr, m))
        return m[4]
    }

    /**
     * Gets the vertical translation value of this matrix, which is the element in row 3, column 2.
     */
    fun OffsetY(): REAL = memScoped {
        val m = allocArray<REALVar>(6)
        updateStatus(GdipGetMatrixElements(ptr, m))
        return m[5]
    }

    /**
     * Updates this matrix with the elements of the identity matrix.
     */
    fun Reset()
        = updateStatus(GdipSetMatrixElements(ptr, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f))

    /**
     * Updates this matrix with the product of itself and a rotation matrix.
     */
    fun Rotate(angle: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipRotateMatrix(ptr, angle, order))

    /**
     * Updates this matrix with the product of itself and a matrix that represents rotation about a specified point.
     */
    fun RotateAt(angle: REAL, center: PointF, order: MatrixOrder = MatrixOrderPrepend) = memScoped {
        val angleRadian = angle * PI / 180.0f
        val cosAngle = cos(angleRadian).toFloat()
        val sinAngle = sin(angleRadian).toFloat()
        val x = center.X
        val y = center.Y
        val matrix2 = Matrix(cosAngle, sinAngle, -sinAngle, cosAngle,
                x * (1.0f-cosAngle) + y * sinAngle, -x * sinAngle + y * (1.0f - cosAngle))
        if (updateStatus(matrix2.GetLastStatus()) == Ok) Multiply(matrix2, order) else lastStatus
    }

    /**
     * Updates this matrix with the product of itself and a scaling matrix.
     */
    fun Scale(scaleX: REAL, scaleY: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipScaleMatrix(ptr, scaleX, scaleY, order))

    /**
     * Sets the elements of this matrix.
     */
    fun SetElements(m11: REAL, m12: REAL, m21: REAL, m22: REAL, dx: REAL, dy: REAL)
        = updateStatus(GdipSetMatrixElements(ptr, m11, m12, m21, m22, dx, dy))

    /**
     * Updates this matrix with the product of itself and a shearing matrix.
     */
    fun Shear(shearX: REAL, shearY: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipShearMatrix(ptr, shearX, shearY, order))

    /**
     * Multiplies each point in an array by this matrix. Each point is treated as a row matrix.
     * The multiplication is performed with the row matrix on the left and this matrix on the right.
     */
    fun TransformPoints(pts: PointF, count: INT = 1)
        = updateStatus(GdipTransformMatrixPoints(ptr, pts.ptr, count))

    /**
     * Multiplies each point in an array by this matrix. Each point is treated as a row matrix.
     * The multiplication is performed with the row matrix on the left and this matrix on the right.
     */
    fun TransformPoints(pts: Point, count: INT = 1)
        = updateStatus(GdipTransformMatrixPointsI(ptr, pts.ptr, count))

    /**
     * Multiplies each vector in an array by this matrix. The translation elements of this matrix
     * (third row) are ignored. Each vector is treated as a row matrix. The multiplication is performed
     * with the row matrix on the left and this matrix on the right.
     */
    fun TransformVectors(pts: PointF, count: INT = 1)
        = updateStatus(GdipVectorTransformMatrixPoints(ptr, pts.ptr, count))

    /**
     * Multiplies each vector in an array by this matrix. The translation elements of this matrix
     * (third row) are ignored. Each vector is treated as a row matrix. The multiplication is performed
     * with the row matrix on the left and this matrix on the right.
     */
    fun TransformVectors(pts: Point, count: INT = 1)
        = updateStatus(GdipVectorTransformMatrixPointsI(ptr, pts.ptr, count))

    /**
     * Updates this matrix with the product of itself and a translation matrix.
     */
    fun Translate(offsetX: REAL, offsetY: REAL, order: MatrixOrder = MatrixOrderPrepend)
        = updateStatus(GdipTranslateMatrix(ptr, offsetX, offsetY, order))
}

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
import platform.posix.GUID
import platform.posix.CLSID

/**
 * The Image class provides methods for loading and saving raster images (bitmaps) and vector images (metafiles).
 * An Image object encapsulates a bitmap or a metafile and stores attributes that you can retrieve
 * by calling various Get methods. You can construct Image objects from a variety of file types
 * including BMP, ICON, GIF, JPEG, Exif, PNG, TIFF, WMF, and EMF.
 */
open class Image : GdipObject {
    protected constructor(ptr: COpaquePointer? = null, status: GpStatus = Ok) : super(ptr, status) {}

    companion object {
        /**
         * Creates an Image object based on a file.
         */
        fun FromFile(filename: String, useEmbeddedColorManagement: Boolean = false)
                = Image(filename, useEmbeddedColorManagement)

        /**
         * Creates a new Image object based on a stream.
         */
        fun FromStream(stream: IStream, useEmbeddedColorManagement: Boolean = false)
                = Image(stream, useEmbeddedColorManagement)
    }

    /**
     * Creates an Image object based on a file.
     */
    constructor(filename: String, useEmbeddedColorManagement: Boolean = false) : this() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = if (useEmbeddedColorManagement)
                GdipLoadImageFromFileICM(filename.wcstr, result.ptr)
            else GdipLoadImageFromFile(filename.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates an Image object based on a stream.
     */
    constructor(stream: IStream, useEmbeddedColorManagement: Boolean = false) : this() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = if (useEmbeddedColorManagement)
                GdipLoadImageFromStreamICM(stream.ptr, result.ptr)
            else GdipLoadImageFromStream(stream.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDisposeImage(ptr)
    }

    /**
     * Creates a new Image object and initializes it with the contents of this Image object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneImage(ptr, result.ptr))
        if (status == Ok) Image(result.value!!, status) else null
    }

    /**
     * Retrieves the description and the data size of the first metadata item in this Image object.
     */
    fun FindFirstItem(item: ImageItemData)
            = updateStatus(GdipFindFirstImageItem(ptr, item.ptr))

    /**
     * Used along with the Image::FindFirstItem method to enumerate the metadata items stored in this
     * Image object. The Image::FindNextItem method retrieves the description and the data size of the
     * next metadata item in this Image object.
     */
    fun FindNextItem(item: ImageItemData)
            = updateStatus(GdipFindNextImageItem(ptr, item.ptr))

    /**
     * Gets all the property items (metadata) stored in this Image object.
     */
    fun GetAllPropertyItems(totalBufferSize: UINT, numProperties: UINT, allItems: PropertyItem)
            = updateStatus(GdipGetAllPropertyItems(ptr, totalBufferSize, numProperties, allItems.ptr))

    /**
     * Gets the bounding rectangle for this image.
     */
    fun GetBounds(srcRect: RectF, srcUnit: GpUnitVar)
            = updateStatus(GdipGetImageBounds(ptr, srcRect.ptr, srcUnit.ptr))

    /**
     * Gets a list of the parameters supported by a specified image encoder.
     */
    fun GetEncoderParameterList(clsidEncoder: CLSID, size: UINT, buffer: EncoderParameters)
            = updateStatus(GdipGetEncoderParameterList(ptr, clsidEncoder.ptr, size, buffer.ptr))

    /**
     * Gets the size, in bytes, of the parameter list for a specified image encoder.
     */
    fun GetEncoderParameterListSize(clsidEncoder: CLSID) = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetEncoderParameterListSize(ptr, clsidEncoder.ptr, result.ptr))
        result.value
    }

    /**
     * Gets a set of flags that indicate certain attributes of this Image object.
     */
    fun GetFlags() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetImageFlags(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the number of frames in a specified dimension of this Image object.
     */
    fun GetFrameCount(dimensionID: GUID) = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipImageGetFrameCount(ptr, dimensionID.ptr, result.ptr))
        result.value
    }

    /**
     * Gets the number of frame dimensions in this Image object.
     */
    fun GetFrameDimensionsCount() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipImageGetFrameDimensionsCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the identifiers for the frame dimensions of this Image object.
     */
    fun GetFrameDimensionsList(dimensionIDs: GUID, count: UINT)
            = updateStatus(GdipImageGetFrameDimensionsList(ptr, dimensionIDs.ptr, count))

    /**
     * Gets the image height, in pixels, of this image.
     */
    fun GetHeight() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetImageHeight(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the horizontal resolution, in dots per inch, of this image.
     */
    fun GetHorizontalResolution() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetImageHorizontalResolution(ptr, result.ptr))
        result.value
    }

    /**
     * Gets one piece of metadata from this Image object.
     */
    fun GetItemData(item: ImageItemData)
            = updateStatus(GdipGetImageItemData(ptr, item.ptr))

    /**
     * Gets the ColorPalette of this Image object.
     */
    fun GetPalette(palette: ColorPalette, size: INT)
            = updateStatus(GdipGetImagePalette(ptr, palette.ptr, size))

    /**
     * Gets the size, in bytes, of the color palette of this Image object.
     */
    fun GetPaletteSize() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetImagePaletteSize(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the width and height of this image.
     */
    fun GetPhysicalDimension(size: SizeF)
        = updateStatus(GdipGetImageDimension(ptr, size.memberAt<REALVar>(0).ptr, size.memberAt<REALVar>(4).ptr))

    /**
     * Gets the pixel format of this Image object.
     */
    fun GetPixelFormat() = memScoped {
        val result = alloc<PixelFormatVar>().apply { value = 0 }
        updateStatus(GdipGetImagePixelFormat(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the number of properties (pieces of metadata) stored in this Image object.
     */
    fun GetPropertyCount() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetPropertyCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets a list of the property identifiers used in the metadata of this Image object.
     */
    fun GetPropertyIdList(numOfProperty: UINT, list: PROPIDVar)
        = updateStatus(GdipGetPropertyIdList(ptr, numOfProperty, list.ptr))

    /**
     * Gets a specified property item (piece of metadata) from this Image object.
     */
    fun GetPropertyItem(propId: PROPID, propSize: UINT, buffer: PropertyItem)
        = updateStatus(GdipGetPropertyItem(ptr, propId, propSize, buffer.ptr))

    /**
     * Gets the size, in bytes, of a specified property item of this Image object.
     */
    fun GetPropertyItemSize(propId: PROPID) = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetPropertyItemSize(ptr, propId, result.ptr))
        result.value
    }

    /**
     * Gets the total size, in bytes, of all the property items stored in this Image object.
     * Also gets the number of property items stored in this Image object.
     */
    fun GetPropertySize(totalBufferSize: UINTVar, numProperties: UINTVar)
        = updateStatus(GdipGetPropertySize(ptr, totalBufferSize.ptr, numProperties.ptr))

    /**
     * Gets a globally unique identifier (  GUID) that identifies the format of this Image object.
     * GUIDs that identify various file formats are defined in Gdiplusimaging.h.
     */
    fun GetRawFormat(format: GUID)
        = updateStatus(GdipGetImageRawFormat(ptr, format.ptr))

    /**
     * Gets a thumbnail image from this Image object.
     */
    fun GetThumbnailImage(thumbWidth: UINT, thumbHeight: UINT, callback: GetThumbnailImageAbort,
                          callbackData: COpaquePointer?): Image? = memScoped {
        val thumbImage = alloc<COpaquePointerVar>().apply { value = null }
        val status = updateStatus(GdipGetImageThumbnail(ptr, thumbWidth, thumbHeight,
                                            thumbImage.ptr, callback, callbackData))
        if (status == Ok) Image(thumbImage.value, Ok) else null
    }

    /**
     * Gets the type (bitmap or metafile) of this Image object.
     */
    fun GetType() = memScoped {
        val result = alloc<ImageTypeVar>().apply { value = ImageTypeUnknown }
        updateStatus(GdipGetImageType(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the vertical resolution, in dots per inch, of this image.
     */
    fun GetVerticalResolution() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetImageVerticalResolution(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the width, in pixels, of this image.
     */
    fun GetWidth() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetImageWidth(ptr, result.ptr))
        result.value
    }

    /**
     * Removes a property item (piece of metadata) from this Image object.
     */
    fun RemovePropertyItem(propId: PROPID)
        = updateStatus(GdipRemovePropertyItem(ptr, propId))

    /**
     * Rotates and flips this image.
     */
    fun RotateFlip(rotateFlipType: RotateFlipType)
        = updateStatus(GdipImageRotateFlip(ptr, rotateFlipType))

    /**
     * Saves this image to a file.
     */
    fun Save(filename: String, clsidEncoder: CLSID, encoderParams: EncoderParameters)
        = updateStatus(GdipSaveImageToFile(ptr, filename.wcstr, clsidEncoder.ptr, encoderParams.ptr))

    /**
     * Saves this image to a stream.
     */
    fun Save(stream: IStream, clsidEncoder: CLSID, encoderParams: EncoderParameters)
        = updateStatus(GdipSaveImageToStream(ptr, stream.ptr, clsidEncoder.ptr, encoderParams.ptr))

    /**
     * Adds a frame to a file or stream specified in a previous call to the Save method. Use this method
     * to save selected frames from a multiple-frame image to another multiple-frame image.
     */
    fun SaveAdd(encoderParams: EncoderParameters)
        = updateStatus(GdipSaveAdd(ptr, encoderParams.ptr))

    /**
     * Adds a frame to a file or stream specified in a previous call to the Save method.
     */
    fun SaveAdd(newImage: Image?, encoderParams: EncoderParameters)
        = updateStatus(GdipSaveAddImage(ptr, newImage?.ptr, encoderParams.ptr))

    /**
     * Selects the frame in this Image object specified by a dimension and an index.
     */
    fun SelectActiveFrame(dimensionID: GUID, frameIndex: UINT)
        = updateStatus(GdipImageSelectActiveFrame(ptr, dimensionID.ptr, frameIndex))

//TODO /**
//      * Sets the object whose Abort method is called periodically during time-consuming rendering operation.
//      */
//     fun SetAbort(pIAbort: GdiplusAbort)
//         = updateStatus(GdipImageSetAbort(ptr, pIAbort))

    /**
     * Sets the color palette of this Image object.
     */
    fun SetPalette(palette: ColorPalette)
        = updateStatus(GdipSetImagePalette(ptr, palette.ptr))

    /**
     * Sets a property item (piece of metadata) for this Image object. If the item already exists, then
     * its contents are updated; otherwise, a new item is added.
     */
    fun SetPropertyItem(item: PropertyItem)
        = updateStatus(GdipSetPropertyItem(ptr, item.ptr))
}

/**
 * An ImageAttributes object contains information about how bitmap and metafile colors are manipulated
 * during rendering. An ImageAttributes object maintains several color-adjustment settings, including
 * color-adjustment matrices, grayscale-adjustment matrices, gamma-correction values, color-map tables,
 * and color-threshold values.
 */
class ImageAttributes : GdipObject {
    protected constructor(ptr: COpaquePointer? = null, status: GpStatus = Ok) : super(ptr, status) {}

    /**
     * Creates an ImageAttributes object. This is the default constructor.
     */
    constructor(){
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateImageAttributes(result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDisposeImageAttributes(ptr)
    }

    /**
     * Makes a copy of this ImageAttributes object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneImageAttributes(ptr, result.ptr))
        if (status == Ok) ImageAttributes(result.value!!, status) else null
    }

    /**
     * Clears the brush color-remap table of this ImageAttributes object.
     */
    fun ClearBrushRemapTable()
        = updateStatus(GdipSetImageAttributesRemapTable(ptr, ColorAdjustTypeBrush, FALSE, 0, null))

    /**
     * Clears the color key (transparency range) for a specified category.
     */
    fun ClearColorKey(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesColorKeys(ptr, type, FALSE, 0, 0))

    /**
     * Clears the color-adjustment matrix and the grayscale-adjustment matrix for a specified category.
     */
    fun ClearColorMatrices(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesColorMatrix(ptr, type, FALSE, null, null, ColorMatrixFlagsDefault))

    /**
     * Clears the color-adjustment matrix for a specified category.
     */
    fun ClearColorMatrix(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesColorMatrix(ptr, type, FALSE, null, null, ColorMatrixFlagsDefault))

    /**
     * Disables gamma correction for a specified category.
     */
    fun ClearGamma(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesGamma(ptr, type, FALSE, 1.0f))

    /**
     * Clears the NoOp setting for a specified category.
     */
    fun ClearNoOp(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesNoOp(ptr, type, FALSE))

    /**
     * Clears the CMYK output channel setting for a specified category.
     */
    fun ClearOutputChannel(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesOutputChannel(ptr, type, FALSE, ColorChannelFlagsC))

    /**
     * Clears the output channel color profile setting for a specified category.
     */
    fun ClearOutputChannelColorProfile(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesOutputChannelColorProfile(ptr, type, FALSE, null))

    /**
     * Clears the color-remap table for a specified category.
     */
    fun ClearRemapTable(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesRemapTable(ptr, type, FALSE, 0, null))

    /**
     * Clears the threshold value for a specified category.
     */
    fun ClearThreshold(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesThreshold(ptr, type, FALSE, 0.0f))

    /**
     * Adjusts the colors in a palette according to the adjustment settings of a specified category.
     */
    fun GetAdjustedPalette(colorPalette: ColorPalette, type: ColorAdjustType)
        = updateStatus(GdipGetImageAttributesAdjustedPalette(ptr, colorPalette.ptr, type))

    /**
     * Clears all color- and grayscale-adjustment settings for a specified category.
     */
    fun Reset(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipResetImageAttributes(ptr, type))

    /**
     * Sets the color remap table for the brush category.
     */
    fun SetBrushRemapTable(mapSize: UINT, map: ColorMap)
        = updateStatus(GdipSetImageAttributesRemapTable(ptr, ColorAdjustTypeBrush, TRUE, mapSize, map.ptr))

    /**
     * Sets the color key (transparency range) for a specified category.
     */
    fun SetColorKey(colorLow: Color, colorHigh: Color, type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesColorKeys(ptr, type, TRUE, colorLow.Value, colorHigh.Value))

    /**
     * Sets the color-adjustment matrix and the grayscale-adjustment matrix for a specified category.
     */
    fun SetColorMatrices(colorMatrix: ColorMatrix, grayMatrix: ColorMatrix,
            mode: ColorMatrixFlags = ColorMatrixFlagsDefault,
            type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesColorMatrix(ptr, type, TRUE, colorMatrix.ptr, grayMatrix.ptr, mode))

    /**
     * Sets the color-adjustment matrix for a specified category.
     */
    fun SetColorMatrix(colorMatrix: ColorMatrix,
            mode: ColorMatrixFlags = ColorMatrixFlagsDefault,
            type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesColorMatrix(ptr, type, TRUE, colorMatrix.ptr, null, mode))

    /**
     * Sets the gamma value for a specified category.
     */
    fun SetGamma(gamma: REAL, type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesGamma(ptr, type, TRUE, gamma))

    /**
     * Turns off color adjustment for a specified category. You can call ImageAttributes::ClearNoOp to reinstate the color-adjustment settings that were in place before the call to ImageAttributes::SetNoOp.
     */
    fun SetNoOp(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesNoOp(ptr, type, TRUE))

    /**
     * Sets the CMYK output channel for a specified category.
     */
    fun SetOutputChannel(channelFlags: ColorChannelFlags, type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesOutputChannel(ptr, type, TRUE, channelFlags))

    /**
     * Sets the output channel color-profile file for a specified category.
     */
    fun SetOutputChannelColorProfile(colorProfileFilename: String, type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesOutputChannelColorProfile(ptr, type, TRUE, colorProfileFilename.wcstr))

    /**
     * Sets the color-remap table for a specified category.
     */
    fun SetRemapTable(mapSize: UINT, map: ColorMap, type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesRemapTable(ptr, type, TRUE, mapSize, map.ptr))

    /**
     * Sets the threshold (transparency range) for a specified category.
     */
    fun SetThreshold(threshold: REAL, type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesThreshold(ptr, type, TRUE, threshold))

    /**
     * Sets the color-adjustment matrix of a specified category to identity matrix.
     */
    fun SetToIdentity(type: ColorAdjustType = ColorAdjustTypeDefault)
        = updateStatus(GdipSetImageAttributesToIdentity(ptr, type))

    /**
     * Sets the wrap mode of this ImageAttributes object.
     */
    fun SetWrapMode(wrap: WrapMode, color: Color? = null, clamp: Boolean = false)
        = updateStatus(GdipSetImageAttributesWrapMode(ptr, wrap, color?.Value ?: 0, if (clamp) TRUE else FALSE))
}

/**
 * The Bitmap class inherits from the Image class. The Image class provides methods for loading and saving
 * vector images (metafiles) and raster images (bitmaps). The Bitmap class expands on the capabilities
 * of the Image class by providing additional methods for creating and manipulating raster images.
 */
class Bitmap : Image {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    companion object {

        /**
         * Creates a Bitmap object based on a BITMAPINFO structure and an array of pixel data.
         */
        fun FromBITMAPINFO(gdiBitmapInfo: BITMAPINFO, gdiBitmapData: COpaquePointer)
            = Bitmap(gdiBitmapInfo, gdiBitmapData)

//TODO  /**
//       * Creates a Bitmap object based on a DirectDraw surface. The Bitmap object maintains a reference
//       * to the DirectDraw surface until the Bitmap object is deleted.
//       */
//      fun FromDirectDrawSurface7(surface: IDirectDrawSurface7)
//          = Bitmap(surface)

        /**
         * Creates a Bitmap object based on an image file.
         */
        fun FromFile(filename: String, useEmbeddedColorManagement: Boolean = false)
            = Bitmap(filename, useEmbeddedColorManagement)

        /**
         * Creates a Bitmap object based on a handle to a Windows GDI bitmap and a handle to a GDI palette.
         */
        fun FromHBITMAP(hbm: HBITMAP, hpal: HPALETTE)
            = Bitmap(hbm, hpal)

        /**
         * Creates a Bitmap object based on a handle to an icon.
         */
        fun FromHICON(icon: HICON)
            = Bitmap(icon)

        /**
         * Creates a Bitmap object based on an application or DLL instance handle and the name of a bitmap resource.
         */
        fun FromResource(hInstance: HINSTANCE, bitmapName: String)
            = Bitmap(hInstance, bitmapName)

        /**
         * Creates a Bitmap object based on a stream.
         */
        fun FromStream(stream: IStream, useEmbeddedColorManagement: Boolean = false)
            = Bitmap(stream, useEmbeddedColorManagement)

//TODO  /**
//       * Creates a new Bitmap object by applying a specified effect to an existing Bitmap object.
//       */
//      fun ApplyEffect(Bitmap **inputs, numInputs: INT, effect: Effect, ROI: RECT,
//                      outputRect: RECT, Bitmap **output)
//          = NotImplemented

        /**
         * Initializes a standard, optimal, or custom color palette.
         */
        fun InitializePalette(palette: ColorPalette, paletteType: PaletteType, optimalColors: INT,
                              useTransparentColor: BOOL, bitmap: Bitmap?)
            = GdipInitializePalette(palette.ptr, paletteType, optimalColors, useTransparentColor, bitmap?.ptr)
    }

    /**
     * Creates a Bitmap object based on a BITMAPINFO structure and an array of pixel data.
     */
    constructor(gdiBitmapInfo: BITMAPINFO, gdiBitmapData: COpaquePointer) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateBitmapFromGdiDib(gdiBitmapInfo.ptr, gdiBitmapData, result.ptr)
            ptr = result.value
        }
    }

//TODO  /**
//       * Creates a Bitmap object based on a DirectDraw surface. The Bitmap object maintains a reference to the
//       * DirectDraw surface until the Bitmap object is deleted or goes out of scope.
//       */
//      constructor(surface: IDirectDrawSurface7) {
//          memScoped {
//              val result = alloc<COpaquePointerVar>()
//              lastStatus = GdipCreateBitmapFromDirectDrawSurface(surface, result.ptr)
//              ptr = result.value
//          }
//      }

    /**
     * Creates a Bitmap object based on an image file.
     */
    constructor(filename: String, useEmbeddedColorManagement: Boolean = false) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = if (useEmbeddedColorManagement)
                              GdipCreateBitmapFromFileICM(filename.wcstr, result.ptr)
                         else GdipCreateBitmapFromFile(filename.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Bitmap object based on a handle to a Windows GDI bitmap and a handle to a GDI palette.
     */
    constructor(hbm: HBITMAP, hpal: HPALETTE) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateBitmapFromHBITMAP(hbm, hpal, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Bitmap object based on an icon.
     */
    constructor(hicon: HICON) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateBitmapFromHICON(hicon, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Bitmap object based on an application or DLL instance handle and the name of a bitmap resource.
     */
    constructor(hInstance: HINSTANCE, bitmapName: String) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateBitmapFromResource(hInstance, bitmapName.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Bitmap object based on an IStream COM interface.
     */
    constructor(stream: IStream, useEmbeddedColorManagement: Boolean = false) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = if (useEmbeddedColorManagement)
                              GdipCreateBitmapFromStreamICM(stream.ptr, result.ptr)
                         else GdipCreateBitmapFromStream(stream.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Bitmap object based on a Graphics object, a width, and a height.
     */
    constructor(width: INT, height: INT, target: Graphics?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateBitmapFromGraphics(width, height, target?.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Bitmap object of a specified size and pixel format. The pixel data must be provided after
     * the Bitmap object is constructed.
     */
    constructor(width: INT, height: INT, format: PixelFormat = PixelFormat32bppARGB) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateBitmapFromScan0(width, height, 0, format, null, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Bitmap object based on an array of bytes along with size and format information.
     */
    constructor(width: INT, height: INT, stride: INT, format: PixelFormat, scan0: CValuesRef<BYTEVar>?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateBitmapFromScan0(width, height, stride, format, scan0, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a new Bitmap object based on this brush.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneImage(ptr, result.ptr))
        if (status == Ok) Bitmap(result.value!!, status) else null
    }

    /**
     * Creates a new Bitmap object by copying a portion of this bitmap.
     */
    fun Clone(rect: RectF, format: PixelFormat)
        = Clone(rect.X, rect.Y, rect.Width, rect.Height, format)

    /**
     * Creates a new Bitmap object by copying a portion of this bitmap.
     */
    fun Clone(rect: Rect, format: PixelFormat)
        = Clone(rect.X, rect.Y, rect.Width, rect.Height, format)

    /**
     * Creates a new Bitmap object by copying a portion of this bitmap.
     */
    fun Clone(x: REAL, y: REAL, width: REAL, height: REAL, format: PixelFormat) = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneBitmapArea(x, y, width, height, format, ptr, result.ptr))
        if (status == Ok) Bitmap(result.value!!, status) else null
    }

    /**
     * Creates a new Bitmap object by copying a portion of this bitmap.
     */
    fun Clone(x: INT, y: INT, width: INT, height: INT, format: PixelFormat) = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneBitmapAreaI(x, y, width, height, format, ptr, result.ptr))
        if (status == Ok) Bitmap(result.value!!, status) else null
    }

    /**
     * The ApplyEffect method alters this Bitmap object by applying a specified effect.
     */
//TODO  fun ApplyEffect(effect: Effect, ROI: RECT)
//          = NotImplemented

    /**
     * The ConvertFormat method converts a bitmap to a specified pixel format. The original pixel data in the
     * bitmap is replaced by the new pixel data.
     */
    fun ConvertFormat(format: PixelFormat, ditherType: DitherType, paletteType: PaletteType, palette: ColorPalette,
                      alphaThresholdPercent: REAL)
        = updateStatus(GdipBitmapConvertFormat(ptr, format, ditherType, paletteType, palette.ptr, alphaThresholdPercent))

    /**
     * Creates a GDI bitmap from this Bitmap object.
     */
    fun GetHBITMAP(colorBackground: Color, hbmReturn: HBITMAPVar)
        = updateStatus(GdipCreateHBITMAPFromBitmap(ptr, hbmReturn.ptr, colorBackground.Value))

    /**
     * Creates an icon from this Bitmap object.
     */
    fun GetHICON(icon: HICONVar)
        = updateStatus(GdipCreateHICONFromBitmap(ptr, icon.ptr))

    /**
     * Returns one or more histograms for specified color channels of this Bitmap object.
     */
    fun GetHistogram(format: HistogramFormat, numberOfEntries: UINT, channel0: CValuesRef<UINTVar>,
                     channel1: CValuesRef<UINTVar>, channel2: CValuesRef<UINTVar>, channel3: CValuesRef<UINTVar>)
        = updateStatus(GdipBitmapGetHistogram(ptr, format, numberOfEntries, channel0, channel1, channel2, channel3))

    /**
     * Returns the number of elements (in an array of UINTs) that you must allocate before you call
     * the GetHistogram method of a Bitmap object.
     */
    fun GetHistogramSize(format: HistogramFormat, numberOfEntries: UINTVar)
        = updateStatus(GdipBitmapGetHistogramSize(format, numberOfEntries.ptr))

    /**
     * Gets the color of a specified pixel in this bitmap.
     */
    fun GetPixel(x: INT, y: INT, color: Color)
          = updateStatus(GdipBitmapGetPixel(ptr, x, y, color.memberAt<ARGBVar>(0).ptr))

    /**
     * Locks a rectangular portion of this bitmap and provides a temporary buffer that you can use to read
     * or write pixel data in a specified format. Any pixel data that you write to the buffer is copied to
     * the Bitmap object when you call UnlockBits.
     */
    fun LockBits(rect: Rect, flags: UINT, format: PixelFormat, lockedBitmapData: BitmapData)
        = updateStatus(GdipBitmapLockBits(ptr, rect.ptr, flags, format, lockedBitmapData.ptr))

    /**
     * Sets the color of a specified pixel in this bitmap.
     */
    fun SetPixel(x: INT, y: INT, color: Color)
        = updateStatus(GdipBitmapSetPixel(ptr, x, y, color.Value))

    /**
     * Sets the resolution of this Bitmap object.
     */
    fun SetResolution(xdpi: REAL, ydpi: REAL)
        = updateStatus(GdipBitmapSetResolution(ptr, xdpi, ydpi))

    /**
     * Unlocks a portion of this bitmap that was previously locked by a call to LockBits.
     */
    fun UnlockBits(lockedBitmapData: BitmapData)
        = updateStatus(GdipBitmapUnlockBits(ptr, lockedBitmapData.ptr))
}

/**
 * A CachedBitmap object stores a bitmap in a format that is optimized for display on a particular device.
 * To display a cached bitmap, call the Graphics.DrawCachedBitmap method.
 */
class CachedBitmap : GdipObject {

    /**
     * Creates a CachedBitmap object based on a Bitmap object and a Graphics object. The cached bitmap takes
     * the pixel data from the Bitmap object and stores it in a format that is optimized for the display device
     * associated with the Graphics object.
     */
    constructor(bitmap: Bitmap?, graphics: Graphics?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateCachedBitmap(bitmap?.ptr, graphics?.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Clone() = TODO()

    /**
     *
     */
    override fun Dispose() {
        GdipDeleteCachedBitmap(ptr)
    }
}

/**
 * The Metafile class defines a graphic metafile. A metafile contains records that describe a sequence
 * of graphics API calls. Metafiles can be recorded (constructed) and played back (displayed).
 */
class Metafile : Image {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    companion object {
        /**
         * Converts an enhanced-format metafile to a WMF metafile and stores the converted records in a specified buffer.
         */
        fun EmfToWmfBits(hEmf: HENHMETAFILE, cbData16: UINT, pData16: LPBYTE, iMapMode: INT = MM_ANISOTROPIC,
                          eFlags: EmfToWmfBitsFlags = EmfToWmfBitsFlagsDefault): UINT
            = GdipEmfToWmfBits(hEmf, cbData16, pData16, iMapMode, eFlags)

        /**
         * Gets the header.
         */
        fun GetMetafileHeader(filename: String, header: MetafileHeader)
            = GdipGetMetafileHeaderFromFile(filename.wcstr, header.ptr)

        /**
         * Gets the header.
         */
        fun GetMetafileHeader(stream: IStream, header: MetafileHeader)
            = GdipGetMetafileHeaderFromStream(stream.ptr, header.ptr)

         ////TODO: Metafile::GetMetafileHeader
         // Gets the header.
         //static Status GetMetafileHeader(HMETAFILE hWmf,
         //      wmfPlaceableFileHeader: WmfPlaceableFileHeader,
         //      header: MetafileHeader)
         //{
         //  // WTF: No flat API to do this.
         //  return NotImplemented
         //}
        /**
         * Gets the header.
         */
         fun GetMetafileHeader(hEmf: HENHMETAFILE, header: MetafileHeader)
            = GdipGetMetafileHeaderFromEmf(hEmf, header.ptr)
    }

    /**
     * Creates a Metafile object for recording. The format will be placeable metafile.
     */
    constructor(hWmf: HMETAFILE, wmfPlaceableFileHeader: WmfPlaceableFileHeader, deleteWmf: Boolean = false) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMetafileFromWmf(hWmf, if (deleteWmf) TRUE else FALSE, wmfPlaceableFileHeader.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for playback based on a GDI Enhanced Metafile (EMF) file.
     */
    constructor(hEmf: HENHMETAFILE, deleteEmf: Boolean = false) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMetafileFromEmf(hEmf, if (deleteEmf) TRUE else FALSE, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for playback.
     */
    constructor(filename: String) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMetafileFromFile(filename.wcstr, result.ptr)
            ptr = result.value
        }
    }

    constructor(filename: String, wmfPlaceableFileHeader: WmfPlaceableFileHeader) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMetafileFromWmfFile(filename.wcstr, wmfPlaceableFileHeader.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object from an IStream interface for playback.
     */
    constructor(stream: IStream) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateMetafileFromStream(stream.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording.
     */
    constructor(referenceHdc: HDC, type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafile(referenceHdc, type, null, MetafileFrameUnitGdi, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording.
     */
    constructor(referenceHdc: HDC, frameRect: RectF, frameUnit: MetafileFrameUnit = MetafileFrameUnitGdi,
                type: EmfType = EmfTypeEmfPlusDual,  description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafile(referenceHdc, type, frameRect.ptr, frameUnit, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording.
     */
    constructor(referenceHdc: HDC, frameRect: Rect, frameUnit: MetafileFrameUnit = MetafileFrameUnitGdi,
            type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafileI(referenceHdc, type, frameRect.ptr, frameUnit, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording.
     */
    constructor(filename: String, referenceHdc: HDC, type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafileFileName(filename.wcstr, referenceHdc, type, null, MetafileFrameUnitGdi, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording.
     */
    constructor(filename: String, referenceHdc: HDC, frameRect: RectF, frameUnit: MetafileFrameUnit = MetafileFrameUnitGdi,
                type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafileFileName(filename.wcstr, referenceHdc, type, frameRect.ptr, frameUnit, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording.
     */
    constructor(filename: String, referenceHdc: HDC, frameRect: Rect, frameUnit: MetafileFrameUnit = MetafileFrameUnitGdi,
                type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafileFileNameI(filename.wcstr, referenceHdc, type, frameRect.ptr, frameUnit, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording to an IStream interface.
     */
    constructor(stream: IStream, referenceHdc: HDC, type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafileStream(stream.ptr, referenceHdc, type, null, MetafileFrameUnitGdi, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording to an IStream interface.
     */
    constructor(stream: IStream, referenceHdc: HDC, frameRect: RectF, frameUnit: MetafileFrameUnit = MetafileFrameUnitGdi,
                type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafileStream(stream.ptr, referenceHdc, type, frameRect.ptr, frameUnit, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Metafile object for recording to an IStream interface.
     */
    constructor(stream: IStream, referenceHdc: HDC, frameRect: Rect, frameUnit: MetafileFrameUnit = MetafileFrameUnitGdi,
                type: EmfType = EmfTypeEmfPlusDual, description: String? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipRecordMetafileStreamI(stream.ptr, referenceHdc, type, frameRect.ptr, frameUnit, description?.wcstr, result.ptr)
            ptr = result.value
        }
    }

    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneImage(ptr, result.ptr))
        if (status == Ok) Metafile(result.value!!, status) else null
    }

    ////TODO: [GDI+ 1.1] Metafile::ConvertToEmfPlus
    //Converts this Metafile object to the EMF+ format.
    //Status ConvertToEmfPlus(refGraphics: Graphics?,
    //      conversionSuccess: BOOL = null,
    //      EmfType emfType = EmfTypeEmfPlusOnly,
    //      description: String = null)
    //{
    //  // FIXME: can't test GdipConvertToEmfPlus because it isn't exported in 1.0
    //  = updateStatus(GdipConvertToEmfPlus(refGraphics?.ptr, ptr, conversionSuccess, emfType, description, ???))
    //}
    ////TODO: [GDI+ 1.1] Metafile::ConvertToEmfPlus
    //Converts this Metafile object to the EMF+ format.
    //Status ConvertToEmfPlus(refGraphics: Graphics,
    //      filename: String,
    //      conversionSuccess: BOOL = null,
    //      EmfType emfType = EmfTypeEmfPlusOnly,
    //      description: String = null)
    //{
    //  // FIXME: can't test GdipConvertToEmfPlusToFile because it isn't exported in 1.0
    //  = updateStatus(GdipConvertToEmfPlusToFile(
    //          refGraphics?.ptr,
    //          (GpMetafile*) ptr, conversionSuccess,
    //          filename, emfType, description, ???))
    //}
    ////TODO: [GDI+ 1.1] Metafile::ConvertToEmfPlus
    //Converts this Metafile object to the EMF+ format.
    //Status ConvertToEmfPlus(refGraphics: Graphics,
    //      stream: IStream,
    //      conversionSuccess: BOOL = null,
    //      EmfType emfType = EmfTypeEmfPlusOnly,
    //      description: String = null)
    //{
    //  // FIXME: can't test GdipConvertToEmfPlusToStream because it isn't exported in 1.0
    //  = updateStatus(GdipConvertToEmfPlusToStream(
    //          refGraphics?.ptr,
    //          (GpMetafile*) ptr, conversionSuccess,
    //          stream, emfType, description, ???))
    //}

    /**
     * Gets the rasterization limit currently set for this metafile. The rasterization limit is the
     * resolution used for certain brush bitmaps that are stored in the metafile.
     * For a detailed explanation of the rasterization limit, see SetDownLevelRasterizationLimit.
     */
    fun GetDownLevelRasterizationLimit() = memScoped {
        val result = alloc<UINTVar>().apply { value = 0 }
        updateStatus(GdipGetMetafileDownLevelRasterizationLimit(ptr, result.ptr))
        result.value
    }

    /**
     * Gets a Windows handle to an Enhanced Metafile (EMF) file.
     */
    fun GetHENHMETAFILE(): HENHMETAFILE? = memScoped {
        val result = alloc<HENHMETAFILEVar>().apply { value = null }
        updateStatus(GdipGetHemfFromMetafile(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the metafile header of this metafile.
     */
    fun GetMetafileHeader(header: MetafileHeader)
        = updateStatus(GdipGetMetafileHeaderFromMetafile(ptr, header.ptr))

    /**
     * Plays a metafile record.
     */
    fun PlayRecord(recordType: EmfPlusRecordType, flags: UINT, dataSize: UINT, data: BYTEVar)
        = updateStatus(GdipPlayMetafileRecord(ptr, recordType, flags, dataSize, data.ptr))

    /**
     * Sets the resolution for certain brush bitmaps that are stored in this metafile.
     */
    fun SetDownLevelRasterizationLimit(limitDpi: UINT)
        = updateStatus(GdipSetMetafileDownLevelRasterizationLimit(ptr, limitDpi))
}

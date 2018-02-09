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
 * The Font class encapsulates the characteristics, such as family, height, size, and style (or combination of styles), of a specific font. A Font object is used when drawing strings.
 */
class Font : GdipObject {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Creates a Font object based on a FontFamily object, a size, a font style, and a unit of measurement.
     */
    constructor(family: FontFamily?, emSize: REAL, style: INT = FontStyleRegular, unit: GpUnit = UnitPoint) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateFont(family?.ptr, emSize, style, unit, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Font object indirectly from a GDI logical font by using a handle to a GDI LOGFONT structure.
     */
    constructor(hdc: HDC, hfont: HFONT?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            val logfont = alloc<LOGFONTA>()
            lastStatus = if (hfont != null && GetObjectA(hfont, sizeOf<LOGFONTA>().narrow(), logfont.ptr) != 0)
                              GdipCreateFontFromLogfontA(hdc, logfont.ptr, result.ptr)
                         else GdipCreateFontFromDC(hdc, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Font object directly from a GDI logical font. The GDI logical font is a LOGFONTA structure,
     * which is the one-byte character version of a logical font. This constructor is provided for
     * compatibility with GDI.
     */
    constructor(hdc: HDC, logfont: LOGFONTA) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateFontFromLogfontA(hdc, logfont.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Font object directly from a GDI logical font. The GDI logical font is a LOGFONTW structure,
     * which is the wide character version of a logical font. This constructor is provided for
     * compatibility with GDI.
     */
    constructor(hdc: HDC, logfont: LOGFONTW) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateFontFromLogfontW(hdc, logfont.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Font object based on the GDI font object that is currently selected into a specified
     * device context. This constructor is provided for compatibility with GDI.
     */
    constructor(hdc: HDC) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateFontFromDC(hdc, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a Font object based on a font family, a size, a font style, a unit of measurement,
     * and a FontCollection object.
     */
    constructor(familyName: String, emSize: REAL, style: INT, unit: GpUnit, fontCollection: FontCollection?) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            val ptrFamily = alloc<COpaquePointerVar>().apply { value = null }
            lastStatus = GdipCreateFontFamilyFromName(familyName.wcstr, fontCollection?.ptr, ptrFamily.ptr)
            if (ptrFamily.value != null) {
                lastStatus = GdipCreateFont(ptrFamily.value, emSize, style, unit, result.ptr)
                GdipDeleteFontFamily(ptrFamily.value)
            }
            ptr = result.value
        }
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDeleteFont(ptr)
    }

    /**
     * Creates a new Font object based on this Font object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneFont(ptr, result.ptr))
        if (status == Ok) Font(result.value!!, status) else null
    }

    /**
     * Gets the font family on which this font is based.
     */
    fun GetFamily(family: FontFamily)= memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipGetFamily(ptr, result.ptr))
        if (status == Ok) family.ptr = result.ptr
    }

    /**
     * Gets the line spacing of this font in the current unit of a specified Graphics object.
     * The line spacing is the vertical distance between the base lines of two consecutive lines of text.
     * Thus, the line spacing includes the blank space between lines along with the height of the
     * character itself.
     */
    fun GetHeight(graphics: Graphics?) = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetFontHeight(ptr, graphics?.ptr, result.ptr))
        result.value
    }

    /**
     * Gets the line spacing, in pixels, of this font. The line spacing is the vertical distance between
     * the base lines of two consecutive lines of text. Thus, the line spacing includes the blank space
     * between lines along with the height of the character itself.
     */
    fun GetHeight(dpi: REAL) = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetFontHeightGivenDPI(ptr, dpi, result.ptr))
        result.value
    }

    /**
     * Uses a LOGFONTA structure to get the attributes of this Font object.
     */
    fun GetLogFontA(graphics: Graphics?, logfontA: LOGFONTA)
        = updateStatus(GdipGetLogFontA(ptr, graphics?.ptr, logfontA.ptr))

    /**
     * Uses a LOGFONTW structure to get the attributes of this Font object.
     */
    fun GetLogFontW(graphics: Graphics?, logfontW: LOGFONTW)
        = updateStatus(GdipGetLogFontW(ptr, graphics?.ptr, logfontW.ptr))

    /**
     * Returns the font size (commonly called the em size) of this Font object. The size is in the units
     * of this Font object.
     */
    fun GetSize() = memScoped {
        val result = alloc<REALVar>().apply { value = 0.0f }
        updateStatus(GdipGetFontSize(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the style of this font's typeface.
     */
    fun GetStyle() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetFontStyle(ptr, result.ptr))
        result.value
    }

    /**
     * Returns the unit of measure of this Font object.
     */
    fun GetUnit() = memScoped {
        val result = alloc<GpUnitVar>().apply { value = UnitPoint }
        updateStatus(GdipGetFontUnit(ptr, result.ptr))
        result.value
    }
}

/**
 * This FontFamily class encapsulates a set of fonts that make up a font family. A font family is a group
 * of fonts that have the same typeface but different styles.
 */
class FontFamily : GdipObject {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    /**
     * Gets a FontFamily object that specifies a generic monospace typeface.
     */
    val GenericMonospace: FontFamily? by lazy {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            val status = GdipGetGenericFontFamilyMonospace(result.ptr)
            if (status == Ok && result.value != null) FontFamily(result.value!!, Ok) else null
        }
    }

    /**
     * Gets a FontFamily object that specifies a generic sans serif typeface.
     */
    val GenericSansSerif: FontFamily? by lazy {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            val status = GdipGetGenericFontFamilySansSerif(result.ptr)
            if (status == Ok && result.value != null) FontFamily(result.value!!, Ok) else null
        }
    }

    /**
     * Gets a FontFamily object that represents a generic serif typeface.
     */
    val GenericSerif: FontFamily? by lazy {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            val status = GdipGetGenericFontFamilySerif(result.ptr)
            if (status == Ok && result.value != null) FontFamily(result.value!!, Ok) else null
        }
    }

    /**
     * Creates a FontFamily::FontFamily object based on a specified font family.
     */
    constructor(name: String, fontCollection: FontCollection? = null) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateFontFamilyFromName(name.wcstr, fontCollection?.ptr, result.ptr)
            ptr = result.value
        }
    }

    override fun Dispose() {
        GdipDeleteFontFamily(ptr)
    }

    /**
     * Creates a new FontFamily object based on this FontFamily object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = updateStatus(GdipCloneFontFamily(ptr, result.ptr))
        if (status == Ok) FontFamily(result.value!!, status) else null
    }

    /**
     * Gets the cell ascent, in design units, of this font family for the specified style or style combination.
     */
    fun GetCellAscent(style: INT): UINT16 = memScoped {
        val result = alloc<UINT16Var>().apply { value = 0 }
        updateStatus(GdipGetCellAscent(ptr, style, result.ptr))
        result.value
    }

    /**
     * Gets the cell descent, in design units, of this font family for the specified style or style combination.
     */
    fun GetCellDescent(style: INT): UINT16 = memScoped {
        val result = alloc<UINT16Var>().apply { value = 0 }
        updateStatus(GdipGetCellDescent(ptr, style, result.ptr))
        result.value
    }

    /**
     * Gets the size (commonly called em size or em height), in design units, of this font family.
     */
    fun GetEmHeight(style: INT): UINT16 = memScoped {
        val result = alloc<UINT16Var>().apply { value = 0 }
        updateStatus(GdipGetEmHeight(ptr, style, result.ptr))
        result.value
    }

    /**
     * Gets the name of this font family.
     */
    fun GetFamilyName(name: String, language: LANGID = LANG_NEUTRAL.toShort())
        = updateStatus(GdipGetFamilyName(ptr, name.wcstr, language))

    /**
     * Gets the line spacing, in design units, of this font family for the specified style or style combination.
     * The line spacing is the vertical distance between the base lines of two consecutive lines of text.
     */
    fun GetLineSpacing(style: INT): UINT16 = memScoped {
        val result = alloc<UINT16Var>().apply { value = 0 }
        updateStatus(GdipGetLineSpacing(ptr, style, result.ptr))
        result.value
    }

    /**
     * Determines whether the specified style is available for this font family.
     */
    fun IsStyleAvailable(style: INT) = memScoped {
        val result = alloc<BOOLVar>().apply { value = FALSE }
        updateStatus(GdipIsStyleAvailable(ptr, style, result.ptr))
        result.value
    }
}

/**
 * The FontCollection class is an abstract base class. It contains methods for enumerating the font families
 * in a collection of fonts. Objects built from this class include the InstalledFontCollection and
 * PrivateFontCollection classes.
 */
open class FontCollection : GdipObject() {

    override fun Clone() = TODO()

    override fun Dispose() {
    }

    /**
     * Gets the font families contained in this font collection.
     */
    fun GetFamilies(numSought: INT, families: List<FontFamily>?, numFound: INTVar): GpStatus = memScoped {
        if (numSought <= 0 || families == null)
            return updateStatus(InvalidParameter)
        for (i in 0 until numSought) {
            families[i].lastStatus = FontFamilyNotFound
            families[i].ptr = null
        }
        numFound.value = 0
        val array: CPointer<COpaquePointerVar>?
             = GdipAlloc((numSought * sizeOf<COpaquePointerVar>()).signExtend())?.reinterpret()
        if (array == null)
            return updateStatus(OutOfMemory)
        val status = updateStatus(GdipGetFontCollectionFamilyList(ptr, numSought,
                                            array.reinterpret(), numFound.ptr))
        // FIXME: must the ptr GpFontFamily objects be cloned? Seems so.
        // (if this is not done, the "Creating a Private Font Collection"
        // example crashes on "delete[] pFontFamily")
        if (status == Ok) {
            val result = alloc<COpaquePointerVar>()
            for (i in 0 until numFound.value) {
                families[i].lastStatus = updateStatus(GdipCloneFontFamily(array[i], result.ptr))
                families[i].ptr = result.ptr
            }
        }
        GdipFree(array)
        return status
    }

    /**
     * Gets the number of font families contained in this font collection.
     */
    fun GetFamilyCount(): INT = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetFontCollectionFamilyCount(ptr, result.ptr))
        result.value
    }
}

/**
 * The PrivateFontCollection is a collection class for fonts. This class keeps a collection of fonts
 * specifically for an application. The fonts in the collection can include installed fonts as well
 * as fonts that have not been installed on the system.
 */
class PrivateFontCollection : FontCollection {

    /**
     * Creates an empty PrivateFontCollection object.
     */
    constructor() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipNewPrivateFontCollection(result.ptr)
            ptr = result.value
        }
    }

    override fun Dispose() {
        //TODO GdipDeletePrivateFontCollection(ptr)
    }

    /**
     * Adds a font file to this private font collection.
     */
    fun AddFontFile(filename: String)
        = updateStatus(GdipPrivateAddFontFile(ptr, filename.wcstr))

    /**
     * Adds a font that is contained in system memory to a GDI+ font collection.
     */
    fun AddMemoryFont(memory: COpaquePointer, length: INT)
        = updateStatus(GdipPrivateAddMemoryFont(ptr, memory, length))
}

/**
 * The InstalledFontCollection class defines a class that represents the fonts installed on the system.
 */
class InstalledFontCollection : FontCollection {

    /**
     * Creates an InstalledFontCollection object.
     */
    constructor() {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipNewInstalledFontCollection(result.ptr)
            ptr = result.value
        }
    }
}

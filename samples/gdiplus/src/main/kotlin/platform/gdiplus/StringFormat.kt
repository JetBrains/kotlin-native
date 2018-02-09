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
 * The StringFormat class encapsulates text layout information (such as alignment, orientation,
 * tab stops, and clipping) and display manipulations (such as trimming, font substitution for characters
 * that are not supported by the requested font, and digit substitution for languages that do not use
 * Western European digits). A StringFormat object can be passed to the DrawString Methods method to
 * format a string.
 */
class StringFormat : GdipObject {
    private constructor(ptr: COpaquePointer, status: GpStatus) : super(ptr, status) {}

    companion object {

        /**
         * Creates a generic, default StringFormat object.
         */
        val GenericDefault: StringFormat? by lazy {
            memScoped {
                val result = alloc<COpaquePointerVar>()
                val status = GdipStringFormatGetGenericDefault(result.ptr)
                if (status == Ok && result.value != null) StringFormat(result.value!!, Ok) else null
            }
        }

        /**
         * Creates a generic, typographic StringFormat object.
         */
        val GenericTypographic: StringFormat? by lazy {
            memScoped {
                val result = alloc<COpaquePointerVar>()
                val status = GdipStringFormatGetGenericTypographic(result.ptr)
                if (status == Ok && result.value != null) StringFormat(result.value!!, Ok) else null
            }
        }
    }

    /**
     * Creates a StringFormat object based on string format flags and a language.
     */
    constructor(formatFlags: INT = 0, language: LANGID = LANG_NEUTRAL.narrow()) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCreateStringFormat(formatFlags, language, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a StringFormat object from another StringFormat object.
     */
    constructor(format: StringFormat) {
        memScoped {
            val result = alloc<COpaquePointerVar>()
            lastStatus = GdipCloneStringFormat(format.ptr, result.ptr)
            ptr = result.value
        }
    }

    /**
     * Creates a new StringFormat object and initializes it with the contents of this StringFormat object.
     */
    override fun Clone() = memScoped {
        val result = alloc<COpaquePointerVar>()
        val status = GdipCloneStringFormat(ptr, result.ptr)
        if (status == Ok) StringFormat(result.value!!, status) else null
    }

    /**
     *
     */
    override fun Dispose() {
        GdipDeleteStringFormat(ptr)
    }

    /**
     * Gets an element of the StringAlignment enumeration that indicates the character alignment of
     * this StringFormat object in relation to the origin of the layout rectangle. A layout rectangle
     * is used to position the displayed string.
     */
    fun GetAlignment() = memScoped {
        val result = alloc<StringAlignmentVar>().apply { value = StringAlignmentNear }
        updateStatus(GdipGetStringFormatAlign(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the language that corresponds with the digits that are to be substituted for Western European digits.
     */
    fun GetDigitSubstitutionLanguage(): LANGID = memScoped {
        val result = alloc<LANGIDVar>().apply { value = 0 }
        val method = alloc<StringDigitSubstituteVar>()
        updateStatus(GdipGetStringFormatDigitSubstitution(ptr, result.ptr, method.ptr))
        result.value
    }

    /**
     * Gets an element of the StringDigitSubstitute enumeration that indicates the digit substitution method that is used by this StringFormat object.
     */
    fun GetDigitSubstitutionMethod() = memScoped {
        val language = alloc<LANGIDVar>()
        val result = alloc<StringDigitSubstituteVar>().apply { value = StringDigitSubstituteUser }
        updateStatus(GdipGetStringFormatDigitSubstitution(ptr, language.ptr, result.ptr))
        result.value
    }

    /**
     * Gets the string format flags for this StringFormat object.
     */
    fun GetFormatFlags() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetStringFormatFlags(ptr, result.ptr))
        result.value
    }

    /**
     * Gets an element of the HotkeyPrefix enumeration that indicates the type of processing that 
     * is performed on a string when a hot key prefix, an ampersand (&), is encountered.
     */
    fun GetHotkeyPrefix() = memScoped {
        val result = alloc<HotkeyPrefixVar>().apply { value = HotkeyPrefixNone }
        updateStatus(GdipGetStringFormatHotkeyPrefix(ptr, result.ptr))
        result.value
    }

    /**
     * Gets an element of the StringAlignment enumeration that indicates the line alignment of this 
     * StringFormat object in relation to the origin of the layout rectangle. The line alignment setting 
     * specifies how to align the string vertically in the layout rectangle. The layout rectangle is used
     * to position the displayed string.
     */
    fun GetLineAlignment() = memScoped {
        val result = alloc<StringAlignmentVar>().apply { value = StringAlignmentNear }
        updateStatus(GdipGetStringFormatLineAlign(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the number of measurable character ranges that are currently set. The character ranges that 
     * are set can be measured in a string by using the Graphics::MeasureCharacterRanges method.
     */
    fun GetMeasurableCharacterRangeCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetStringFormatMeasurableCharacterRangeCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the number of tab-stop offsets in this StringFormat object.
     */
    fun GetTabStopCount() = memScoped {
        val result = alloc<INTVar>().apply { value = 0 }
        updateStatus(GdipGetStringFormatTabStopCount(ptr, result.ptr))
        result.value
    }

    /**
     * Gets the offsets of the tab stops in this StringFormat object.
     */
    fun GetTabStops(count: INT, firstTabOffset: CValuesRef<REALVar>, tabStops: CValuesRef<REALVar>)
        = updateStatus(GdipGetStringFormatTabStops(ptr, count, firstTabOffset, tabStops))

    /**
     * Gets an element of the StringTrimming enumeration that indicates the trimming style of this 
     * StringFormat object. The trimming style determines how to trim characters from a string that is 
     * too large to fit in the layout rectangle.
     */
    fun GetTrimming() = memScoped {
        val result = alloc<StringTrimmingVar>().apply { value = StringTrimmingNone }
        updateStatus(GdipGetStringFormatTrimming(ptr, result.ptr))
        result.value
    }

    /**
     * Sets the character alignment of this StringFormat object in relation to the origin of the layout
     * rectangle. A layout rectangle is used to position the displayed string.
     */
    fun SetAlignment(align: StringAlignment)
        = updateStatus(GdipSetStringFormatAlign(ptr, align))

    /**
     * Sets the digit substitution method and the language that corresponds to the digit substitutes.
     */
    fun SetDigitSubstitution(language: LANGID, substitute: StringDigitSubstitute)
        = updateStatus(GdipSetStringFormatDigitSubstitution(ptr, language, substitute))

    /**
     * Sets the format flags for this StringFormat object. The format flags determine most of the
     * characteristics of a StringFormat object.
     */
    fun SetFormatFlags(flags: INT)
        = updateStatus(GdipSetStringFormatFlags(ptr, flags))

    /**
     * Sets the type of processing that is performed on a string when the hot key prefix, an ampersand (&),
     * is encountered. The ampersand is called the hot key prefix and can be used to designate certain keys
     * as hot keys.
     */
    fun SetHotkeyPrefix(hotkeyPrefix: HotkeyPrefix)
        = updateStatus(GdipSetStringFormatHotkeyPrefix(ptr, hotkeyPrefix))

    /**
     * Sets the line alignment of this StringFormat object in relation to the origin of the layout rectangle.
     * The line alignment setting specifies how to align the string vertically in the layout rectangle.
     * The layout rectangle is used to position the displayed string.
     */
    fun SetLineAlignment(align: StringAlignment)
        = updateStatus(GdipSetStringFormatLineAlign(ptr, align))

    /**
     * Sets a series of character ranges for this StringFormat object that, when in a string, can be measured
     * by the Graphics::MeasureCharacterRanges method.
     */
    fun SetMeasurableCharacterRanges(rangeCount: INT, ranges: CharacterRange)
        = updateStatus(GdipSetStringFormatMeasurableCharacterRanges(ptr, rangeCount, ranges.ptr))

    /**
     * Sets the offsets for tab stops in this StringFormat object.
     */
    fun SetTabStops(firstTabOffset: REAL, count: INT, tabStops: REALVar)
        = updateStatus(GdipSetStringFormatTabStops(ptr, firstTabOffset, count, tabStops.ptr))

    /**
     * Sets the trimming style for this StringFormat object. The trimming style determines how to trim
     * a string so that it fits into the layout rectangle.
     */
    fun SetTrimming(trimming: StringTrimming)
        = updateStatus(GdipSetStringFormatTrimming(ptr, trimming))
}

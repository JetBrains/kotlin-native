/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <wctype.h>

#include <iterator>
#include <string>

#include "Assert.h"
#include "City.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Types.h"

#include "utf8.h"

namespace {

// Container with STL-like iterator over raw data.
template <typename T>
class Raw {
 public:
  Raw(T* start, uint32_t count) : start_(start), end_(start + count) {}

  T* begin() { return start_; }
  T* end() { return end_; }

 private:
  T* start_;
  T* end_;
};


OBJ_GETTER(utf8ToUtf16, Raw<const char> rawString) {
  uint32_t charCount = utf8::distance(rawString.begin(), rawString.end());
  ArrayHeader* result = AllocArrayInstance(
    theStringTypeInfo, charCount, OBJ_RESULT)->array();
  Raw<KChar> rawResult(CharArrayAddressOfElementAt(result, 0), charCount);
  auto convertResult =
      utf8::utf8to16(rawString.begin(), rawString.end(), rawResult.begin());
  RuntimeAssert(rawResult.end() == convertResult, "Must properly fit");
  RETURN_OBJ(result->obj());
}

template <typename T> T parseInt(KString value, KInt radix) {
  Raw<const KChar> utf16(
    CharArrayAddressOfElementAt(value, 0), value->count_);
  std::string utf8;
  utf8::utf16to8(utf16.begin(), utf16.end(), back_inserter(utf8));
  char* end = nullptr;
  long result = strtol(utf8.c_str(), &end, radix);
  if (end != utf8.c_str() + utf8.size()) {
    ThrowNumberFormatException();
  }
  return result;
}

KLong parseLong(KString value, KInt radix) {
  Raw<const KChar> utf16(
    CharArrayAddressOfElementAt(value, 0), value->count_);
  std::string utf8;
  utf8::utf16to8(utf16.begin(), utf16.end(), back_inserter(utf8));
  char* end = nullptr;
  KLong result = strtoll(utf8.c_str(), &end, radix);
  if (end != utf8.c_str() + utf8.size()) {
    ThrowNumberFormatException();
  }
  return result;
}

KFloat parseFloat(KString value) {
  Raw<const KChar> utf16(
    CharArrayAddressOfElementAt(value, 0), value->count_);
  std::string utf8;
  utf8::utf16to8(utf16.begin(), utf16.end(), back_inserter(utf8));
  char* end = nullptr;
  KFloat result = strtof(utf8.c_str(), &end);
  if (end != utf8.c_str() + utf8.size()) {
    ThrowNumberFormatException();
  }
  return result;
}

KDouble parseDouble(KString value) {
  Raw<const KChar> utf16(
    CharArrayAddressOfElementAt(value, 0), value->count_);
  std::string utf8;
  utf8::utf16to8(utf16.begin(), utf16.end(), back_inserter(utf8));
  char* end = nullptr;
  KDouble result = strtod(utf8.c_str(), &end);
  if (end != utf8.c_str() + utf8.size()) {
    ThrowNumberFormatException();
  }
  return result;
}

} // namespace

extern "C" {

OBJ_GETTER(CreateStringFromCString, const char* cstring) {
  RETURN_RESULT_OF(utf8ToUtf16, Raw<const char>(cstring, strlen(cstring)));
}

OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes) {
  RETURN_RESULT_OF(utf8ToUtf16, Raw<const char>(utf8, lengthBytes));
}

// String.kt
KInt Kotlin_String_compareTo(KString thiz, KString other) {
  return memcmp(
    CharArrayAddressOfElementAt(thiz, 0),
    CharArrayAddressOfElementAt(other, 0),
    (thiz->count_ < other->count_ ? thiz->count_ : other->count_) * sizeof(KChar));
}

KChar Kotlin_String_get(KString thiz, KInt index) {
  if (static_cast<uint32_t>(index) >= thiz->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *CharArrayAddressOfElementAt(thiz, index);
}

KInt Kotlin_String_getStringLength(KString thiz) {
  return thiz->count_;
}

OBJ_GETTER(Kotlin_String_fromUtf8Array, KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theByteArrayTypeInfo, "Must use a byte array");
  if (start < 0 || size < 0 || size > array->count_ - start) {
    ThrowArrayIndexOutOfBoundsException();
  }
  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  Raw<const char> rawString(
    reinterpret_cast<const char*>(
      ByteArrayAddressOfElementAt(array, start)), size);
  RETURN_RESULT_OF(utf8ToUtf16, rawString);
}

OBJ_GETTER(Kotlin_String_fromCharArray, KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theCharArrayTypeInfo, "Must use a char array");
  if (start < 0 || size < 0 || size > array->count_ - start) {
    ThrowArrayIndexOutOfBoundsException();
  }

  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }

  ArrayHeader* result = AllocArrayInstance(
    theStringTypeInfo, size, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(array, start),
         size * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toCharArray, KString string) {
  ArrayHeader* result = AllocArrayInstance(
    theCharArrayTypeInfo, string->count_, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(string, 0),
         string->count_ * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_plusImpl, KString thiz, KString other) {
  RuntimeAssert(thiz != nullptr, "this cannot be null");
  RuntimeAssert(other != nullptr, "other cannot be null");
  RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must be a string");
  RuntimeAssert(other->type_info() == theStringTypeInfo, "Must be a string");
  KInt result_length = thiz->count_ + other->count_;
  if (result_length < thiz->count_ || result_length < other->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  ArrayHeader* result = AllocArrayInstance(
    theStringTypeInfo, result_length, OBJ_RESULT)->array();
  memcpy(
      CharArrayAddressOfElementAt(result, 0),
      CharArrayAddressOfElementAt(thiz, 0),
      thiz->count_ * sizeof(KChar));
  memcpy(
      CharArrayAddressOfElementAt(result, thiz->count_),
      CharArrayAddressOfElementAt(other, 0),
      other->count_ * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

KBoolean Kotlin_String_equals(KString thiz, KConstRef other) {
  if (other == nullptr || other->type_info() != theStringTypeInfo) return false;
  // Important, due to literal internalization.
  KString otherString = other->array();
  if (thiz == otherString) return true;
  return thiz->count_ == otherString->count_ &&
      memcmp(CharArrayAddressOfElementAt(thiz, 0),
             CharArrayAddressOfElementAt(otherString, 0),
             thiz->count_ * sizeof(KChar)) == 0;
}

KBoolean Kotlin_String_equalsIgnoreCase(KString thiz, KConstRef other) {
  RuntimeAssert(thiz->type_info() == theStringTypeInfo &&
                other->type_info() == theStringTypeInfo, "Must be strings");
  // Important, due to literal internalization.
  KString otherString = other->array();
  if (thiz == otherString) return true;
  if (thiz->count_ != otherString->count_) return false;
  auto count = thiz->count_;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  const KChar* otherRaw = CharArrayAddressOfElementAt(otherString, 0);
  for (KInt index = 0; index < count; ++index) {
    if (towlower(*thizRaw++) != towlower(*otherRaw++)) return false;
  }
  return true;
}

OBJ_GETTER(Kotlin_String_replace, KString thiz, KChar oldChar, KChar newChar,
           KBoolean ignoreCase) {
  auto count = thiz->count_;
  ArrayHeader* result = AllocArrayInstance(
      theStringTypeInfo, count, OBJ_RESULT)->array();
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  KChar* resultRaw = CharArrayAddressOfElementAt(result, 0);
  if (ignoreCase) {
    KChar oldCharLower = towlower(oldChar);
    for (KInt index = 0; index < count; ++index) {
      KChar thizChar = *thizRaw++;
      *resultRaw++ = towlower(thizChar) == oldCharLower ? newChar : thizChar;
    }
  } else {
    for (KInt index = 0; index < count; ++index) {
      KChar thizChar = *thizRaw++;
      *resultRaw++ = thizChar == oldChar ? newChar : thizChar;
    }
  }
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toUpperCase, KString thiz) {
  auto count = thiz->count_;
  ArrayHeader* result = AllocArrayInstance(
      theStringTypeInfo, count, OBJ_RESULT)->array();
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  KChar* resultRaw = CharArrayAddressOfElementAt(result, 0);
  for (KInt index = 0; index < count; ++index) {
    *resultRaw++ = towupper(*thizRaw++);
  }
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toLowerCase, KString thiz) {
  auto count = thiz->count_;
  ArrayHeader* result = AllocArrayInstance(
      theStringTypeInfo, count, OBJ_RESULT)->array();
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  KChar* resultRaw = CharArrayAddressOfElementAt(result, 0);
  for (KInt index = 0; index < count; ++index) {
    *resultRaw++ = towlower(*thizRaw++);
  }
  RETURN_OBJ(result->obj());
}

KBoolean Kotlin_String_regionMatches(KString thiz, KInt thizOffset,
                                     KString other, KInt otherOffset,
                                     KInt length, KBoolean ignoreCase) {
  if (length < 0 ||
      thizOffset < 0 || length > thiz->count_ - thizOffset ||
      otherOffset < 0 || length > other->count_ - otherOffset) {
    return false;
  }
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, thizOffset);
  const KChar* otherRaw = CharArrayAddressOfElementAt(other, otherOffset);
  if (ignoreCase) {
    for (KInt index = 0; index < length; ++index) {
      if (towlower(*thizRaw++) != towlower(*otherRaw++)) return false;
    }
  } else {
    for (KInt index = 0; index < length; ++index) {
      if (*thizRaw++ != *otherRaw++) return false;
    }
  }
  return true;
}

KBoolean Kotlin_CharSequence_regionMatches(KString thiz, KInt thizOffset,
                                           KString other, KInt otherOffset,
                                           KInt length, KBoolean ignoreCase) {
  return Kotlin_String_regionMatches(thiz, thizOffset, other, otherOffset,
                                     length, ignoreCase);
}

KBoolean Kotlin_Char_isDefined(KChar ch) {
  // TODO: fixme!
  RuntimeAssert(false, "Kotlin_Char_isDefined() is not implemented");
  return true;
}

KBoolean Kotlin_Char_isLetter(KChar ch) {
  return iswalpha(ch) != 0;
}

KBoolean Kotlin_Char_isLetterOrDigit(KChar ch) {
  return iswalnum(ch) != 0;
}

KBoolean Kotlin_Char_isDigit(KChar ch) {
  return iswdigit(ch) != 0;
}

KBoolean Kotlin_Char_isIdentifierIgnorable(KChar ch) {
  RuntimeAssert(false, "Kotlin_Char_isIdentifierIgnorable() is not implemented");
  return false;
}

KBoolean Kotlin_Char_isISOControl(KChar ch) {
  return (ch <= 0x1F) || (ch >= 0x7F && ch <= 0x9F);
}

KBoolean Kotlin_Char_isHighSurrogate(KChar ch) {
  return ((ch & 0xfc00) == 0xd800);
}

KBoolean Kotlin_Char_isLowSurrogate(KChar ch) {
  return ((ch & 0xfc00) == 0xdc00);
}

KBoolean Kotlin_Char_isWhitespace(KChar ch) {
  return iswspace(ch) != 0;
}

KBoolean Kotlin_Char_isLowerCase(KChar ch) {
  return iswlower(ch) != 0;
}

KBoolean Kotlin_Char_isUpperCase(KChar ch) {
  return iswupper(ch) != 0;
}

KChar Kotlin_Char_toLowerCase(KChar ch) {
  return towlower(ch);
}

KChar Kotlin_Char_toUpperCase(KChar ch) {
  return towupper(ch);
}

KInt Kotlin_Char_digitOf(KChar ch, KInt radix) {
  // TODO: make smarter and support full unicode.
  const static KInt digits[] = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
    -1, -1, -1, -1, -1, -1, -1,
    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
    30, 31, 32, 33, 34, 35,
    -1, -1, -1, -1, -1, -1,
    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
    30, 31, 32, 33, 34, 35 };
  if (ch < 0x30 /* 0 */ || ch > 0x7a /* z */) return -1;
  return digits[ch - 0x30];
}

KInt Kotlin_String_indexOfChar(KString thiz, KChar ch, KInt fromIndex) {
  if (fromIndex < 0 || fromIndex > thiz->count_) {
    return -1;
  }
  KInt count = thiz->count_;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, fromIndex);
  while (fromIndex < count) {
    if (*thizRaw++ == ch) return fromIndex;
    fromIndex++;
  }
  return -1;
}

KInt Kotlin_String_lastIndexOfChar(KString thiz, KChar ch, KInt fromIndex) {
  if (fromIndex < 0 || fromIndex > thiz->count_ || thiz->count_ == 0) {
    return -1;
  }
  KInt count = thiz->count_;
  KInt index = fromIndex;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, index);
  while (index >= 0) {
    if (*thizRaw-- == ch) return index;
    index--;
  }
  return -1;
}

// TODO: or code up Knuth-Moris-Pratt.
KInt Kotlin_String_indexOfString(KString thiz, KString other, KInt fromIndex) {
  if (fromIndex < 0 || fromIndex > thiz->count_ ||
      other->count_ > thiz->count_ - fromIndex) {
    return -1;
  }
  KInt count = thiz->count_;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, fromIndex);
  const KChar* otherRaw = CharArrayAddressOfElementAt(other, 0);
  void* result = memmem(thizRaw, (thiz->count_ - fromIndex) * sizeof(KChar),
                        otherRaw, other->count_ * sizeof(KChar));
  if (result == nullptr) return -1;

  return (reinterpret_cast<intptr_t>(result) - reinterpret_cast<intptr_t>(
      CharArrayAddressOfElementAt(thiz, 0))) / sizeof(KChar);
}

KInt Kotlin_String_lastIndexOfString(KString thiz, KString other, KInt fromIndex) {
  if (fromIndex < 0 || fromIndex > thiz->count_ || thiz->count_ == 0 ||
      other->count_ > thiz->count_ - fromIndex) {
    return -1;
  }
  KInt count = thiz->count_;
  KInt otherCount = other->count_;
  KInt start = fromIndex;
  if (otherCount <= count && start >= 0) {
    if (otherCount > 0) {
      if (fromIndex > count - otherCount)
        start = count - otherCount;
      KChar firstChar = *CharArrayAddressOfElementAt(other, 0);
      while (true) {
        KInt candidate = Kotlin_String_lastIndexOfChar(thiz, firstChar, start);
        if (candidate == -1) return -1;
        KInt offsetThiz = candidate;
        KInt offsetOther = 0;
        while (++offsetOther < otherCount &&
               *CharArrayAddressOfElementAt(thiz, ++offsetThiz) ==
               *CharArrayAddressOfElementAt(other, offsetOther)) {}
        if (offsetOther == otherCount) {
          return candidate;
        }
        start = candidate - 1;
      }
    }
    return start < count ? start : count;
  }
  return -1;
}

KInt Kotlin_String_hashCode(KString thiz) {
  // TODO: consider caching strings hashes.
  // TODO: maybe use some simpler hashing algorithm?
  // Note that we don't use Java's string hash.
  return CityHash64(
    CharArrayAddressOfElementAt(thiz, 0), thiz->count_ * sizeof(KChar));
}

OBJ_GETTER(Kotlin_String_subSequence, KString thiz, KInt startIndex, KInt endIndex) {
  if (startIndex < 0 || endIndex > thiz->count_ || startIndex > endIndex) {
    // TODO: is it correct exception?
    ThrowArrayIndexOutOfBoundsException();
  }
  if (startIndex == endIndex) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  KInt length = endIndex - startIndex;
  ArrayHeader* result = AllocArrayInstance(
    theStringTypeInfo, length, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(thiz, startIndex),
         length * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

// io/Console.kt
void Kotlin_io_Console_print(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  // TODO: system stdout must be aware about UTF-8.
  Raw<const KChar> utf16(
    CharArrayAddressOfElementAt(message, 0), message->count_);
  std::string utf8;
  utf8::utf16to8(utf16.begin(), utf16.end(), back_inserter(utf8));
  write(STDOUT_FILENO, utf8.c_str(), utf8.size());
}

void Kotlin_io_Console_println(KString message) {
  Kotlin_io_Console_print(message);
  Kotlin_io_Console_println0();
}

void Kotlin_io_Console_println0() {
  write(STDOUT_FILENO, "\n", 1);
}

OBJ_GETTER0(Kotlin_io_Console_readLine) {
  char data[4096];
  if (!fgets(data, sizeof(data) - 1, stdin)) {
    return nullptr;
  }
  RETURN_RESULT_OF(CreateStringFromCString, data);
}

KByte Kotlin_String_parseByte(KString value, KInt radix) {
  return parseInt<KByte>(value, radix);
}

KShort Kotlin_String_parseShort(KString value, KInt radix) {
  return parseInt<KShort>(value, radix);
}

KInt Kotlin_String_parseInt(KString value, KInt radix) {
  return parseInt<KInt>(value, radix);
}

KLong Kotlin_String_parseLong(KString value, KInt radix) {
  return parseLong(value, radix);
}

KFloat Kotlin_String_parseFloat(KString value) {
  return parseFloat(value);
}

KDouble Kotlin_String_parseDouble(KString value) {
  return parseDouble(value);
}

} // extern "C"

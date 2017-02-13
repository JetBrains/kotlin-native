#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <iterator>
#include <string>

#include "Assert.h"
#include "City.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "String.h"
#include "Types.h"

#include "utf8.h"

namespace {

// Container with STL-compliant iterator over raw data.
template <typename T>
class Raw {
  class iterator {
   public:
    typedef iterator self_type;
    typedef T value_type;
    typedef T& reference;
    typedef T* pointer;
    typedef std::forward_iterator_tag iterator_category;
    typedef int difference_type;

    iterator(T* start) : current_(start) {}

    self_type operator++(int) {
      auto result = *this;
      current_++;
      return result;
    }
    self_type operator++() {
      current_++;
      return *this;
    }
    reference operator*() const {
      return *current_;
    }
    pointer operator->() {
      return current_;
    }
    bool operator==(const self_type& other) {
      return current_ == other.current_;
    }
    bool operator!=(const self_type& other) {
      return current_ != other.current_;
    }
    bool operator<(const self_type& other) {
      return current_ < other.current_;
    }
   private:
    T* current_;
  };

 public:
  Raw(T* start, uint32_t count) : start_(start), end_(start + count) {}

  iterator begin() { return iterator(start_); }
  iterator end() { return iterator(end_); }

 private:
  T* start_;
  T* end_;
};

} // namespace

extern "C" {

OBJ_GETTER(CreateStringFromCString, const char* cstring) {
  RETURN_RESULT_OF(CreateStringFromUtf8, cstring, strlen(cstring));
}

OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes) {
  Raw<const char> rawString(utf8, lengthBytes);
  uint32_t charCount = utf8::distance(rawString.begin(), rawString.end());
  ArrayHeader* result = AllocArrayInstance(
    theStringTypeInfo, charCount, OBJ_RESULT)->array();
  Raw<KChar> rawResult(CharArrayAddressOfElementAt(result, 0), charCount);
  utf8::utf8to16(rawString.begin(), rawString.end(), rawResult.begin());
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(CreateString, uint32_t charCount) {
  ArrayHeader* result = AllocArrayInstance(
      theStringTypeInfo, charCount, OBJ_RESULT)->array();
  RETURN_OBJ(result->obj());
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
  if (start < 0 || size < 0 || start + size > array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  Raw<const KByte> rawString(ByteArrayAddressOfElementAt(array, start), size);
  uint32_t charCount = utf8::distance(rawString.begin(), rawString.end());
  ArrayHeader* result = AllocArrayInstance(
    theStringTypeInfo, charCount, OBJ_RESULT)->array();
  Raw<KChar> rawResult(CharArrayAddressOfElementAt(result, 0), charCount);
  utf8::utf8to16(rawString.begin(), rawString.end(), rawResult.begin());
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_fromCharArray, KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theCharArrayTypeInfo, "Must use a byte array");
  if (start < 0 || size < 0 || start + size > array->count_) {
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
  uint32_t result_length = thiz->count_ + other->count_;
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

KInt Kotlin_String_hashCode(KString thiz) {
  // TODO: consider caching strings hashes.
  // TODO: maybe use some simpler hashing algorithm?
  return CityHash64(
    ByteArrayAddressOfElementAt(thiz, 0),
    thiz->count_ * sizeof(KChar));
}

OBJ_GETTER(Kotlin_String_subSequence, KString thiz, KInt startIndex, KInt endIndex) {
  if (startIndex < 0 || endIndex >= thiz->count_ || startIndex > endIndex) {
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


} // extern "C"

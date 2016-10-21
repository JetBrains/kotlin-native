#include <string.h>
#include <unistd.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// Arrays.kt
// TODO: those must be compiler intrinsics afterwards.
KByte Kotlin_ByteArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

void Kotlin_ByteArray_set(ArrayHeader* obj, KInt index, KByte value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ByteArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_ByteArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theByteArrayTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_ByteArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KChar Kotlin_CharArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *CharArrayAddressOfElementAt(obj, index);
}

void Kotlin_CharArray_set(ArrayHeader* obj, KInt index, KChar value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *CharArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_CharArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theCharArrayTypeInfo, length).GetPlace();
  memcpy(
      CharArrayAddressOfElementAt(result, 0),
      CharArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_CharArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KInt Kotlin_IntArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *IntArrayAddressOfElementAt(obj, index);
}

void Kotlin_IntArray_set(ArrayHeader* obj, KInt index, KInt value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *IntArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_IntArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theIntArrayTypeInfo, length).GetPlace();
  memcpy(
      IntArrayAddressOfElementAt(result, 0),
      IntArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_IntArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

// io/Console.kt
void Kotlin_io_Console_print(const ArrayHeader* array) {
  RuntimeAssert(array->type_info_ == theStringTypeInfo, "Must use a string");
  write(1, ByteArrayAddressOfElementAt(array, 0), array->count_);
}

// String.kt
KInt Kotlin_String_compareTo(const ArrayHeader* obj, const ArrayHeader* other) {
  return memcmp(ByteArrayAddressOfElementAt(obj, 0),
                ByteArrayAddressOfElementAt(other, 0),
                obj->count_ < other->count_ ? obj->count_ : other->count_);
}

KChar Kotlin_String_get(const ArrayHeader* obj, KInt index) {
  // TODO: support full UTF-8.
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

KInt Kotlin_String_getStringLength(const ArrayHeader* array) {
  return array->count_;
}

ArrayHeader* Kotlin_String_fromUtf8Array(const ArrayHeader* array) {
  RuntimeAssert(array->type_info_ == theByteArrayTypeInfo, "Must use a byte array");
  uint32_t length = ArraySizeBytes(array);
  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

ArrayHeader* Kotlin_String_plusImpl(
    const ArrayHeader* obj, const ArrayHeader* other) {
  // TODO: support UTF-8
  RuntimeAssert(obj->type_info_ == theStringTypeInfo, "Must be a string");
  RuntimeAssert(other->type_info_ == theStringTypeInfo, "Must be a string");
  uint32_t result_length = obj->count_ + other->count_;
  ArrayHeader* result = ArrayContainer(
      theStringTypeInfo, result_length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(obj, 0),
      obj->count_);
  memcpy(
      ByteArrayAddressOfElementAt(result, obj->count_),
      ByteArrayAddressOfElementAt(other, 0),
      other->count_);
  return result;
}

}

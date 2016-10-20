#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// TODO: those must be compiler intrinsics afterwards.
KByte Kotlin_ByteArray_get(const ArrayHeader* obj, int32_t index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

void Kotlin_ByteArray_set(ArrayHeader* obj, int32_t index, KByte value) {
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

KChar Kotlin_CharArray_get(const ArrayHeader* obj, int32_t index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *CharArrayAddressOfElementAt(obj, index);
}

void Kotlin_CharArray_set(ArrayHeader* obj, int32_t index, KChar value) {
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

KInt Kotlin_IntArray_get(const ArrayHeader* obj, int32_t index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *IntArrayAddressOfElementAt(obj, index);
}

void Kotlin_IntArray_set(ArrayHeader* obj, int32_t index, KInt value) {
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

KChar Kotlin_String_get(const ArrayHeader* obj, int32_t index) {
  // TODO: support full UTF-8.
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

ArrayHeader* Kotlin_String_fromUtf8Array(const ArrayHeader* array) {
  RuntimeAssert(array->type_info_ == theByteArrayTypeInfo, "Must get a byte array");
  uint32_t length = ArraySizeBytes(array);
  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

}

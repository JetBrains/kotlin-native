#include <stdio.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// TODO: those must be compiler intrinsics afterwards.

// Array.kt
KRef Kotlin_Array_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ArrayAddressOfElementAt(obj, index);
}

void Kotlin_Array_set(ArrayHeader* obj, KInt index, KRef value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_Array_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(array->type_info(), length).GetPlace();
  memcpy(
      ArrayAddressOfElementAt(result, 0),
      ArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_Array_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

// Arrays.kt
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
  return *PrimitiveArrayAddressOfElementAt<KChar>(obj, index);
}

void Kotlin_CharArray_set(ArrayHeader* obj, KInt index, KChar value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KChar>(obj, index) = value;
}

ArrayHeader* Kotlin_CharArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theCharArrayTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_CharArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KShort Kotlin_ShortArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KShort>(obj, index);
}

void Kotlin_ShortArray_set(ArrayHeader* obj, KInt index, KShort value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KShort>(obj, index) = value;
}

ArrayHeader* Kotlin_ShortArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theShortArrayTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_ShortArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KInt Kotlin_IntArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KInt>(obj, index);
}

void Kotlin_IntArray_set(ArrayHeader* obj, KInt index, KInt value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KInt>(obj, index) = value;
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

KLong Kotlin_LongArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KLong>(obj, index);
}

void Kotlin_LongArray_set(ArrayHeader* obj, KInt index, KLong value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KLong>(obj, index) = value;
}

ArrayHeader* Kotlin_LongArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theLongArrayTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_LongArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KFloat Kotlin_FloatArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KFloat>(obj, index);
}

void Kotlin_FloatArray_set(ArrayHeader* obj, KInt index, KFloat value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KFloat>(obj, index) = value;
}

KDouble Kotlin_DoubleArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KDouble>(obj, index);
}

void Kotlin_DoubleArray_set(ArrayHeader* obj, KInt index, KDouble value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KDouble>(obj, index) = value;
}

ArrayHeader* Kotlin_DoubleArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theDoubleArrayTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_DoubleArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KBoolean Kotlin_BooleanArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KBoolean>(obj, index);
}

void Kotlin_DoubleArray_set(ArrayHeader* obj, KInt index, KBoolean value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KBoolean>(obj, index) = value;
}

ArrayHeader* Kotlin_BooleanArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theBooleanArrayTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_BooleanArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

}  // extern "C"

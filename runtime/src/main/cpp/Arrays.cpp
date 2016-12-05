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
KRef Kotlin_Array_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ArrayAddressOfElementAt(array, index);
}

void Kotlin_Array_set(KRef thiz, KInt index, KConstRef value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ArrayAddressOfElementAt(array, index) = value;
}

KRef Kotlin_Array_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      array->type_info(), array->count_).GetPlace();
  memcpy(
      ArrayAddressOfElementAt(result, 0),
      ArrayAddressOfElementAt(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_Array_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

void Kotlin_Array_fillImpl(KRef thiz, KInt fromIndex, KInt toIndex, KRef value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (fromIndex < 0 || toIndex < fromIndex || toIndex >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  // TODO: refcounting!
  for (KInt index = fromIndex; index < toIndex; ++index) {
    *ArrayAddressOfElementAt(array, index) = value;
  }
}

void Kotlin_Array_copyImpl(KConstRef thiz, KInt fromIndex,
                           KRef destination, KInt toIndex, KInt count) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* destinationArray = static_cast<ArrayHeader*>(destination);
   if (fromIndex < 0 || fromIndex + count > array->count_ ||
       toIndex < 0 || toIndex + count > destinationArray->count_) {
        ThrowArrayIndexOutOfBoundsException();
    }
    // TODO: refcounting!
    memmove(ArrayAddressOfElementAt(destinationArray, toIndex),
        ArrayAddressOfElementAt(array, fromIndex), count * sizeof(KRef));
}

// Arrays.kt
KByte Kotlin_ByteArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(array, index);
}

void Kotlin_ByteArray_set(KRef thiz, KInt index, KByte value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ByteArrayAddressOfElementAt(array, index) = value;
}

KRef Kotlin_ByteArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theByteArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_ByteArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

KChar Kotlin_CharArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KChar>(array, index);
}

void Kotlin_CharArray_set(KRef thiz, KInt index, KChar value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KChar>(array, index) = value;
}

KRef Kotlin_CharArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theCharArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      PrimitiveArrayAddressOfElementAt<KChar>(result, 0),
      PrimitiveArrayAddressOfElementAt<KChar>(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KRef Kotlin_CharArray_copyOf(KConstRef thiz, KInt newSize) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theCharArrayTypeInfo, newSize).GetPlace();
  KInt toCopy = array->count_ < newSize ?  array->count_ : newSize;
  memcpy(
      PrimitiveArrayAddressOfElementAt<KChar>(result, 0),
      PrimitiveArrayAddressOfElementAt<KChar>(array, 0),
      toCopy * sizeof(KChar));
  return result;
}

KInt Kotlin_CharArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

KShort Kotlin_ShortArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KShort>(array, index);
}

void Kotlin_ShortArray_set(KRef thiz, KInt index, KShort value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KShort>(array, index) = value;
}

KRef Kotlin_ShortArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theShortArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      PrimitiveArrayAddressOfElementAt<KShort>(result, 0),
      PrimitiveArrayAddressOfElementAt<KShort>(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_ShortArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

KInt Kotlin_IntArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KInt>(array, index);
}

void Kotlin_IntArray_set(KRef thiz, KInt index, KInt value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KInt>(array, index) = value;
}

ObjHeader* Kotlin_IntArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theIntArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      PrimitiveArrayAddressOfElementAt<KInt>(result, 0),
      PrimitiveArrayAddressOfElementAt<KInt>(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_IntArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

void Kotlin_IntArray_fillImpl(KRef thiz, KInt fromIndex, KInt toIndex, KInt value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (fromIndex < 0 || toIndex < fromIndex || toIndex >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  for (KInt index = fromIndex; index < toIndex; ++index) {
    *PrimitiveArrayAddressOfElementAt<KInt>(array, index) = value;
  }
}

KLong Kotlin_LongArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {

        ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KLong>(array, index);
}

void Kotlin_IntArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              ArrayHeader* destination, KInt toIndex, KInt count) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (fromIndex < 0 || fromIndex + count > array->count_ ||
      toIndex < 0 || toIndex + count > destination->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  memmove(PrimitiveArrayAddressOfElementAt<KInt>(destination, toIndex),
          PrimitiveArrayAddressOfElementAt<KInt>(array, fromIndex),
          count * sizeof(KInt));
}

void Kotlin_LongArray_set(KRef thiz, KInt index, KLong value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KLong>(array, index) = value;
}

KRef Kotlin_LongArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theLongArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      PrimitiveArrayAddressOfElementAt<KLong>(result, 0),
      PrimitiveArrayAddressOfElementAt<KLong>(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_LongArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

KFloat Kotlin_FloatArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KFloat>(array, index);
}

void Kotlin_FloatArray_set(KRef thiz, KInt index, KFloat value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KFloat>(array, index) = value;
}

ArrayHeader* Kotlin_FloatArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theFloatArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      PrimitiveArrayAddressOfElementAt<KFloat>(result, 0),
      PrimitiveArrayAddressOfElementAt<KFloat>(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_FloatArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

KDouble Kotlin_DoubleArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KDouble>(array, index);
}

void Kotlin_DoubleArray_set(KRef thiz, KInt index, KDouble value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KDouble>(array, index) = value;
}

KRef Kotlin_DoubleArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theDoubleArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      PrimitiveArrayAddressOfElementAt<KDouble>(result, 0),
      PrimitiveArrayAddressOfElementAt<KDouble>(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_DoubleArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

KBoolean Kotlin_BooleanArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KBoolean>(array, index);
}

void Kotlin_BooleanArray_set(KRef thiz, KInt index, KBoolean value) {
  ArrayHeader* array = static_cast<ArrayHeader*>(thiz);
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KBoolean>(array, index) = value;
}

KRef Kotlin_BooleanArray_clone(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  ArrayHeader* result = ArrayContainer(
      theBooleanArrayTypeInfo, array->count_).GetPlace();
  memcpy(
      PrimitiveArrayAddressOfElementAt<KBoolean>(result, 0),
      PrimitiveArrayAddressOfElementAt<KBoolean>(array, 0),
      ArrayDataSizeBytes(array));
  return result;
}

KInt Kotlin_BooleanArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = static_cast<const ArrayHeader*>(thiz);
  return array->count_;
}

}  // extern "C"

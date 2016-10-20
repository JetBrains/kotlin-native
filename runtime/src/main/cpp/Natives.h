#ifndef RUNTIME_NATIVES_H
#define RUNTIME_NATIVES_H

#include "Memory.h"
#include "Types.h"

typedef uint8_t KByte;
typedef uint16_t KChar;
// Note that it is signed.
typedef int32_t KInt;

// Optimized versions not accessing type info.
inline KByte* ByteArrayAddressOfElementAt(ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<KByte*>(obj + 1) + index;
}

inline const KByte* ByteArrayAddressOfElementAt(const ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<const KByte*>(obj + 1) + index;
}

inline KChar* CharArrayAddressOfElementAt(ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<KChar*>(obj + 1) + index;
}

inline const KChar* CharArrayAddressOfElementAt(const ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<const KChar*>(obj + 1) + index;
}

inline KInt* IntArrayAddressOfElementAt(ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<KInt*>(obj + 1) + index;
}

inline const KInt* IntArrayAddressOfElementAt(const ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<const KInt*>(obj + 1) + index;
}

#ifdef __cplusplus
extern "C" {
#endif

// TODO: those must be compiler intrinsics afterwards.
ArrayHeader* Kotlin_ByteArray_clone(const ArrayHeader* obj);
KByte Kotlin_ByteArray_get(const ArrayHeader* obj, int32_t index);
void Kotlin_ByteArray_set(ArrayHeader* obj, int32_t index, KByte value);
KInt Kotlin_ByteArray_getArrayLength(const ArrayHeader* obj);

ArrayHeader* Kotlin_CharArray_clone(const ArrayHeader* obj);
KChar Kotlin_CharArray_get(const ArrayHeader* obj, int32_t index);
void Kotlin_CharArray_set(ArrayHeader* obj, int32_t index, KChar value);
KInt Kotlin_CharArray_getArrayLength(const ArrayHeader* obj);

ArrayHeader* Kotlin_IntArray_clone(const ArrayHeader* obj);
KInt Kotlin_IntArray_get(const ArrayHeader* obj, int32_t index);
void Kotlin_IntArray_set(ArrayHeader* obj, int32_t index, KInt value);
KInt Kotlin_IntArray_getArrayLength(const ArrayHeader* obj);

KChar Kotlin_String_get(const ArrayHeader* obj, int32_t index);
ArrayHeader* Kotlin_String_fromUtf8Array(const ArrayHeader* array);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_NATIVES_H

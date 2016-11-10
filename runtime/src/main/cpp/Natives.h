#ifndef RUNTIME_NATIVES_H
#define RUNTIME_NATIVES_H

#include "Memory.h"
#include "Types.h"

typedef uint8_t KBool;
typedef uint8_t KByte;
typedef uint16_t KChar;
// Note that it is signed.
typedef int16_t KShort;
typedef int32_t KInt;
typedef int64_t KLong;
typedef float   KFloat;
typedef double  KDouble;

typedef ObjHeader* KRef;
typedef const ObjHeader* KConstRef;
typedef const ArrayHeader* KString;

// Optimized versions not accessing type info.
inline KByte* ByteArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<KByte*>(obj + 1) + index;
}

inline const KByte* ByteArrayAddressOfElementAt(
    const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const KByte*>(obj + 1) + index;
}

template <typename T>
inline T* PrimitiveArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<T*>(obj + 1) + index;
}

template <typename T>
inline const T* PrimitiveArrayAddressOfElementAt(
    const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const T*>(obj + 1) + index;
}

inline KRef* ArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<KRef*>(obj + 1) + index;
}

inline const KRef* ArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const KRef*>(obj + 1) + index;
}

#ifdef __cplusplus
extern "C" {
#endif

// Any.kt
KBool Kotlin_Any_equals(KConstRef thiz, KConstRef other);
KInt Kotlin_Any_hashCode(KConstRef thiz);
KString Kotlin_Any_toString(KConstRef thiz);

// Arrays.kt
// TODO: those must be compiler intrinsics afterwards.
ArrayHeader* Kotlin_Array_clone(const ArrayHeader* thiz);
KRef Kotlin_Array_get(const ArrayHeader* thiz, KInt index);
void Kotlin_Array_set(ArrayHeader* thiz, KInt index, KRef value);
KInt Kotlin_Array_getArrayLength(const ArrayHeader* thiz);

ArrayHeader* Kotlin_ByteArray_clone(const ArrayHeader* thiz);
KByte Kotlin_ByteArray_get(const ArrayHeader* thiz, KInt index);
void Kotlin_ByteArray_set(ArrayHeader* thiz, KInt index, KByte value);
KInt Kotlin_ByteArray_getArrayLength(const ArrayHeader* thiz);

ArrayHeader* Kotlin_CharArray_clone(const ArrayHeader* thiz);
KChar Kotlin_CharArray_get(const ArrayHeader* thiz, KInt index);
void Kotlin_CharArray_set(ArrayHeader* thiz, KInt index, KChar value);
KInt Kotlin_CharArray_getArrayLength(const ArrayHeader* thiz);

ArrayHeader* Kotlin_IntArray_clone(const ArrayHeader* thiz);
KInt Kotlin_IntArray_get(const ArrayHeader* thiz, KInt index);
void Kotlin_IntArray_set(ArrayHeader* thiz, KInt index, KInt value);
KInt Kotlin_IntArray_getArrayLength(const ArrayHeader* thiz);

// io/Console.kt
void Kotlin_io_Console_print(KString message);
void Kotlin_io_Console_println(KString message);
void Kotlin_io_Console_println0();
KString Kotlin_io_Console_readLine();

// Primitives.kt.
KString Kotlin_Int_toString(KInt value);

// String.kt
KInt Kotlin_String_hashCode(KString thiz);
KBool Kotlin_String_equals(KString thiz, KConstRef other);
KInt Kotlin_String_compareTo(KString thiz, KString other);
KChar Kotlin_String_get(KString thiz, KInt index);
KString Kotlin_String_fromUtf8Array(const ArrayHeader* array);
KString Kotlin_String_plusImpl(KString thiz, KString other);
KInt Kotlin_String_getStringLength(KString thiz);
KRef Kotlin_String_subSequence(KString thiz, KInt startIndex, KInt endIndex);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_NATIVES_H

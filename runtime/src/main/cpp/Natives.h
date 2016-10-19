#ifndef RUNTIME_NATIVES_H
#define RUNTIME_NATIVES_H

#include "Memory.h"
#include "Types.h"

#ifdef __cplusplus
extern "C" {
#endif

// TODO: those must be compiler intrinsics afterwards.
uint8_t Kotlin_ByteArray_get(ArrayHeader* obj, int32_t index);
void Kotlin_ByteArray_set(ArrayHeader* obj, int32_t index, uint8_t value);
uint16_t Kotlin_String_get(ArrayHeader* obj, int32_t index);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_NATIVES_H

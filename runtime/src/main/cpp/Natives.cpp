#include "Assert.h"
#include "Exceptions.h"
#include "Natives.h"

extern "C" {

// TODO: those must be compiler intrinsics afterwards.
uint8_t Kotlin_ByteArray_get(ArrayHeader* obj, int32_t index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

void Kotlin_ByteArray_set(ArrayHeader* obj, int32_t index, uint8_t value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ByteArrayAddressOfElementAt(obj, index) = value;
}

uint16_t Kotlin_String_get(ArrayHeader* obj, int32_t index) {
   // TODO: support full UTF-8.
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

}

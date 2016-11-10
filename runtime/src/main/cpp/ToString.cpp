#include <stdio.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

namespace {

KString makeString(const char* cstring) {
  uint32_t length = strlen(cstring);
  ArrayHeader* result = ArrayContainer(
      theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      cstring,
      length);
  return result;
}

} // namespace

extern "C" {

KString Kotlin_Byte_toString(KByte value) {
  char cstring[8];
  snprintf(cstring, sizeof(cstring), "\\%02x", value);
  return makeString(cstring);
}

KString Kotlin_Char_toString(KChar value) {
  char cstring[8];
  snprintf(cstring, sizeof(cstring), "\\u%04x", value);
  return makeString(cstring);
}

KString Kotlin_Short_toString(KShort value) {
  char cstring[8];
  snprintf(cstring, sizeof(cstring), "%d", value);
  return makeString(cstring);
}

KString Kotlin_Int_toString(KInt value) {
  char cstring[16];
  snprintf(cstring, sizeof(cstring), "%d", value);
  return makeString(cstring);
}

KString Kotlin_Long_toString(KLong value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%lld", value);
  return makeString(cstring);
}

KString Kotlin_Float_toString(KFloat value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%g", value);
  return makeString(cstring);
}

KString Kotlin_Double_toString(KDouble value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%g", value);
  return makeString(cstring);
}

KString Kotlin_Boolean_toString(KBool value) {
  return  makeString(value ? "true" : "false");
}

} // extern "C"

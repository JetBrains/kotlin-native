#include "testlib_api.h"

#define __ testlib_symbols()->
#define T_(x) testlib_kref_ ## x
#define CAST(T, v) testlib_kref_ ## T { .pinned = v }

int main() {
  T_(kotlin_ByteArray) a = __ kotlin.root.createByteArray(0xde, 0xad, 0xbe, 0xef);
  __ kotlin.root.takeByte(a);
}

/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifdef USE_LIBUNWIND
#define UNW_LOCAL_ONLY
#include <libunwind.h>
#else
#include <execinfo.h>
#endif
#include <stdio.h>  // for snprintf
#include <stdlib.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Types.h"

class KotlinException {
 public:

  KRef exception_;

  KotlinException(KRef exception) : exception_(exception) {
      ::AddRef(exception_->container());
  };

  ~KotlinException() {
      ::Release(exception_->container());
  };
};

// TODO: it seems to be very common case; does C++ std library provide something like this?
class AutoFree {
 private:
  void* mem_;

 public:
  AutoFree(void* mem): mem_(mem) {}

  ~AutoFree() {
    free(mem_);
  }
};

#ifdef __cplusplus
extern "C" {
#endif

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
OBJ_GETTER0(GetCurrentStackTrace) {
  const int maxSize = 64;
  const int skipFrames = 3;
#ifdef USE_LIBUNWIND
  unw_cursor_t cursor;
  unw_context_t context;

  unw_getcontext(&context);
  unw_init_local(&cursor, &context);

  // Count number of stacktrace elements.
  int count = 0;
  while (unw_step(&cursor) > 0 && count++ < maxSize) {
    unw_word_t offset, pc;
    unw_get_reg(&cursor, UNW_REG_IP, &pc);
    if (pc == 0) {
      break;
    }
  }

  int frameCount = count > skipFrames ? count - skipFrames : 0;
  ObjHeader* result =
    AllocArrayInstance(theArrayTypeInfo, frameCount, OBJ_RESULT);

  unw_init_local(&cursor, &context);
  // Skip few initial frames.
  count = 0;
  while (unw_step(&cursor) > 0 && count++ < skipFrames) {
  }

  ArrayHeader* array = result->array();
  count = 0;
  while (unw_step(&cursor) > 0 && count < frameCount) {
    unw_word_t offset, pc;
    unw_get_reg(&cursor, UNW_REG_IP, &pc);
    if (pc == 0) {
      break;
    }
    char symbol[512];
    char traceLine[512 + 64];
    if (unw_get_proc_name(&cursor, symbol, sizeof(symbol), &offset) == 0) {
      snprintf(traceLine, sizeof(traceLine), "%s [+%lx]", symbol, offset);
    } else {
      snprintf(traceLine, sizeof(traceLine), " [%lx]", pc);
    }
    CreateStringFromCString(
      traceLine, ArrayAddressOfElementAt(array, count++));
  }
  return result;
#else
  void* buffer[maxSize];
  int size = backtrace(buffer, maxSize);
  char** symbols = backtrace_symbols(buffer, size);
  RuntimeAssert(symbols != nullptr, "Not enough memory to retrieve the stacktrace");

  AutoFree autoFree(symbols);
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, size, OBJ_RESULT);
  ArrayHeader* array = result->array();
  for (int index = 0; index < size; ++index) {
    CreateStringFromCString(
      symbols[index], ArrayAddressOfElementAt(array, index));
  }
  return result;
#endif
}

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
		"Throwing something non-throwable");
  throw KotlinException(exception);
}


#ifdef __cplusplus
} // extern "C"
#endif

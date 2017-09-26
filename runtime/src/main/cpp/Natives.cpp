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

#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// Any.kt
KBoolean Kotlin_Any_equals(KConstRef thiz, KConstRef other) {
  return thiz == other;
}

KInt Kotlin_Any_hashCode(KConstRef thiz) {
  // Here we will use different mechanism for stable hashcode, using meta-objects
  // if moving collector will be used.
  return reinterpret_cast<uintptr_t>(thiz);
}

OBJ_GETTER0(Kotlin_getCurrentStackTrace) {
  RETURN_RESULT_OF0(GetCurrentStackTrace);
}

// TODO: consider handling it with compiler magic instead.
OBJ_GETTER0(Kotlin_konan_internal_undefined) {
  RETURN_OBJ(nullptr);
}

void* Kotlin_interop_malloc(KLong size, KInt align) {
  if (size > SIZE_MAX) {
    return nullptr;
  }

  void* result = konan::calloc(1, size);
  if ((reinterpret_cast<uintptr_t>(result) & (align - 1)) != 0) {
    // Unaligned!
    RuntimeAssert(false, "unsupported alignment");
  }

  return result;
}

void Kotlin_interop_free(void* ptr) {
  konan::free(ptr);
}

void Kotlin_system_exitProcess(KInt status) {
  konan::exit(status);
}

const void* Kotlin_Any_getTypeInfo(KConstRef obj) {
  return obj->type_info();
}

}  // extern "C"

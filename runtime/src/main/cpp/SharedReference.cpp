/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SharedReference.h"

#include "Alloc.h"
#include "Memory.h"
#include "MemorySharedRefs.hpp"

namespace {

struct SharedReference {
  ObjHeader header;
  KRefSharedHolder* holder;
};

SharedReference* asSharedReference(KRef thiz) {
  return reinterpret_cast<SharedReference*>(thiz);
}

}  // namespace

RUNTIME_NOTHROW void DisposeSharedRef(KRef thiz) {
  // DisposeSharedRef is only called when all references to thiz are gone.
  auto* holder = asSharedReference(thiz)->holder;
  holder->dispose();
  konanDestructInstance(holder);
}

extern "C" {

KNativePtr Kotlin_SharedReference_createSharedRef(KRef value) {
  auto* holder = konanConstructInstance<KRefSharedHolder>();
  holder->init(value);
  return holder;
}

OBJ_GETTER(Kotlin_SharedReference_derefSharedRef, KRef thiz) {
  // If thiz exists, holder must also exist. Disposal only happens
  // when all references to thiz are gone.
  RETURN_OBJ(asSharedReference(thiz)->holder->ref());
}

}

/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SharedRef.h"

#include "Alloc.h"
#include "Memory.h"
#include "MemorySharedRefs.hpp"

namespace {

struct SharedRefLayout {
  ObjHeader header;
  KRefSharedHolder* holder;
};

SharedRefLayout* asSharedRef(KRef thiz) {
  return reinterpret_cast<SharedRefLayout*>(thiz);
}

}  // namespace

void DisposeSharedRef(KRef thiz) {
  // DisposeSharedRef is only called when all references to thiz are gone.
  auto* holder = asSharedRef(thiz)->holder;
  holder->dispose();
  konanDestructInstance(holder);
}

extern "C" {

KNativePtr Kotlin_SharedRef_createSharedRef(KRef value) {
  auto* holder = konanConstructInstance<KRefSharedHolder>();
  holder->init(value);
  return holder;
}

OBJ_GETTER(Kotlin_SharedRef_derefSharedRef, KRef thiz) {
  // If thiz exists, holder must also exist. Disposal only happens
  // when all references to thiz are gone.
  RETURN_OBJ(asSharedRef(thiz)->holder->ref());
}

}

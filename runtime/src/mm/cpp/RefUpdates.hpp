/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_REF_UPDATES_H
#define RUNTIME_MM_REF_UPDATES_H

#include "Memory.h"

namespace kotlin {
namespace mm {

class ThreadData;

// TODO: Consider adding some kind of an `Object` type (that wraps `ObjHeader*`) which
//       will have these operations (+ allocation) for a friendlier API.

void SetStackRef(ObjHeader** location, ObjHeader* value) noexcept;
void SetHeapRef(ObjHeader** location, ObjHeader* value) noexcept;
void SetHeapRefLocked(ObjHeader** location, ObjHeader* value) noexcept;
ObjHeader* ReadHeapRefLocked(ObjHeader** location) noexcept;
ObjHeader* CompareAndSwapHeapRef(ObjHeader** location, ObjHeader* expected, ObjHeader* value) noexcept;

// TODO: Maybe these belong to a different file.
ObjHeader* InitThreadLocalSingleton(ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));
// TODO: `OBJ_GETTER` is used because the created object needs to be stored on the stack before `ctor` is invoked, so
//       it's present in the GC roots. If we had a different way to efficiently keep the object in the roots, `OBJ_GETTER`
//       can be removed.
OBJ_GETTER(InitSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_REF_UPDATES_H

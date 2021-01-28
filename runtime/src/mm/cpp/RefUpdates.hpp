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

// NOTE: Implementation of these operations assume safe points.
// TODO: Make an implementation that supports GCs that can stop the thread at any point.

// TODO: Consider adding some kind of an `Object` type (that wraps `ObjHeader*`) which
//       will have these operations (+ allocation) for a friendlier API.

// TODO: `OBJ_GETTER` is used because the returned objects needs to be accessible via the rootset before the function
//       returns. If we had a different way to efficiently keep the object in the roots, `OBJ_GETTER` can be removed.

void SetStackRef(ObjHeader** location, ObjHeader* value) noexcept;
void SetHeapRef(ObjHeader** location, ObjHeader* value) noexcept;
void SetHeapRefAtomic(ObjHeader** location, ObjHeader* value) noexcept;
OBJ_GETTER(ReadHeapRefAtomic, ObjHeader** location) noexcept;
OBJ_GETTER(CompareAndSwapHeapRef, ObjHeader** location, ObjHeader* expected, ObjHeader* value) noexcept;

// TODO: Maybe these belong to a different file.
OBJ_GETTER(InitThreadLocalSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));
OBJ_GETTER(InitSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_REF_UPDATES_H

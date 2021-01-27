/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RefUpdates.hpp"

#include "Common.h"
#include "ThreadData.hpp"

using namespace kotlin;

// TODO: Memory barriers.

ALWAYS_INLINE void mm::SetStackRef(ObjHeader** location, ObjHeader* value) noexcept {
    *location = value;
}

ALWAYS_INLINE void mm::SetHeapRef(ObjHeader** location, ObjHeader* value) noexcept {
    *location = value;
}

#pragma clang diagnostic push
// On 32-bit android arm clang warns of significant performance penalty because of large
// atomic operations. TODO: Consider using alternative ways of ordering memory operations if they
// turn out to be more efficient on these platforms.
#pragma clang diagnostic ignored "-Watomic-alignment"

ALWAYS_INLINE void mm::SetHeapRefAtomic(ObjHeader** location, ObjHeader* value) noexcept {
    __atomic_store_n(location, value, __ATOMIC_RELEASE);
}

ALWAYS_INLINE ObjHeader* mm::ReadHeapRefAtomic(ObjHeader** location) noexcept {
    return __atomic_load_n(location, __ATOMIC_ACQUIRE);
}

ALWAYS_INLINE ObjHeader* mm::CompareAndSwapHeapRef(ObjHeader** location, ObjHeader* expected, ObjHeader* value) noexcept {
    return __sync_val_compare_and_swap(location, expected, value);
}

#pragma clang diagnostic pop

ObjHeader* mm::InitThreadLocalSingleton(ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    if (auto* value = *location) {
        // Initialized by someone else.
        return value;
    }
    auto* value = threadData->objectFactoryThreadQueue().CreateObject(typeInfo);
    // This places `value` in the root set.
    mm::SetHeapRef(location, value);
#if KONAN_NO_EXCEPTIONS
    ctor(value);
#else
    try {
        ctor(value);
    } catch (...) {
        mm::SetHeapRef(location, nullptr);
        throw;
    }
#endif
    return value;
}

OBJ_GETTER(mm::InitSingleton, ThreadData* threadData, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    auto& initializingSingletons = threadData->initializingSingletons();

    // Search from the top of the stack.
    for (auto it = initializingSingletons.rbegin(); it != initializingSingletons.rend(); ++it) {
        if (it->first == location) {
            RETURN_OBJ(it->second);
        }
    }

    ObjHeader* initializing = reinterpret_cast<ObjHeader*>(1);

    // Spin lock.
    ObjHeader* value = nullptr;
    while ((value = __sync_val_compare_and_swap(location, nullptr, initializing)) == initializing) {
    }
    if (value != nullptr) {
        // Initialized by someone else.
        RETURN_OBJ(value);
    }
    auto* object = threadData->objectFactoryThreadQueue().CreateObject(typeInfo);
    // Write object to stack to make sure there's a reference to it from the roots.
    mm::SetStackRef(OBJ_RESULT, object);
    initializingSingletons.push_back(std::make_pair(location, object));

#if KONAN_NO_EXCEPTIONS
    ctor(object);
#else
    try {
        ctor(object);
    } catch (...) {
        mm::SetStackRef(OBJ_RESULT, nullptr);
        mm::SetHeapRefAtomic(location, nullptr);
        initializingSingletons.pop_back();
        throw;
    }
#endif
    mm::SetHeapRefAtomic(location, object);
    mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(threadData, location);
    initializingSingletons.pop_back();
    return object;
}

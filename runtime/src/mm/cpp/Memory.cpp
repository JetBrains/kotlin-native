/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

extern "C" struct MemoryState {
    kotlin::mm::ThreadRegistry::ThreadDataNode data;

    ALWAYS_INLINE static MemoryState* from(kotlin::mm::ThreadRegistry::ThreadDataNode* data) { return wrapper_cast(MemoryState, data, data); }
};

extern "C" MemoryState* InitMemory(bool firstRuntime) {
    auto* data = kotlin::mm::ThreadRegistry::instance().RegisterCurrentThread();
    return MemoryState::from(data);
}

extern "C" void DeinitMemory(MemoryState* state, bool destroyRuntime) {
    kotlin::mm::ThreadRegistry::instance().Unregister(&state->data);
}

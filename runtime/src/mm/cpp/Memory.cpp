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
    // Do not add any other fields: this struct is just a wrapper around ThreadDataNode.
};

namespace {

ALWAYS_INLINE MemoryState* ToMemoryState(kotlin::mm::ThreadRegistry::ThreadDataNode* data) {
    return wrapper_cast(MemoryState, data, data);
}

ALWAYS_INLINE kotlin::mm::ThreadRegistry::ThreadDataNode* FromMemoryState(MemoryState* state) {
    return &state->data;
}

} // namespace

extern "C" MemoryState* InitMemory(bool firstRuntime) {
    return ToMemoryState(kotlin::mm::ThreadRegistry::instance().RegisterCurrentThread());
}

extern "C" void DeinitMemory(MemoryState* state, bool destroyRuntime) {
    kotlin::mm::ThreadRegistry::instance().Unregister(FromMemoryState(state));
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GlobalsRegistry.hpp"

#include <iterator>

#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

// static
mm::GlobalsRegistry& mm::GlobalsRegistry::Instance() noexcept {
    return GlobalData::Instance().globalsRegistry();
}

void mm::GlobalsRegistry::RegisterStorageForGlobal(mm::ThreadData* threadData, ObjHeader** location) noexcept {
    threadData->globalsThreadQueue().queue_.push_back(location);
}

mm::GlobalsRegistry::Iterable mm::GlobalsRegistry::CollectAndIterate(mm::ThreadRegistry::Iterable& threadRegistry) noexcept {
    Iterable iterable(this);
    for (auto& threadData : threadRegistry) {
        RuntimeAssert(threadData.state() == mm::ThreadData::State::kWaitGC, "Thread must be waiting for GC to complete.");
        auto& queue = threadData.globalsThreadQueue().queue_;
        globals_.splice(globals_.cend(), queue);
    }
    return iterable;
}

mm::GlobalsRegistry::GlobalsRegistry() = default;
mm::GlobalsRegistry::~GlobalsRegistry() = default;

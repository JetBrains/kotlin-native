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

void mm::GlobalsRegistry::ProcessThread(mm::ThreadData* threadData) noexcept {
    RuntimeAssert(threadData->state() == mm::ThreadData::State::kWaitGC, "Thread must be waiting for GC to complete.");
    auto& queue = threadData->globalsThreadQueue().queue_;
    globals_.splice(globals_.end(), queue);
}

mm::GlobalsRegistry::GlobalsRegistry() = default;
mm::GlobalsRegistry::~GlobalsRegistry() = default;

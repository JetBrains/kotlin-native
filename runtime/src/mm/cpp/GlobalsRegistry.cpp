/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GlobalsRegistry.hpp"

#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

void mm::GlobalsRegistry::ThreadQueue::AddToQueue(ObjHeader** location) noexcept {
    RuntimeCheck(false, "Unimplemented");
}

void mm::GlobalsRegistry::ThreadQueue::DumpInto(std::vector<ObjHeader**>& storage) noexcept {
    RuntimeCheck(false, "Unimplemented");
}

// static
mm::GlobalsRegistry& mm::GlobalsRegistry::Instance() noexcept {
    return GlobalData::Instance().globalsRegistry();
}

void mm::GlobalsRegistry::RegisterStorageForGlobal(mm::ThreadData* threadData, ObjHeader** location) noexcept {
    threadData->globalsThreadQueueUnsafe().AddToQueue(location);
}

mm::GlobalsRegistry::Iterable mm::GlobalsRegistry::CollectAndIterate(mm::ThreadRegistry::Iterable& threadRegistry) noexcept {
    Iterable iterable(this);
    for (auto& threadData : threadRegistry) {
        threadData.globalsThreadQueueUnsafe().DumpInto(globals_);
    }
    return iterable;
}


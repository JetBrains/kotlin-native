/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

#include "ThreadData.hpp"

using namespace kotlin;

mm::ThreadRegistry::ThreadDataNode* mm::ThreadRegistry::RegisterCurrentThread() noexcept {
    ThreadDataNode* threadDataNode = list_.Emplace(pthread_self());
    ThreadData*& currentData = currentThreadData_;
    RuntimeAssert(currentData == nullptr, "This thread already had some data assigned to it.");
    currentData = list_.ValueForNode(threadDataNode);
    return threadDataNode;
}

void mm::ThreadRegistry::Unregister(ThreadDataNode* threadDataNode) noexcept {
    list_.Erase(threadDataNode);
    // Do not touch `currentThreadData_` as TLS may already have been deallocated.
}

mm::ThreadRegistry::Iterable mm::ThreadRegistry::Iter() noexcept { return list_.Iter(); }

// static
mm::ThreadRegistry mm::ThreadRegistry::instance_;

// static
thread_local mm::ThreadData* mm::ThreadRegistry::currentThreadData_ = nullptr;


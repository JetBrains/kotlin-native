/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_REGISTRY_H
#define RUNTIME_MM_THREAD_REGISTRY_H

#include <pthread.h>

#include "SingleLockList.hpp"
#include "ThreadData.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class ThreadRegistry final : private Pinned {
public:
    using ThreadDataNode = SingleLockList<ThreadData>::Node;

    static ThreadRegistry& instance() noexcept { return instance_; }

    ThreadDataNode* RegisterCurrentThread() noexcept {
        ThreadDataNode* threadDataNode = list_.Emplace(pthread_self());
        ThreadData*& currentData = currentThreadData_;
        RuntimeAssert(currentData == nullptr, "This thread already had some data assigned to it.");
        currentData = list_.ValueForNode(threadDataNode);
        return threadDataNode;
    }

    // `ThreadData` associated with `threadDataNode` cannot be used after this call.
    void Unregister(ThreadDataNode* threadDataNode) noexcept {
        list_.Erase(threadDataNode);
        // Do not touch `currentThreadData_` as TLS may already have been deallocated.
    }

    // Locks `ThreadRegistry` for safe iteration.
    SingleLockList<ThreadData>::Iterable Iter() noexcept { return list_.Iter(); }

    // Try not to use it very often, as (1) thread local access can be slow on some platforms,
    // (2) TLS gets deallocated before our thread destruction hooks run.
    // Using this after `Unregister` for the thread has been called is undefined behaviour.
    ThreadData* CurrentThreadData() const noexcept { return currentThreadData_; }

private:
    ThreadRegistry() = default;
    ~ThreadRegistry() = default;

    static ThreadRegistry instance_;

    static thread_local ThreadData* currentThreadData_;

    SingleLockList<ThreadData> list_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_REGISTRY_H

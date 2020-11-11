/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_REGISTRY_H
#define RUNTIME_MM_THREAD_REGISTRY_H

#include <pthread.h>

#include "ThreadData.hpp"
#include "SingleLockList.hpp"
#include "Utils.h"

namespace kotlin {
namespace mm {

class ThreadRegistry final : private NoCopyOrMove {
public:
    static ThreadRegistry& instance() noexcept { return instance_; }

    ThreadData* RegisterCurrentThread() noexcept {
        ThreadData* threadData = list_.Emplace(pthread_self());
        ThreadData*& currentData = currentThreadData_;
        RuntimeAssert(currentData == nullptr, "This thread already had some data assigned to it.");
        currentData = threadData;
        return threadData;
    }

    // Can only be called with `threadData` returned from `RegisterCurrentThread`.
    // `threadData` cannot be used after this call.
    void Unregister(ThreadData* threadData) noexcept {
        list_.Erase(threadData);
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

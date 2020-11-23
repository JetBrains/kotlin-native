/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_DATA_H
#define RUNTIME_MM_THREAD_DATA_H

#include <pthread.h>

#include "GlobalsRegistry.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

// `ThreadData` is supposed to be thread local singleton.
// Pin it in memory to prevent accidental copying.
class ThreadData final : private Pinned {
public:
    enum class State {
        kRunning,
        kWaitGC,
    };

    ThreadData(pthread_t threadId) noexcept : threadId_(threadId) {}

    ~ThreadData() = default;

    pthread_t threadId() const noexcept { return threadId_; }

    State state() const noexcept { return state_; }

    GlobalsRegistry::ThreadQueue* globalsThreadQueue() noexcept { return &globalsThreadQueue_; }

private:
    const pthread_t threadId_;
    State state_ = State::kRunning;
    GlobalsRegistry::ThreadQueue globalsThreadQueue_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_DATA_H

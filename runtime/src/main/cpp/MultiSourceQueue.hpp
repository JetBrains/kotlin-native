/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MULTI_SOURCE_QUEUE_H
#define RUNTIME_MULTI_SOURCE_QUEUE_H

#include <mutex>

#include "Mutex.hpp"
#include "SingleLockList.hpp"

namespace kotlin {

// A queue that is constructed by collecting subqueues from several `Producer`s.
template <typename T>
class MultiSourceQueue {
public:
    class Producer {
    public:
        explicit Producer(MultiSourceQueue& owner) noexcept : owner_(owner) {}

        ~Producer() { Publish(); }

        void Insert(const T& value) noexcept { queue_.EmplaceBack(value); }

        // Merge `this` queue with owning `MultiSourceQueue`. `this` will have empty queue after the call.
        // This call is performed without heap allocations. TODO: Test that no allocations are happening.
        void Publish() noexcept { owner_.Collect(*this); }

    private:
        friend class MultiSourceQueue;

        MultiSourceQueue& owner_; // weak
        SingleLockList<T, NoLock> queue_;
    };

    using Iterator = typename SingleLockList<T, SimpleMutex>::Iterator;
    using Iterable = typename SingleLockList<T, SimpleMutex>::Iterable;

    // Lock MultiSourceQueue for safe iteration.
    Iterable Iter() noexcept { return commonQueue_.Iter(); }

private:
    void Collect(Producer& producer) noexcept { commonQueue_.SpliceBack(producer.queue_); }

    // Using `SingleLockList` as it allows to implement `Collect` without memory allocations,
    // which is important for GC mark phase.
    SingleLockList<T, SimpleMutex> commonQueue_;
};

} // namespace kotlin

#endif // RUNTIME_MULTI_SOURCE_QUEUE_H

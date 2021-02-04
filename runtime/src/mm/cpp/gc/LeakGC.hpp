/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_LEAK_GC_H
#define RUNTIME_MM_LEAK_GC_H

#include <cstddef>

#include "Utils.hpp"

namespace kotlin {
namespace mm {

// LeakGC (aka no-op GC) is a GC that does not free memory.
// TODO: It can be made more efficient.
class LeakGC : private Pinned {
public:
    class ObjectData {};

    class ThreadData : private Pinned {
    public:
        using ObjectData = LeakGC::ObjectData;

        explicit ThreadData(LeakGC& gc) noexcept {}
        ~ThreadData() = default;

        void SafePointFunctionEpilogue() noexcept {}
        void SafePointLoopBody() noexcept {}
        void SafePointExceptionUnwind() noexcept {}
        void SafePointAllocation(size_t size) noexcept {}

        void PerformFullGC() noexcept {}

        void OnOOM(size_t size) noexcept {}

    private:
    };

    LeakGC() noexcept = default;
    ~LeakGC() = default;

private:
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_LEAK_GC_H
